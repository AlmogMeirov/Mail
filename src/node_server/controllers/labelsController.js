const labelModel = require("../models/labels");

// Get all labels and return as JSON
function getAll(req, res) {
  res.json(labelModel.getAllLabels());
}


// Get a label by ID, return 404 if not found
function getById(req, res) {
  const label = labelModel.getLabelById(req.params.id);
  if (!label) return res.status(404).json({ error: "Label not found" });
  res.json(label);
}

// Create a new label, require 'name' in request body
function create(req, res) {
  const { name } = req.body;
  if (!name) return res.status(400).json({ error: "Name is required" });
  const label = labelModel.createLabel(name);
  res.status(201).json(label);
}

// Update an existing label by ID, require 'name' in request body
function update(req, res) {
  const { name } = req.body;
  const updated = labelModel.updateLabel(req.params.id, name);
  if (!updated) return res.status(404).json({ error: "Label not found" });
  res.status(204).end();
}

// Delete a label by ID, return 204 on success, 404 if not found
function remove(req, res) {
  const deleted = labelModel.deleteLabel(req.params.id);
  if (!deleted) return res.status(404).json({ error: "Label not found" });
  res.status(204).end();
}

// Export controller functions
module.exports = {
  getAll,
  getById,
  create,
  update,
  remove,
};
