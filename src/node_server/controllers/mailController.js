const { v4: uuidv4 } = require('uuid');
const Mail = require('../models/Mail'); // MongoDB Mail model
const User = require('../models/User'); // User model for validation
const { extractUrls } = require("../utils/extractUrls");
const { checkUrlBlacklist } = require("../utils/blacklistClient");
const { addUrlToBlacklist } = require("../utils/blacklistClient");
const { Label } = require('../models/labels'); 

// Add temporary functions instead of labelModel:
const labelModel = {
    addLabelToMail: (userId, mailId, labelId) => {
        console.log(`[TEMP] Adding label ${labelId} to mail ${mailId} for user ${userId}`);
        return Promise.resolve();
    },
    addLabel: (userId, labelName) => {
        console.log(`[TEMP] Creating label ${labelName} for user ${userId}`);
        return { id: 'temp-' + Date.now(), name: labelName };
    }
};

// System labels that cannot be created/edited/deleted
const SYSTEM_LABELS = ['inbox', 'sent', 'spam', 'drafts', 'starred', 'trash', 'important'];

const getAllLabels = async (userId) => {
    try {
        const userLabels = await Label.getAllLabelsForUser(userId);
        
        // Basic labels - same list as in labelsController
        const basicLabels = SYSTEM_LABELS.map(name => ({
            id: name, 
            name: name
        }));
        
        const customLabels = userLabels.map(label => ({
            id: label.labelId,
            name: label.name
        }));
        
        return [...basicLabels, ...customLabels];
    } catch (error) {
        console.error('Error fetching labels:', error);
        return SYSTEM_LABELS.map(name => ({
            id: name, 
            name: name
        }));
    }
};

