const express = require('express');
const app = express();

// Middleware to parse JSON bodies
app.use(express.json());

// Import routes
const mailRoutes = require('./routes/mailRoutes');

// Load memory and initialize inboxes
const { inboxMap } = require('./controllers/mailController');
const memory = require('./memory/memory');

// Initialize in-memory inboxes for each user
// This is a temporary solution to ensure that each user has an inbox
// TODO: remove after real registration is implemented
memory.users.forEach(user => {
    if (!inboxMap.has(user.username)) {
        inboxMap.set(user.username, []);
        console.log(`Inbox initialized for ${user.username}`);
    }
});

// Register routes
app.use('/api/mails', mailRoutes);

// Default fallback for unknown endpoints
app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint not found' });
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
