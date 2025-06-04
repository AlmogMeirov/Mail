const express = require('express');
const router = express.Router(); // Create a router object to define API routes
// Import the authController which contains the logic for user registration and login
const authController = require('../controllers/authController'); // Import the authMiddleware to protect routes that require authentication
const authenticateToken = require('../middleware/authMiddleware');
const multer = require('multer');
const upload = multer({ dest: 'uploads/' }); // Set up multer for file uploads, storing files in the 'uploads' directory

router.post('/', authController.register);
router.post('/tokens', authController.login);
router.post('/', upload.single('profileImage'), authController.register);
router.post('/tokens', authController.login);

module.exports = router; 