// Create mail function with MongoDB
const createMail = async (req, res) => {
    try {
        let { labels = ["inbox"] } = req.body;
        const { sender, recipient, recipients, subject, content } = req.body;
        const groupId = uuidv4();

        // Build recipients list (back-compat)
        const recipientsList = Array.isArray(recipients)
            ? recipients
            : recipient
                ? [recipient]
                : [];

        // Basic validation
        if (recipientsList.length === 0) {
            return res.status(400).json({ error: "Missing required fields" });
        }

        // Check if sender exists in MongoDB
        const senderUser = await User.findOne({ email: sender }).lean();
        if (!senderUser) {
            return res.status(400).json({ error: "Sender does not exist" });
        }

        if (sender !== req.user.email) {
            return res.status(403).json({ error: "Sender email does not match authenticated user" });
        }

        // Check if all recipients exist
        for (const r of recipientsList) {
            const recipientUser = await User.findOne({ email: r }).lean();
            if (!recipientUser) {
                return res.status(400).json({ error: `Recipient does not exist: ${r}` });
            }
        }

        // Blacklist check
        try {
            const urls = extractUrls(`${subject} ${content}`);
            const results = await Promise.all(urls.map(checkUrlBlacklist));
            if (results.includes(true)) {
                console.log("Message contains blacklisted URL, marking as spam");

                const sent = [];
                
                // Handle recipients (not sender) - spam
                for (const r of recipientsList.filter(r => r !== sender)) {
                    const recipientLabels = await getAllLabels(r); // Add await
                    let spamLabel = recipientLabels.find(l => l.name.toLowerCase() === "spam");
                    if (!spamLabel) {
                        spamLabel = labelModel.addLabel(r, "Spam");
                    }

                    const mail = new Mail({
                        mailId: uuidv4(),
                        sender,
                        recipient: r,
                        recipients: recipientsList,
                        subject,
                        content,
                        labels: [{ userEmail: r, labelIds: [spamLabel.id] }],
                        groupId,
                        timestamp: new Date()
                    });

                    await mail.save();
                    sent.push(mail);
                }

                // Handle sender - spam
                if (recipientsList.includes(sender)) {
                    // Self-send case: one mail with spam label for sender
                    const senderLabels = await getAllLabels(sender); // Add await
                    let spamLabel = senderLabels.find(l => l.name.toLowerCase() === "spam");
                    if (!spamLabel) {
                        spamLabel = labelModel.addLabel(sender, "Spam");
                    }

                    const senderMail = new Mail({
                        mailId: uuidv4(),
                        sender,
                        recipient: sender,
                        recipients: recipientsList,
                        subject,
                        content,
                        labels: [{ userEmail: sender, labelIds: [spamLabel.id] }],
                        groupId,
                        timestamp: new Date()
                    });

                    await senderMail.save();
                    sent.push(senderMail);
                } else {
                    // Regular case: sender gets copy with spam + sent labels
                    const senderLabels = await getAllLabels(sender); // Add await
                    let spamLabel = senderLabels.find(l => l.name.toLowerCase() === "spam");
                    let sentLabel = senderLabels.find(l => l.name.toLowerCase() === "sent");
                    
                    if (!spamLabel) {
                        spamLabel = labelModel.addLabel(sender, "Spam");
                    }
                    if (!sentLabel) {
                        sentLabel = labelModel.addLabel(sender, "Sent");
                    }

                    const senderMail = new Mail({
                        mailId: uuidv4(),
                        sender,
                        recipient: sender,
                        recipients: recipientsList,
                        subject,
                        content,
                        labels: [{ userEmail: sender, labelIds: [spamLabel.id, sentLabel.id] }],
                        groupId,
                        timestamp: new Date()
                    });

                    await senderMail.save();
                    sent.push(senderMail);
                }

                return res.status(201).json({ message: "Mail sent to spam", sent });
            }
        } catch (err) {
            console.error("Error while checking blacklist:", err);
            return res.status(500).json({ error: "Failed to validate message links" });
        }

        // Regular mail sending
        const sent = [];

        // Handle recipients (not sender)
        for (const r of recipientsList.filter(r => r !== sender)) {
            const mail = new Mail({
                mailId: uuidv4(),
                sender,
                recipient: r,
                recipients: recipientsList,
                subject,
                content,
                labels: [{ userEmail: r, labelIds: labels }],
                groupId,
                timestamp: new Date()
            });

            await mail.save();
            sent.push(mail);
        }

        // Handle sender
        if (recipientsList.includes(sender)) {
            // Self-send case: one mail with inbox + sent labels
            const senderLabels = await getAllLabels(sender); // Add await
            let inboxLabel = senderLabels.find(l => l.name.toLowerCase() === "inbox");
            let sentLabel = senderLabels.find(l => l.name.toLowerCase() === "sent");
            
            if (!inboxLabel) {
                inboxLabel = labelModel.addLabel(sender, "Inbox");
            }
            if (!sentLabel) {
                sentLabel = labelModel.addLabel(sender, "Sent");
            }

            const senderMail = new Mail({
                mailId: uuidv4(),
                sender,
                recipient: sender,
                recipients: recipientsList,
                subject,
                content,
                labels: [{ userEmail: sender, labelIds: [inboxLabel.id, sentLabel.id] }],
                groupId,
                timestamp: new Date()
            });

            await senderMail.save();
            sent.push(senderMail);
        } else {
            // Regular case: sender gets copy with sent label only
            const senderLabels = await getAllLabels(sender); // Add await
            let sentLabel = senderLabels.find(l => l.name.toLowerCase() === "sent");
            
            if (!sentLabel) {
                sentLabel = labelModel.addLabel(sender, "Sent");
            }

            const senderMail = new Mail({
                mailId: uuidv4(),
                sender,
                recipient: sender,
                recipients: recipientsList,
                subject,
                content,
                labels: [{ userEmail: sender, labelIds: [sentLabel.id] }],
                groupId,
                timestamp: new Date()
            });

            await senderMail.save();
            sent.push(senderMail);
        }

        return res.status(201).json({ message: "Mail sent successfully", sent });

    } catch (err) {
        console.error("Error creating mail:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Get mails function with MongoDB
const getMails = async (req, res) => {
    try {
        if (!req.user || !req.user.email) {
            return res.status(401).json({ error: "Unauthorized: missing user data" });
        }

        const userEmail = req.user.email;

        // Get inbox mails
        const inbox = await Mail.findInboxByUser(userEmail).lean();

        // Get sent mails
        const sent = await Mail.findSentByUser(userEmail).lean();

        // Get drafts
        const drafts = await Mail.findDraftsByUser(userEmail).lean();

        if (inbox.length === 0 && sent.length === 0 && drafts.length === 0) {
            return res.status(200).json({
                message: "No mails found for this user",
                inbox: [],
                sent: [],
                drafts: [],
                recent_mails: []
            });
        }

        // Sort inbox by timestamp and limit to 50
        const recent_mails = inbox.slice(0, 50).map(mail => {
            const isSent = mail.sender === userEmail;
            const otherEmail = isSent ? mail.recipient : mail.sender;

            return {
                id: mail.mailId,
                subject: mail.subject,
                timestamp: mail.timestamp,
                direction: isSent ? 'sent' : 'received',
                otherParty: { email: otherEmail },
                preview: mail.content?.slice(0, 100) || ""
            };
        });

        // Helper function to get user labels from mail
        const getUserLabels = (mail, userEmail) => {
            const userLabel = mail.labels?.find(l => l.userEmail === userEmail);
            return userLabel ? userLabel.labelIds : [];
        };

        res.status(200).json({
            message: "Mails fetched successfully",
            inbox: inbox.map(mail => ({
                id: mail.mailId,
                sender: mail.sender,
                recipient: mail.recipient,
                recipients: mail.recipients,
                subject: mail.subject,
                content: mail.content,
                labels: getUserLabels(mail, userEmail),
                groupId: mail.groupId,
                timestamp: mail.timestamp
            })),
            recent_mails,
            sent: sent.map(mail => ({
                id: mail.mailId,
                sender: mail.sender,
                recipient: mail.recipient,
                recipients: mail.recipients,
                subject: mail.subject,
                content: mail.content,
                labels: getUserLabels(mail, userEmail),
                groupId: mail.groupId,
                timestamp: mail.timestamp
            })),
            drafts: drafts.map(mail => ({
                id: mail.mailId,
                sender: mail.sender,
                recipient: mail.recipient,
                recipients: mail.recipients,
                subject: mail.subject,
                content: mail.content,
                labels: getUserLabels(mail, userEmail),
                groupId: mail.groupId,
                timestamp: mail.timestamp,
                isDraft: true
            }))
        });

    } catch (err) {
        console.error("Failed to fetch mails:", err.message);
        res.status(500).json({ error: "Internal server error" });
    }
};

// Get mail by ID function with MongoDB
const getMailById = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;

        if (!mailId) {
            return res.status(400).json({ error: "Missing mail ID" });
        }

        const mail = await Mail.findOne({ mailId }).lean();
        if (!mail) {
            return res.status(404).json({ error: "Mail not found" });
        }

        if (!mail.isAccessibleBy || !mail.isAccessibleBy(userEmail)) {
            if (mail.sender !== userEmail && mail.recipient !== userEmail &&
                (!mail.recipients || !mail.recipients.includes(userEmail))) {
                return res.status(403).json({ error: "You are not authorized to view this mail" });
            }
        }

        const getUserLabels = (mail, userEmail) => {
            const userLabel = mail.labels?.find(l => l.userEmail === userEmail);
            return userLabel ? userLabel.labelIds : [];
        };

        return res.status(200).json({
            id: mail.mailId,
            sender: { email: mail.sender },
            recipient: { email: mail.recipient },
            recipients: mail.recipients || [],
            subject: mail.subject,
            content: mail.content,
            timestamp: mail.timestamp,
            labels: getUserLabels(mail, userEmail)
        });

    } catch (err) {
        console.error("Error getting mail by ID:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Update mail function with MongoDB
const updateMail = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;

        if (!mailId) {
            return res.status(400).json({ error: "Missing mail ID" });
        }

        const { subject, content } = req.body;
        if (subject === undefined && content === undefined) {
            return res.status(400).json({ error: "Nothing to update" });
        }

        const mail = await Mail.findOne({ mailId });
        if (!mail) {
            return res.status(404).json({ error: "Mail not found" });
        }

        if (mail.sender !== userEmail) {
            return res.status(403).json({ error: "Only sender may edit subject or content" });
        }

        if (subject !== undefined) mail.subject = subject;
        if (content !== undefined) mail.content = content;

        await mail.save();

        return res.status(200).json({ message: "Mail updated", mail });

    } catch (err) {
        console.error("Error updating mail:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Delete mail function with MongoDB (soft delete)
const deleteMailById = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;

        if (!mailId) {
            return res.status(400).json({ error: "Missing mail ID" });
        }

        const mail = await Mail.findOne({ mailId });
        if (!mail) {
            return res.status(404).json({ error: "Mail not found" });
        }

        if (mail.sender !== userEmail && mail.recipient !== userEmail &&
            (!mail.recipients || !mail.recipients.includes(userEmail))) {
            return res.status(403).json({ error: "Not authorized to delete this mail" });
        }

        // Soft delete: add user to deletedBy array
        if (!mail.deletedBy.includes(userEmail)) {
            mail.deletedBy.push(userEmail);
            await mail.save();
        }

        return res.status(200).json({ message: "Mail deleted successfully" });

    } catch (err) {
        console.error("Error deleting mail:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Search mails function with MongoDB
const searchMails = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const searchQuery = req.query.q; // Search parameter from URL

        if (!searchQuery) {
            return res.status(400).json({ error: "Missing search query" });
        }

        const q = searchQuery.toLowerCase(); // Content to search for

        // MongoDB query - limits drafts only to sender
        const mongoQuery = {
            $and: [
                {
                    $or: [
                        { sender: userEmail },
                        { recipient: userEmail },
                        { recipients: userEmail }
                    ]
                },
                {
                    $or: [
                        { isDraft: { $ne: true } }, // Not a draft
                        { isDraft: true, sender: userEmail } // Draft only by sender
                    ]
                }
            ],
            deletedBy: { $ne: userEmail }
        };

        // Search in database
        const mails = await Mail.find(mongoQuery).lean();

        // Filter by search content
        const results = mails.filter(mail => {
            const subject = mail.subject?.toLowerCase() || "";
            const content = mail.content?.toLowerCase() || "";
            const sender = mail.sender?.toLowerCase() || "";
            const recipientsJoined = mail.recipients?.map(r => r.toLowerCase()).join(" ") || "";

            return (
                subject.includes(q) ||
                content.includes(q) ||
                sender.includes(q) ||
                recipientsJoined.includes(q)
            );
        });

        if (results.length === 0) {
            return res.status(404).json({ error: "No matching mails found" });
        }

        // Remove duplicates by groupId
        const seenGroups = new Set();
        const uniqueResults = results.filter(mail => {
            if (mail.groupId && seenGroups.has(mail.groupId)) return false;
            seenGroups.add(mail.groupId || mail.mailId);
            return true;
        });

        return res.json(uniqueResults.map(mail => ({
            id: mail.mailId,
            subject: mail.subject,
            timestamp: mail.timestamp,
            direction: mail.sender === userEmail ? "sent" : "received",
            sender: mail.sender,
            recipients: mail.recipients || [mail.recipient],
            content: mail.content,
        })));

    } catch (err) {
        console.error("Error searching mails:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Update mail labels function with MongoDB
const updateMailLabelsForUser = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;
        const { labels } = req.body;

        if (!Array.isArray(labels)) {
            return res.status(400).json({ error: "Labels must be an array" });
        }

        const mail = await Mail.findOne({ mailId });
        if (!mail) {
            return res.status(404).json({ error: "Mail not found" });
        }

        if (mail.sender !== userEmail && mail.recipient !== userEmail &&
            (!mail.recipients || !mail.recipients.includes(userEmail))) {
            return res.status(403).json({ error: "Not authorized for this mail" });
        }

        // Change here: await getAllLabels instead of synchronous call
        const allowed = (await getAllLabels(userEmail)).map(l => l.name.toLowerCase());
        const invalid = labels.filter(l => !allowed.includes(l.toLowerCase()));

        if (invalid.length > 0) {
            return res.status(400).json({
                error: `Invalid labels for user: ${invalid.join(", ")}`
            });
        }

        // Update labels for this user
        let userLabelEntry = mail.labels.find(l => l.userEmail === userEmail);
        if (!userLabelEntry) {
            userLabelEntry = { userEmail: userEmail, labelIds: [] };
            mail.labels.push(userLabelEntry);
        }

        const previousLabels = userLabelEntry.labelIds || [];
        userLabelEntry.labelIds = labels;
        
        // Check if spam label was added manually
        const addedSpam = !previousLabels.map(l => l.toLowerCase()).includes("spam") &&
            labels.map(l => l.toLowerCase()).includes("spam");

        if (addedSpam) {
            console.log("[SpamLabel] Spam label was added manually");
            const urls = extractUrls(`${mail.subject} ${mail.content}`);
            console.log("[SpamLabel] Extracted URLs:", urls);
            for (const url of urls) {
                try {
                    await addUrlToBlacklist(url);
                    console.log(`Added URL to blacklist: ${url}`);
                } catch (err) {
                    console.error(`Failed to add URL to blacklist: ${url}`, err.message);
                }
            }
        }

        await mail.save();

        const currentUserLabels = mail.labels.find(l => l.userEmail === userEmail);
        return res.status(200).json({
            message: "Labels updated",
            labels: currentUserLabels ? currentUserLabels.labelIds : []
        });

    } catch (err) {
        console.error("Error updating mail labels:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

const archiveMail = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;

        const mail = await Mail.findOne({ mailId });
        if (!mail || !mail.isAccessibleBy(userEmail)) {
            return res.status(404).json({ error: "Mail not found" });
        }

        // Remove inbox from user's labels
        let userLabelEntry = mail.labels.find(l => l.userEmail === userEmail);
        if (userLabelEntry) {
            userLabelEntry.labelIds = userLabelEntry.labelIds.filter(label => 
                label.toLowerCase() !== 'inbox'
            );
            await mail.save();
        }

        return res.status(200).json({ message: "Mail archived successfully" });
    } catch (err) {
        console.error("Error archiving mail:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Star/Unstar mail
const toggleStarMail = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;

        const mail = await Mail.findOne({ mailId });
        if (!mail || !mail.isAccessibleBy(userEmail)) {
            return res.status(404).json({ error: "Mail not found" });
        }

        let userLabelEntry = mail.labels.find(l => l.userEmail === userEmail);
        if (!userLabelEntry) {
            userLabelEntry = { userEmail: userEmail, labelIds: [] };
            mail.labels.push(userLabelEntry);
        }

        const hasStarred = userLabelEntry.labelIds.includes('starred');
        if (hasStarred) {
            userLabelEntry.labelIds = userLabelEntry.labelIds.filter(label => label !== 'starred');
        } else {
            userLabelEntry.labelIds.push('starred');
        }

        await mail.save();
        return res.status(200).json({ 
            message: hasStarred ? "Mail unstarred" : "Mail starred",
            starred: !hasStarred
        });
    } catch (err) {
        console.error("Error toggling star:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Add single label to mail
const addLabelToMail = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;
        const { labelName } = req.body;

        if (!labelName) {
            return res.status(400).json({ error: "Label name is required" });
        }

        const mail = await Mail.findOne({ mailId });
        if (!mail || !mail.isAccessibleBy(userEmail)) {
            return res.status(404).json({ error: "Mail not found" });
        }

        let userLabelEntry = mail.labels.find(l => l.userEmail === userEmail);
        if (!userLabelEntry) {
            userLabelEntry = { userEmail: userEmail, labelIds: [] };
            mail.labels.push(userLabelEntry);
        }

        // Add label if not exists
        if (!userLabelEntry.labelIds.includes(labelName.toLowerCase())) {
            userLabelEntry.labelIds.push(labelName.toLowerCase());
            await mail.save();
        }

        return res.status(200).json({ message: "Label added successfully" });
    } catch (err) {
        console.error("Error adding label:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Remove single label from mail
const removeLabelFromMail = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;
        const { labelName } = req.body;

        if (!labelName) {
            return res.status(400).json({ error: "Label name is required" });
        }

        const mail = await Mail.findOne({ mailId });
        if (!mail || !mail.isAccessibleBy(userEmail)) {
            return res.status(404).json({ error: "Mail not found" });
        }

        // Cannot remove critical system labels
        if (['sent', 'drafts'].includes(labelName.toLowerCase())) {
            return res.status(400).json({ 
                error: `Cannot remove system label: ${labelName}` 
            });
        }

        let userLabelEntry = mail.labels.find(l => l.userEmail === userEmail);
        if (userLabelEntry) {
            userLabelEntry.labelIds = userLabelEntry.labelIds.filter(label => 
                label.toLowerCase() !== labelName.toLowerCase()
            );
            await mail.save();
        }

        return res.status(200).json({ message: "Label removed successfully" });
    } catch (err) {
        console.error("Error removing label:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Get mails by specific label with full details
const getMailsByLabel = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const labelName = req.params.label.toLowerCase();

        const query = {
            'labels': {
                $elemMatch: {
                    'userEmail': userEmail,
                    'labelIds': labelName
                }
            },
            deletedBy: { $ne: userEmail }
        };

        const mails = await Mail.find(query).lean().sort({ timestamp: -1 });

        const getUserLabels = (mail, userEmail) => {
            const userLabel = mail.labels?.find(l => l.userEmail === userEmail);
            return userLabel ? userLabel.labelIds : [];
        };

        const formattedMails = mails.map(mail => ({
            id: mail.mailId,
            sender: mail.sender,
            recipient: mail.recipient,
            recipients: mail.recipients,
            subject: mail.subject,
            content: mail.content,
            labels: getUserLabels(mail, userEmail),
            groupId: mail.groupId,
            timestamp: mail.timestamp
        }));

        return res.status(200).json({
            message: `Mails with label '${labelName}' fetched successfully`,
            mails: formattedMails
        });
    } catch (err) {
        console.error(`Error fetching mails with label:`, err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Create draft function
const createDraft = async (req, res) => {
    try {
        const { sender, recipient, recipients, subject, content } = req.body;

        if (sender !== req.user.email) {
            return res.status(403).json({ error: "Sender email does not match authenticated user" });
        }

        const recipientsList = Array.isArray(recipients) ? recipients : recipient ? [recipient] : [];

        const draft = new Mail({
            mailId: uuidv4(),
            sender,
            recipient: recipientsList[0] || sender,
            recipients: recipientsList,
            subject: subject || '',
            content: content || '',
            labels: [{ userEmail: sender, labelIds: ['drafts'] }],
            groupId: uuidv4(),
            isDraft: true,
            timestamp: new Date()
        });

        await draft.save();
        return res.status(201).json({ message: "Draft created successfully", draft });

    } catch (err) {
        console.error("Error creating draft:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Send draft function
const sendDraft = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const draftId = req.params.id;

        const draft = await Mail.findOne({ mailId: draftId, isDraft: true });
        if (!draft) {
            return res.status(404).json({ error: "Draft not found" });
        }

        if (draft.sender !== userEmail) {
            return res.status(403).json({ error: "Not authorized to send this draft" });
        }

        // Convert draft to regular mail
        draft.isDraft = false;
        
        // Update labels - remove drafts
        const senderLabels = draft.labels.find(l => l.userEmail === userEmail);
        if (senderLabels) {
            senderLabels.labelIds = senderLabels.labelIds.filter(label => label !== 'drafts');
        }

        // Check if it's self-send
        const isSelfSend = draft.recipients.includes(userEmail);
        
        if (isSelfSend) {
            // Self-send: add both inbox and sent
            if (senderLabels) {
                senderLabels.labelIds.push('inbox', 'sent');
            }
        } else {
            // Regular send: only sent
            if (senderLabels) {
                senderLabels.labelIds.push('sent');
            }
        }

        await draft.save();

        // Create copies for recipients (if not self-send)
        const sent = [draft];
        
        if (!isSelfSend) {
            for (const recipient of draft.recipients.filter(r => r !== userEmail)) {
                const recipientMail = new Mail({
                    mailId: uuidv4(),
                    sender: draft.sender,
                    recipient: recipient,
                    recipients: draft.recipients,
                    subject: draft.subject,
                    content: draft.content,
                    labels: [{ userEmail: recipient, labelIds: ['inbox'] }],
                    groupId: draft.groupId,
                    timestamp: new Date()
                });

                await recipientMail.save();
                sent.push(recipientMail);
            }
        }

        return res.status(200).json({ message: "Draft sent successfully", sent });

    } catch (err) {
        console.error("Error sending draft:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

const getDrafts = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const drafts = await Mail.findDraftsByUser(userEmail).lean();

        const getUserLabels = (mail, userEmail) => {
            const userLabel = mail.labels?.find(l => l.userEmail === userEmail);
            return userLabel ? userLabel.labelIds : [];
        };

        const formattedDrafts = drafts.map(mail => ({
            id: mail.mailId,
            sender: mail.sender,
            recipient: mail.recipient,
            recipients: mail.recipients,
            subject: mail.subject,
            content: mail.content,
            labels: getUserLabels(mail, userEmail),
            groupId: mail.groupId,
            timestamp: mail.timestamp,
            isDraft: true
        }));

        return res.status(200).json({
            message: "Drafts fetched successfully",
            drafts: formattedDrafts
        });
    } catch (err) {
        console.error("Error fetching drafts:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

// Delete mail permanently (hard delete)
const deleteMailByIdPermanently = async (req, res) => {
    try {
        const userEmail = req.user.email;
        const mailId = req.params.id;

        if (!mailId) {
            return res.status(400).json({ error: "Missing mail ID" });
        }

        console.log(`Attempting to permanently delete mail ${mailId} by user ${userEmail}`);

        // Find and delete mail permanently - without .lean() so we can use methods
        const result = await Mail.findOneAndDelete({ 
            mailId,
            $or: [
                { sender: userEmail },
                { recipient: userEmail },
                { recipients: userEmail }
            ]
        });

        if (!result) {
            return res.status(404).json({ error: "Mail not found or not authorized" });
        }

        console.log(`Mail ${mailId} permanently deleted by ${userEmail}`);
        return res.status(200).json({ message: "Mail permanently deleted" });

    } catch (err) {
        console.error("Error permanently deleting mail:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
};

module.exports = {
    createMail,
    getMails,
    getMailById,
    updateMail,
    deleteMailById,
    searchMails,
    updateMailLabelsForUser,
    deleteMailByIdPermanently,
    // New APIs:
    archiveMail,
    toggleStarMail,
    addLabelToMail,
    removeLabelFromMail,
    getMailsByLabel,
    // Draft
    createDraft,
    sendDraft,
    getDrafts 
};