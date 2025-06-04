const express = require("express");
const app = express();

// Middleware to parse JSON bodies
app.use(express.json());

// Load routes
const router = require("./routes");
app.use("/api", router);

// TCP Client connection to C++ server (for blacklist checking)
const TcpClient = require("./utils/blacklistClient");
const blacklistClient = new TcpClient("127.0.0.1", 12345); // Replace with actual IP/port
blacklistClient.connect()
    .then(() => {
        console.log("Connected to C++ blacklist server");
        // Optional: expose client for reuse
        app.set("blacklistClient", blacklistClient);
    })
    .catch((err) => {
        console.error("Failed to connect to blacklist server:", err);
    });

// Fallback for unknown endpoints
app.use((req, res) => {
    res.status(404).json({ error: "Not found" });
});

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
