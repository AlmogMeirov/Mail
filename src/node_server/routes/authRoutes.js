const express = require('express'); 
const router = express.Router(); // Create a router object to define API routes
// Import the authController which contains the logic for user registration and login
const authController = require('../controllers/authController'); // Import the authMiddleware to protect routes that require authentication
const authenticateToken = require('../middlewares/authMiddleware');
const multer = require('multer');
const upload = multer({ dest: 'uploads/' }); // Set up multer for file uploads, storing files in the 'uploads' directory

// When a POST request is made to /api/users, run the 'register' function from authController
router.post('/api/users', authController.register); 
// When a POST request is made to /api/tokens, run the 'login' function from authController
router.post('/api/tokens', authController.login); 

// Protected route - must include valid token
router.get('/api/users/:id', authenticateToken, authController.getProfile);

router.post('/api/users', upload.single('profileImage'), authController.register);

module.exports = router; 
