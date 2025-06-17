const { v4: uuidv4 } = require("uuid");

// key: userId (email), value: Map of labels: labelId => { id, name }
const labels = new Map();

// Ensure the user's label map exists
function getUserMap(userId) {
  if (!labels.has(userId)) {
    labels.set(userId, new Map());
  }
  return labels.get(userId);
}

function getAllLabels(userId) {
  const userMap = getUserMap(userId);
  return Array.from(userMap.values());
}

function getLabelById(userId, labelId) {
  const userMap = getUserMap(userId);
  return userMap.get(labelId) || null;
}

function createLabel(userId, name) {
  if (labelNameExists(userId, name)) {
    throw new Error(`Label with name "${name}" already exists`);
  }

  const userMap = getUserMap(userId);
  const id = uuidv4();
  const newLabel = { id, name };
  userMap.set(id, newLabel);
  return newLabel;
}

function updateLabel(userId, labelId, name) {
  const userMap = getUserMap(userId);
  if (!userMap.has(labelId)) return null;
  const updated = { id: labelId, name };
  userMap.set(labelId, updated);
  return updated;
}

function deleteLabel(userId, labelId) {
  const userMap = getUserMap(userId);
  return userMap.delete(labelId);
}

function labelNameExists(userId, name) {
  const userMap = getUserMap(userId);
  name = name.trim().toLowerCase();
  for (const label of userMap.values()) {
    if (label.name.trim().toLowerCase() === name) {
      return true;
    }
  }
  return false;
}


module.exports = {
  // Export functions for label operations
  getUserMap,
  getAllLabels,
  getLabelById,
  createLabel,
  updateLabel,
  deleteLabel,
  labelNameExists,
};
