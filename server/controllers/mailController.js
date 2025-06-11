const { v4: uuidv4 } = require('uuid');
const fs = require('fs');
const path = require('path');
// This module provides functions to manage mails in an in-memory store.
const inboxMap = new Map(); // Stores user inboxes in-memory


function createMail(req, res) {
    const sender = req.user.email; // Extract sender email from the decoded JWT token
    const { recipient, subject, content } = req.body;

    // Basic validation
    if (!recipient || !subject || !content) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    // Ensure sender and recipient exist in memory
    if (!inboxMap.has(sender)) {
        return res.status(400).json({ error: 'Sender does not exist' });
    }
    if (!inboxMap.has(recipient)) {
        return res.status(400).json({ error: 'Recipient does not exist' });
    }
    if (sender !== req.user.email) {
        return res.status(403).json({ error: 'Sender email does not match authenticated user' });
    }


    // Load list of blocked URLs
    const filePath = path.join(__dirname, '../../data/urls.txt');
    let blockedUrls;
    try {
        const fileContent = fs.readFileSync(filePath, 'utf-8');
        blockedUrls = fileContent.split('\n').map(line => line.trim()).filter(line => line);
    } catch (err) {
        console.error('Failed to read blocked URLs:', err);
        return res.status(500).json({ error: 'Failed to validate message links' });
    }

    // Check if the message contains any blacklisted URL
    const fullText = `${subject} ${content}`;
    const foundBlocked = blockedUrls.find(url => fullText.includes(url));
    if (foundBlocked) {
        return res.status(400).json({ error: `Message contains blocked URL: ${foundBlocked}` });
    }

    // Create mail object
    const newMail = {
        id: uuidv4(),
        sender,
        recipient,
        subject,
        content,
        timestamp: new Date().toISOString(),
    };

    // Store mail in recipient's inbox
    inboxMap.get(recipient).push(newMail);

    // Store mail in sender's inbox (marked as sent = true)
    inboxMap.get(sender).push({ ...newMail, sent: true });

    // Respond with success
    res.status(201).json({ message: 'Mail sent successfully', mail: newMail });
}

function getMails(req, res) {
    const username = req.user.email; // Securely extracted from the verified JWT

    // Collect all mails where the user is either the sender or the recipient
    const allMails = [];
    for (const inbox of inboxMap.values()) {
        inbox.forEach(mail => {
            if (mail.sender === username || mail.recipient === username) {
                allMails.push(mail);
            }
        });
    }

    // Sort by timestamp (descending) and limit to the latest 50 mails
    const sorted = allMails.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    const result = sorted.slice(0, 50).map(mail => ({
        id: mail.id,
        subject: mail.subject,
        timestamp: mail.timestamp,
        direction: mail.sender === username ? 'sent' : 'received'
    }));

    // Optional debug logging
    result.forEach((mail, i) => {
        console.log(`${i + 1}. [${new Date(mail.timestamp).toLocaleString()}] ${mail.subject}`);
    });

    res.json(result);
}

// This function retrieves a mail by its ID and ensures the requesting user is authorized
function getMailById(req, res) {
    const userEmail = req.user.email; // Extracted securely from the verified JWT
    const mailId = req.params.id;

    if (!mailId) {
        return res.status(400).json({ error: "Missing mail ID" });
    }

    // Search through all inboxes for the requested mail
    for (const inbox of inboxMap.values()) {
        for (const mail of inbox) {
            if (mail.id === mailId) {
                // Check if the requesting user is the sender or recipient
                if (mail.sender === userEmail || mail.recipient === userEmail) {
                    return res.status(200).json(mail);
                } else {
                    return res.status(403).json({ error: "You are not authorized to view this mail" });
                }
            }
        }
    }

    // If no mail was found with that ID
    return res.status(404).json({ error: "Mail not found" });
}

// This function updates a mail by its ID and ensures the user is authorized to edit it.
function updateMail(req, res) {
    const userEmail = req.user.email; // Extracted securely from JWT
    const mailId = req.params.id;

    if (!mailId) {
        return res.status(400).json({ error: "Missing mail ID" });
    }

    // Iterate through all inboxes to find the mail by ID
    for (const inbox of inboxMap.values()) {
        for (let mail of inbox) {
            if (mail.id === mailId) {
                // Only the sender is allowed to edit the mail
                if (mail.sender !== userEmail) {
                    return res.status(403).json({ error: "You are not authorized to edit this mail" });
                }

                // Apply updates if fields are provided
                const { subject, content } = req.body;
                if (subject !== undefined) mail.subject = subject;
                if (content !== undefined) mail.content = content;

                return res.status(200).json({ message: "Mail updated successfully", mail });
            }
        }
    }

    // Mail with specified ID was not found
    return res.status(404).json({ error: "Mail not found" });
}

// This function deletes a mail by its ID if the user is authorized (as sender or recipient).
function deleteMailById(req, res) {
    const userEmail = req.user.email; // Extracted from JWT
    const mailId = req.params.id;

    if (!mailId) {
        return res.status(400).json({ error: "Missing mail ID" });
    }

    let mailFound = false;

    // Iterate through inboxMap and remove the mail if it belongs to the user
    for (const [username, inbox] of inboxMap.entries()) {
        const index = inbox.findIndex(mail => mail.id === mailId);

        if (index !== -1) {
            const mail = inbox[index];

            // Allow deletion only if the current user is sender or recipient
            if (mail.sender === userEmail || mail.recipient === userEmail) {
                inbox.splice(index, 1);
                mailFound = true;
                console.log(`Mail ${mailId} deleted for user '${username}'`);
            }
        }
    }

    if (mailFound) {
        return res.status(200).json({ message: "Mail deleted successfully" });
    } else {
        return res.status(404).json({ error: "Mail not found or not authorized to delete" });
    }
}

// This function searches for mails that match a query string in the user's inbox
function searchMails(req, res) {
    const userEmail = req.user.email; // Authenticated user from JWT middleware
    const query = req.query.q;

    // Validate input
    if (!query) {
        return res.status(400).json({ error: "Missing search query" });
    }

    const q = query.toLowerCase();
    const inbox = inboxMap.get(userEmail);

    if (!inbox) {
        return res.status(404).json({ error: "Inbox not found" });
    }

    const results = inbox.filter(mail => {
        const subject = mail.subject?.toLowerCase() || "";
        const content = mail.content?.toLowerCase() || "";
        const sender = mail.sender?.toLowerCase() || "";
        const recipient = mail.recipient?.toLowerCase() || "";

        return (
            subject.includes(q) ||
            content.includes(q) ||
            sender.includes(q) ||
            recipient.includes(q)
        );
    });

    if (results.length === 0) {
        return res.status(404).json({ error: "No matching mails found" });
    }

    return res.json(results.map(mail => ({
        id: mail.id,
        subject: mail.subject,
        timestamp: mail.timestamp,
        direction: mail.sender === userEmail ? "sent" : "received"
    })));
}






// This module provides functions to manage mails in an in-memory store.
// It includes creating, retrieving, updating, deleting, and searching mails.
module.exports = {
    createMail,
    getMails,
    getMailById,
    updateMail,
    deleteMailById,
    searchMails,
    inboxMap,
};
