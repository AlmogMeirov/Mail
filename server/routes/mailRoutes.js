// Description: This file defines the routes for handling mail-related operations in the application.
// This module defines the routes for handling mail-related operations in the application.\
const express = require('express');
const router = express.Router();
// Import the mail controller functions
const { createMail } = require('../controllers/mailController');
const mailController = require('../controllers/mailController');
// This module defines the routes for handling mail-related operations in the application.
// It includes routes for creating, retrieving, updating, deleting, and searching mails.
router.get('/', mailController.getMails);
router.post('/', mailController.createMail);
router.get('/:id', mailController.getMailById);
router.patch('/:id', mailController.updateMail);
router.delete('/:id', mailController.deleteMailById);
router.get('/search/:query', mailController.searchMails);
// This module defines the routes for handling mail-related operations in the application.
module.exports = router;
