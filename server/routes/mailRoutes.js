// Description: Defines routes for handling mail-related operations
const express = require('express');
const router = express.Router();
const mailController = require('../controllers/mailController');

// Import JWT authentication middleware
const authenticateToken = require('../middleware/authMiddleware');

// Apply authentication middleware to all mail routes
router.get('/', authenticateToken, mailController.getMails);
router.post('/', authenticateToken, mailController.createMail);
router.get('/:id', authenticateToken, mailController.getMailById);
router.patch('/:id', authenticateToken, mailController.updateMail);
router.delete('/:id', authenticateToken, mailController.deleteMailById);
router.get('/search/:query', authenticateToken, mailController.searchMails);


module.exports = router;
