const express = require("express");
const router = express.Router();

const usersRouter = require("./users");
const mailsRouter = require("./mails");
const blacklistRouter = require("./blacklist");
const labelsRouter = require("./labels");

router.use("/users", usersRouter);
router.use("/mails", mailsRouter);
router.use("/blacklist", blacklistRouter);
router.use("/labels", labelsRouter);

module.exports = router;
