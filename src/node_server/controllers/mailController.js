const { v4: uuidv4 } = require('uuid');
const fs = require('fs');
const path = require('path');
const { extractUrls } = require("../utils/extractUrls");
const { checkUrlBlacklist } = require("../utils/blacklistClient");
const inboxMap = require('../utils/inboxMap');


const createMail = async (req, res) => {
    const { sender, recipient, subject, content } = req.body;

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

    // Extract all URLs from subject + content
    const fullText = `${subject} ${content}`;
    const urls = extractUrls(fullText);

    try {
        // Check all URLs concurrently using the blacklist server
        const results = await Promise.all(urls.map(checkUrlBlacklist));

        // If any result is true (i.e., blacklisted), reject the request
        if (results.includes(true)) {
            return res.status(400).json({ error: 'Message contains blacklisted URL' });
        }
    } catch (err) {
        console.error('Error while checking blacklist:', err);
        return res.status(500).json({ error: 'Failed to validate message links' });
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

    res.status(201).json({ message: 'Mail sent successfully', mail: newMail });
};

const getMails = (req, res) => {
    try {
        if (!req.user || !req.user.email) {
            return res.status(401).json({ error: "Unauthorized: missing user data" });
        }

        const userEmail = req.user.email;

        if (!inboxMap || inboxMap.size === 0) {
            return res.status(200).json({ message: "No mails exist in the system", inbox: [], sent: [] });
        }

        const inbox = inboxMap.get(userEmail) || [];

        const sent = [];
        for (const [recipient, mails] of inboxMap.entries()) {
            mails.forEach(mail => {
                if (mail.sender === userEmail) {
                    sent.push(mail);
                }
            });
        }

        if (inbox.length === 0 && sent.length === 0) {
            return res.status(200).json({ message: "No mails found for this user", inbox: [], sent: [] });
        }

        res.status(200).json({
            message: "Mails fetched successfully",
            inbox,
            sent
        });

    } catch (err) {
        console.error("Failed to fetch mails:", err.message);
        res.status(500).json({ error: "Internal server error" });
    }
};


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
