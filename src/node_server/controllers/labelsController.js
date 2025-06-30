const labelModel = require("../models/labels");

// Get all labels for the current user
function getAll(req, res) {
  const userId = req.user.email;
  const labels = labelModel.getAllLabels(userId);
  res.json(labels);
}

// Get a label by ID for the current user
function getById(req, res) {
  const userId = req.user.email;
  const label = labelModel.getLabelById(userId, req.params.id);
  if (!label) return res.status(404).json({ error: "Label not found" });
  res.json(label);
}

// Create a new label for the current user
function create(req, res) {
  const userId = req.user.email;
  const { name } = req.body;
  if (!name) return res.status(400).json({ error: "Name is required" });
  const label = labelModel.createLabel(userId, name);
  res.status(201).json(label);
}

// Update a label by ID for the current user
function update(req, res) {
  const userId = req.user.email;
  const { name } = req.body;
  const updated = labelModel.updateLabel(userId, req.params.id, name);
  if (!updated) return res.status(404).json({ error: "Label not found" });
  res.status(204).end();
}

// Delete a label by ID for the current user
function remove(req, res) {
  const userId = req.user.email;
  const deleted = labelModel.deleteLabel(userId, req.params.id);
  if (!deleted) return res.status(404).json({ error: "Label not found" });
  res.status(204).end();
}

// Search labels by substring in name
function search(req, res) {
  const userId = req.user.email;
  const query = req.params.query?.toLowerCase() || "";
  const labels = labelModel.getAllLabels(userId);
  const filtered = labels.filter(label => label.name.toLowerCase().includes(query));
  res.json(filtered);
}

/**Add in exercises 4**/

// Add a label to a mail
function tagMail(req, res) {
  const userId = req.user.email;
  const { mailId, labelId } = req.body;

  if (!mailId || !labelId) {
    return res.status(400).json({ error: "mailId and labelId are required" });
  }

  labelModel.addLabelToMail(userId, mailId, labelId);
  res.status(200).json({ success: true });
}

// Remove a label from a mail
function untagMail(req, res) {
  const userId = req.user.email;
  const { mailId, labelId } = req.body;

  if (!mailId || !labelId) {
    return res.status(400).json({ error: "mailId and labelId are required" });
  }

  const removed = labelModel.removeLabelFromMail(userId, mailId, labelId);
  if (!removed) return res.status(404).json({ error: "Label not found for mail" });
  res.status(200).json({ success: true });
}

// Get all mails associated with a label
function getMailsByLabel(req, res) {
  const userId = req.user.email;
  const { labelId } = req.params;

  const mails = labelModel.getMailsByLabel(userId, labelId);
  res.json(mails);

}
// Get all labels for a specific mail
function getLabelsForMail(req, res) {
  const userId = req.user.email;
  const { mailId } = req.params;

  const labels = labelModel.getLabelsForMail(userId, mailId);
  res.json(labels);
}

module.exports = {
  getAll,
  getById,
  create,
  update,
  remove,
  search,
  tagMail, //Add in exercises 4
  untagMail, //Add in exercises 4
  getMailsByLabel, //Add in exercises 4
  getLabelsForMail //Add in exercises 4
};
