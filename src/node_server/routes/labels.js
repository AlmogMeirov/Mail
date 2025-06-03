const express = require("express");
const controller = require("../controllers/labelsController");

const router = express.Router(); // Create a new Express router

router.get("/", controller.getAll); // Route to get all labels
router.post("/", controller.create); // Route to create a new label
router.get("/:id", controller.getById); // Route to get a label by ID
router.patch("/:id", controller.update); // Route to update a label by ID
router.delete("/:id", controller.remove); // Route to delete a label by ID

module.exports = router; // Export the router to be used in the main app
