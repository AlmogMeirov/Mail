const express = require("express");

const app = express();

// Middlewares
app.use(express.json());         // Parses incoming JSON requests

// Routes


// Fallback handler for unknown routes
app.use((req, res) => {
  res.status(404).json({ error: "Not found" });
});

module.exports = app;
