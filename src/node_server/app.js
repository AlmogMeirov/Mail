// NOTE: comments in English only
require("dotenv").config();
const express = require("express");
const cors = require("cors");
const mongoose = require("mongoose");

const app = express();

// --- 0) Basic request log (kept from your version) ---
app.use((req, _res, next) => {
  console.log(`[SERVER] ${req.method} ${req.url}`);
  next();
});

// --- 1) Core middlewares ---
app.use(cors());                          // allow cross-origin
app.use(express.json({ limit: "2mb" }));  // allow base64 avatars in JSON

// --- 2) Mongo connection (Mongoose) ---
mongoose.set("strictQuery", true);
mongoose
  .connect(process.env.MONGO_URI, {
    serverSelectionTimeoutMS: 8000,
    maxPoolSize: 10,
  })
  .then(() => console.log("[DB] Mongo connected"))
  .catch((e) => {
    console.error("[DB] connect error:", e.message);
    process.exit(1); // fail fast on boot if DB is required
  });

mongoose.connection.on("disconnected", () => console.warn("[DB] disconnected"));
mongoose.connection.on("reconnected", () => console.warn("[DB] reconnected"));

// --- 3) TCP Client wrapper (kept) ---
const blacklistClient = require("./utils/blacklistClient");
app.set("blacklistClient", blacklistClient);

// --- 4) Routes ---
const router = require("./routes");       // your main router index
app.use("/api", router);

// NOTE: comments in English only
//const listEndpoints = require('express-list-endpoints');
//console.log(listEndpoints(app));


// Optional: lightweight health check
app.get("/health", (_req, res) => {
  const dbUp = mongoose.connection.readyState === 1; // 1 = connected
  res.json({ ok: true, db: dbUp ? "up" : "down" });
});

// --- 5) 404 fallback ---
app.use((req, res) => {
  res.status(404).json({ error: "Not found" });
});

// --- 6) Start AFTER DB is ready ---
const PORT = process.env.PORT || 3000;
mongoose.connection.once("open", () => {
  app.listen(PORT, () => console.log(`Server is running on port ${PORT}`));
});

// --- 7) Graceful shutdown ---
const shutdown = async () => {
  console.log("\n[SYS] Shutting down...");
  await mongoose.connection.close();
  process.exit(0);
};
process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
