const express = require("express");
const router = express.Router();

const usersRouter = require("../../../server/routes/authRoutes")
const mailsRouter = require("../../../server/routes/mailRoutes");
//const blacklistRouter = require("../../src/node_server/routes/blacklist");
const labelsRouter = require("../../../server/routes/labels");

router.use("/users", usersRouter);
router.use("/mails", mailsRouter);
//router.use("/blacklist", blacklistRouter);
router.use("/labels", labelsRouter);

module.exports = router;
