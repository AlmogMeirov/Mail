const express = require("express");

const app = express();

// Middlewares
app.use(express.json());         // Parses incoming JSON requests

// Routes
const router = require("./routes"); // Loads all route definitions from routes/index.js
app.use("/api", router);            // Mounts the API under /api prefix (e.g., /api/users)

// Fallback handler for unknown routes
app.use((req, res) => {
  res.status(404).json({ error: "Not found" });
});

module.exports = app;
