const express = require("express");
const router = express.Router();
//import of the varibles
const usersRouter = require('./authRoutes');
const mailsRouter = require("./mailRoutes");
const blacklistRouter = require("./blacklist"); 
const labelsRouter = require("./labels");
//made the routes
router.use("/users", usersRouter);
router.use("/mails", mailsRouter);
router.use("/blacklist", blacklistRouter); 
router.use("/labels", labelsRouter);

module.exports = router;