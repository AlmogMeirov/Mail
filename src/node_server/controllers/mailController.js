const { v4: uuidv4 } = require('uuid');
const Mail = require('../models/Mail'); // MongoDB Mail model
const User = require('../models/User'); // User model for validation
const { extractUrls } = require("../utils/extractUrls");
const { checkUrlBlacklist } = require("../utils/blacklistClient");
// const { getAllLabels } = require('../models/labels'); // הגב זמנית
// const labelModel = require('../models/labels'); // הגב זמנית
const { addUrlToBlacklist } = require("../utils/blacklistClient");

// הוסף פונקציות זמניות במקום labelModel:
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

const getAllLabels = (userId) => {
    // זמני - מחזיר labels בסיסיים
    return [
        { id: 'inbox', name: 'inbox' },
        { id: 'spam', name: 'spam' },
        { id: 'sent', name: 'sent' },
        { id: 'drafts', name: 'drafts' }
    ];
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
        let finalLabels = labels;
        try {
            const urls = extractUrls(`${subject} ${content}`);
            const results = await Promise.all(urls.map(checkUrlBlacklist));
            if (results.includes(true)) {
                console.log("Message contains blacklisted URL, marking as spam");

                const sent = [];
                for (const r of recipientsList) {
                    let spamLabel = getAllLabels(r).find(l => l.name.toLowerCase() === "spam");
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

                    // *** תיקון: הוסף await וזמני לא עושה כלום ***
                    await labelModel.addLabelToMail(sender, mail.mailId, spamLabel.id);
                    await labelModel.addLabelToMail(r, mail.mailId, spamLabel.id);
                }

                return res.status(201).json({ message: "Mail sent to spam", sent });
            }
        } catch (err) {
            console.error("Error while checking blacklist:", err);
            return res.status(500).json({ error: "Failed to validate message links" });
        }

        // Create and save mails for each recipient
        const sent = [];
        for (const r of recipientsList) {
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

            // *** תיקון: הוסף await להוספת labels ***
            for (const labelId of labels) {
                await labelModel.addLabelToMail(sender, mail.mailId, labelId);
            }
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

        if (inbox.length === 0 && sent.length === 0) {
            return res.status(200).json({
                message: "No mails found for this user",
                inbox: [],
                sent: [],
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
        const query = req.query.q;

        if (!query) {
            return res.status(400).json({ error: "Missing search query" });
        }

        const q = query.toLowerCase();

        // Search in both inbox and sent mails
        const mails = await Mail.findByUser(userEmail).lean();

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

        const allowed = getAllLabels(userEmail).map(l => l.name.toLowerCase());
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

module.exports = {
    createMail,
    getMails,
    getMailById,
    updateMail,
    deleteMailById,
    searchMails,
    updateMailLabelsForUser
};