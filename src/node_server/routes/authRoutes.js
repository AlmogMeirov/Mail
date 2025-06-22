const express = require('express');
const router = express.Router(); // Create a router object to define API routes

const authController = require('../controllers/authController'); // Controller with register & login logic
const authenticateToken = require('../middlewares/authMiddleware');
const multer = require('multer');
const upload = multer({ dest: 'uploads/' }); // Configure multer to save uploaded files to 'uploads/' directory

// Register route (with optional profile image upload)
router.post('/register', upload.single('profileImage'), authController.register);

// Login route (issues JWT token)
router.post('/login', authController.login);

// Example of a protected route (uncomment if needed)
// router.get('/:id', authenticateToken, authController.getProfile);

module.exports = router;
