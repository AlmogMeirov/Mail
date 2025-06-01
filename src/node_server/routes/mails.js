const express = require("express");
const router = express.Router();

// get list of all mails
// This route is not implemented yet
router.get("/", (req, res) => {
  res.status(501).json({ error: "Not implemented" });
});

// create a new mail
router.post("/", (req, res) => {
  res.status(501).json({ error: "Not implemented" });
});

// get a specific mail by ID
router.get("/:id", (req, res) => {
  res.status(501).json({ error: "Not implemented" });
});

// update a specific mail by ID
router.patch("/:id", (req, res) => {
  res.status(501).json({ error: "Not implemented" });
});

// delete a specific mail by ID
router.delete("/:id", (req, res) => {
  res.status(501).json({ error: "Not implemented" });
});

// search mails by query
router.get("/search/:query", (req, res) => {
  res.status(501).json({ error: "Not implemented" });
});

module.exports = router;