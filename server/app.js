const express = require('express');
const app = express();

// Middleware to parse JSON bodies
app.use(express.json());

// Import routes
// Routers
const usersRouter = require("./routes/authRoutes");
const mailsRouter = require("./routes/mailRoutes");

app.use("/api/users", usersRouter);
app.use("/api/mails", mailsRouter);
// Default fallback for unknown endpoints
app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint not found' });
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
