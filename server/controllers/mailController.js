const { v4: uuidv4 } = require('uuid');
const fs = require('fs');
const path = require('path');

// In-memory inbox store
const inboxMap = new Map();

const createMail = (req, res) => {
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

    // Load blocked URLs
    const filePath = path.join(__dirname, '../../data/urls.txt');
    let blockedUrls;
    try {
        const fileContent = fs.readFileSync(filePath, 'utf-8');
        blockedUrls = fileContent.split('\n').map(line => line.trim()).filter(line => line);
    } catch (err) {
        console.error('Failed to read blocked URLs:', err);
        return res.status(500).json({ error: 'Failed to validate message links' });
    }

    // Check if message contains any blacklisted URL
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

    res.status(201).json({ message: 'Mail sent successfully', mail: newMail });
};

module.exports = {
    createMail,
    inboxMap,
};
