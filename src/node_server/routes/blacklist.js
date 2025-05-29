const express = require("express");
const router = express.Router();

// add a URL to the blacklist
router.post("/", (req, res) => {
  res.status(501).json({ error: "Not implemented" });
});

// remove a URL from the blacklist
router.delete("/:id", (req, res) => {
  res.status(501).json({ error: "Not implemented" });
});

module.exports = router;