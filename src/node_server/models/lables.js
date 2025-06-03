const { v4: uuidv4 } = require("uuid"); // Import UUID generator

const labels = new Map(); // key: id, value: { id, name }

// Return all labels as an array
function getAllLabels() {
  return Array.from(labels.values());
}

// Get a label by its ID, or null if not found
function getLabelById(id) {
  return labels.get(id) || null;
}

// Create a new label with a unique ID
function createLabel(name) {
  const id = uuidv4();
  const newLabel = { id, name };
  labels.set(id, newLabel);
  return newLabel;
}

// Update the name of an existing label by ID
function updateLabel(id, name) {
  if (!labels.has(id)) return null;
  const updated = { id, name };
  labels.set(id, updated);
  return updated;
}

// Delete a label by ID, return true if deleted
function deleteLabel(id) {
  return labels.delete(id);
}

// Export label model functions
module.exports = {
  getAllLabels,
  getLabelById,
  createLabel,
  updateLabel,
  deleteLabel,
};
