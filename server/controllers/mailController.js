const { v4: uuidv4 } = require('uuid');
const fs = require('fs');
const path = require('path');
const { extractUrls } = require("../utils/extractUrls");
const { checkUrlBlacklist } = require("../utils/blacklistClient");

// In-memory inbox store
const inboxMap = new Map();

const createMail = async (req, res) => {
    const { sender, recipient, subject, content } = req.body;

    // Basic validation
    if (!sender || !recipient || !subject || !content) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    // Ensure sender and recipient exist
    if (!inboxMap.has(sender)) {
        return res.status(400).json({ error: 'Sender does not exist' });
    }
    if (!inboxMap.has(recipient)) {
        return res.status(400).json({ error: 'Recipient does not exist' });
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
    if (sender === recipient) {
        inboxMap.get(sender).push(newMail);
    } else {
        inboxMap.get(sender).push({ ...newMail, sent: true });
        inboxMap.get(recipient).push(newMail);
    }

    res.status(201).json({ message: 'Mail sent successfully', mail: newMail });
};

// Temporary user list for development only
function getMails(req, res) {
    const username = req.headers['user-id'];
    // Check if the user-id header is present
    if (!username) {
        console.warn("Missing 'user-id' header");
        return res.status(401).json({ error: "Missing 'user-id' header" });
    }
    // Check if the user exists in the inboxMap
    const allMails = [];

    for (const inbox of inboxMap.values()) {
        inbox.forEach(mail => {
            if (mail.sender === username || mail.recipient === username) {
                allMails.push(mail);
            }
        });
    }
    // or you can return a message indicating no mails found
    const sorted = allMails.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    const result = sorted.slice(0, 50).map(mail => ({
        id: mail.id,
        subject: mail.subject,
        timestamp: mail.timestamp,
        direction: mail.sender === username ? 'sent' : 'received'
    }));

    result.forEach((mail, i) => {
        console.log(`${i + 1}. [${new Date(mail.timestamp).toLocaleString()}] ${mail.subject}`);
    });

    res.json(result);
}
// This function retrieves a mail by its ID and checks if the user is authorized to access it.
function getMailById(req, res) {
    const userId = req.headers['user-id'];
    const mailId = req.params.id;
    // Check if the user-id header is present
    if (!userId) {
        return res.status(400).json({ error: "Missing user-id header" });
    }
    // Check if the mail ID is provided
    if (!mailId) {
        return res.status(400).json({ error: "Missing mail ID" });
    }
    // Check if the user exists in the inboxMap
    for (const inbox of inboxMap.values()) {
        for (const mail of inbox) {
            if (mail.id === mailId) {
                // Check if the user is authorized to access this mail
                if (mail.sender === userId || mail.recipient === userId) {
                    return res.json(mail);
                } else {
                    return res.status(403).json({ error: "Unauthorized to access this mail" });
                }
            }
        }
    }

    return res.status(404).json({ error: "Mail not found" });
}
// This function updates a mail by its ID and checks if the user is authorized to edit it.
function updateMail(req, res) {
    const userId = req.header("user-id");
    const mailId = req.params.id;
    // Check if the user-id header is present
    if (!userId) {
        return res.status(400).json({ error: "Missing 'user-id' header" });
    }
    // Check if the mail ID is provided
    let foundMail = null;

    for (const inbox of inboxMap.values()) {
        for (let mail of inbox) {
            if (mail.id === mailId) {
                if (mail.sender !== userId) {
                    return res.status(403).json({ error: "Unauthorized to edit this mail" });
                }
                //  Check if the mail is found
                const { subject, content } = req.body;
                if (subject !== undefined) mail.subject = subject;
                if (content !== undefined) mail.content = content;

                return res.json({ message: "Mail updated", mail });
            }
        }
    }

    res.status(404).json({ error: "Mail not found" });
}
// This function deletes a mail by its ID and checks if the user is authorized to delete it.
function deleteMailById(req, res) {
    const userId = req.headers['user-id'];
    const mailId = req.params.id;

    if (!userId) {
        return res.status(401).json({ error: "Missing user-id header" });
    }
    // Check if the mail ID is provided
    // Check if the user exists in the inboxMap
    // Check if the mail ID is provided
    for (const [username, inbox] of inboxMap.entries()) {
        const index = inbox.findIndex(mail => mail.id === mailId);

        if (index !== -1) {
            const mail = inbox[index];

            if (mail.sender !== userId && mail.recipient !== userId) {
                return res.status(403).json({ error: "Unauthorized to delete this mail" });
            }
            // If the mail is found, delete it
            inbox.splice(index, 1);
            console.log(`Deleted mail ${mailId} for user '${username}'`);
            return res.json({ message: "Mail deleted successfully" });
        }
    }

    return res.status(404).json({ error: "Mail not found" });
}
// This function searches for mails based on a query string and returns matching results.
function searchMails(req, res) {
    const userId = req.header('x-user-id');
    const query = req.params.query.toLowerCase();

    if (!userId || !inboxMap.has(userId)) {
        return res.status(404).json({ error: 'User not found' });
    }

    if (!query) {
        return res.status(400).json({ error: 'Missing search query' });
    }

    const userInbox = inboxMap.get(userId);
    const q = query.toLowerCase();

    // Filter the user's inbox for mails that match the query
    const matchingMails = userInbox.filter(mail => {
        if (!mail) return false;
        if (mail.sender !== userId && mail.recipient !== userId) return false;
        if (!mail.subject || !mail.content || !mail.sender || !mail.recipient) return false;
        const subject = mail.subject.toLowerCase();
        const content = mail.content.toLowerCase();
        const sender = mail.sender.toLowerCase();
        const recipient = mail.recipient.toLowerCase();
        // Return true if any of the fields match the query
        if (subject.includes(q) || content.includes(q) || sender.includes(q) || recipient.includes(q)) {
            return true;
        }
    });


    if (matchingMails.length === 0) {
        return res.status(404).json({ error: 'No matching mails found' });
    }
    // Return the matching mails with limited fields
    res.json(matchingMails.map(mail => ({
        id: mail.id,
        subject: mail.subject,
        timestamp: mail.timestamp,
        direction: mail.sender === userId ? 'sent' : 'received'
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
