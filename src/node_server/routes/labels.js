const express = require("express");
const controller = require("../controllers/labelsController");
const authenticateToken = require("../middlewares/authMiddleware"); // Import the authentication middleware to protect routes
const router = express.Router();
// Add authentication middleware to all routes in this router
router.use(authenticateToken);

router.get("/", controller.getAll);        // Get all labels for user
router.post("/", controller.create);       // Create new label for user
router.get("/:id", controller.getById);    // Get label by ID for user
router.patch("/:id", controller.update);   // Update label by ID for user
router.delete("/:id", controller.remove);  // Delete label by ID for user

module.exports = router;
