// controllers/labelsController.js - Improved error messages
const { Label, MailLabel } = require("../models/labels");
const { extractUrls } = require("../utils/extractUrls");
const { addUrlToBlacklist } = require("../utils/blacklistClient");
const Mail = require("../models/Mail");

// System labels that cannot be created/edited/deleted
const SYSTEM_LABELS = ['inbox', 'sent', 'spam', 'drafts', 'starred', 'trash', 'important'];

async function getAll(req, res) {
  try {
    console.log("=== getAll function called ===");
    console.log("SYSTEM_LABELS:", SYSTEM_LABELS);
    
    const userId = req.user.email;
    const userLabels = await Label.getAllLabelsForUser(userId);
    
    console.log("userLabels from DB:", userLabels);

    // System labels (always available)
    const systemLabels = SYSTEM_LABELS.map(name => ({
      id: name,
      name: name,
      isSystem: true
    }));
    
    console.log("systemLabels:", systemLabels);

    // Custom labels  
    const customLabels = userLabels.map(label => ({
      id: label.labelId,
      name: label.name,
      isSystem: false
    }));

    console.log("Final result:", [...systemLabels, ...customLabels]);
    
    res.json([...systemLabels, ...customLabels]);
  } catch (err) {
    console.error("Error fetching labels:", err);
    res.status(500).json({ 
      error: "Unable to retrieve labels",
      message: "There was an issue loading your labels. Please try again."
    });
  }
}

// Get a label by ID for the current user
async function getById(req, res) {
  try {
    const userId = req.user.email;
    const label = await Label.getLabelById(userId, req.params.id);

    if (!label) {
      return res.status(404).json({ 
        error: "Label not found",
        message: "The requested label does not exist or you don't have permission to access it."
      });
    }

    res.json({
      id: label.labelId,
      name: label.name
    });
  } catch (err) {
    console.error("Error fetching label:", err);
    res.status(500).json({ 
      error: "Unable to retrieve label",
      message: "There was an issue loading the label details. Please try again."
    });
  }
}

// Check if label name is reserved system label (case-insensitive)
function isSystemLabel(labelName) {
    if (!labelName || typeof labelName !== 'string') {
        return false;
    }
    
    // Remove whitespace and convert to lowercase for comparison
    const cleanName = labelName.trim().toLowerCase();
    
    // Check against system labels array
    return SYSTEM_LABELS.some(systemLabel => 
        systemLabel.toLowerCase() === cleanName
    );
}

// Normalize label name for comparison (trim + lowercase)
function normalizeLabelName(labelName) {
    if (!labelName || typeof labelName !== 'string') {
        return '';
    }
    return labelName.trim().toLowerCase();
}

// Create new custom label with strict system label validation
async function create(req, res) {
  try {
    const userId = req.user.email;
    const { name } = req.body;

    // Validate input
    if (!name || !name.trim()) {
      return res.status(400).json({ 
        error: "Label name required",
        message: "Please provide a valid label name. Label name cannot be empty."
      });
    }

    const cleanName = name.trim();
    
    // Check for empty name after trimming
    if (cleanName.length === 0) {
      return res.status(400).json({ 
        error: "Invalid label name",
        message: "Label name cannot consist only of spaces. Please enter a valid name."
      });
    }

    // Check for name length limits
    if (cleanName.length > 50) {
      return res.status(400).json({ 
        error: "Label name too long",
        message: "Label name must be 50 characters or less. Please choose a shorter name."
      });
    }

    // Block any variation of system labels (inbox, INBOX, Inbox, etc.)
    if (isSystemLabel(cleanName)) {
      return res.status(400).json({ 
        error: "Reserved label name",
        message: `"${cleanName}" is a reserved system label and cannot be used as a custom label name. System labels include: ${SYSTEM_LABELS.join(", ")}.`
      });
    }

    // Check for duplicate labels (case-insensitive) in MongoDB
    const normalizedName = normalizeLabelName(cleanName);
    
    const existingLabel = await Label.findOne({
      userId,
      $expr: {
        $eq: [
          { $toLower: { $trim: { input: "$name" } } },
          normalizedName
        ]
      }
    });

    if (existingLabel) {
      return res.status(409).json({ 
        error: "Label already exists",
        message: `A label with the name "${cleanName}" already exists. Label names are case-insensitive, so "${existingLabel.name}" and "${cleanName}" are considered the same.`
      });
    }

    // Double-check against system labels array for safety
    const systemLabelConflict = SYSTEM_LABELS.some(systemLabel => 
      systemLabel.toLowerCase() === normalizedName
    );

    if (systemLabelConflict) {
      return res.status(400).json({ 
        error: "System label conflict",
        message: `Cannot create label "${cleanName}" as it conflicts with a system label. Please choose a different name.`
      });
    }

    const label = await Label.createLabelForUser(userId, cleanName);

    res.status(201).json({
      id: label.labelId,
      name: label.name,
      message: `Label "${label.name}" created successfully.`
    });
  } catch (err) {
    console.error("Error creating label:", err);
    if (err.code === 11000) { // MongoDB duplicate key error
      return res.status(409).json({ 
        error: "Duplicate label",
        message: "A label with this name already exists. Please choose a different name."
      });
    }
    res.status(500).json({ 
      error: "Unable to create label",
      message: "There was an issue creating your label. Please try again."
    });
  }
}

async function update(req, res) {
  try {
    const userId = req.user.email;
    const labelId = req.params.id;
    const { name } = req.body;

    // Validate input
    if (!name || !name.trim()) {
      return res.status(400).json({ 
        error: "Label name required",
        message: "Please provide a valid label name. Label name cannot be empty."
      });
    }

    const cleanName = name.trim();

    // Check for name length limits
    if (cleanName.length > 50) {
      return res.status(400).json({ 
        error: "Label name too long",
        message: "Label name must be 50 characters or less. Please choose a shorter name."
      });
    }

    // Prevent editing system labels by ID
    if (SYSTEM_LABELS.includes(labelId)) {
      return res.status(403).json({ 
        error: "Cannot edit system label",
        message: `System labels like "${labelId}" cannot be modified. Only custom labels can be edited.`
      });
    }

    // Prevent renaming to system label name
    if (isSystemLabel(cleanName)) {
      return res.status(400).json({ 
        error: "Reserved label name",
        message: `"${cleanName}" is a reserved system label name and cannot be used. System labels include: ${SYSTEM_LABELS.join(", ")}.`
      });
    }

    // Get the label before updating
    const oldLabel = await Label.getLabelById(userId, labelId);
    if (!oldLabel) {
      return res.status(404).json({ 
        error: "Label not found",
        message: "The label you're trying to update doesn't exist or you don't have permission to modify it."
      });
    }

    const oldName = oldLabel.name.toLowerCase();
    const newName = cleanName.toLowerCase();

    // Check if name actually changed
    if (oldName === newName) {
      return res.status(400).json({ 
        error: "No changes made",
        message: "The new label name is the same as the current name. Please provide a different name."
      });
    }

    // Check for name conflicts with other labels (case-insensitive)
    const normalizedNewName = normalizeLabelName(cleanName);
    
    const conflictingLabel = await Label.findOne({
      userId,
      labelId: { $ne: labelId }, // Not the same label
      $expr: {
        $eq: [
          { $toLower: { $trim: { input: "$name" } } },
          normalizedNewName
        ]
      }
    });

    if (conflictingLabel) {
      return res.status(409).json({ 
        error: "Label name already exists",
        message: `A label with the name "${cleanName}" already exists. Please choose a different name.`
      });
    }

    // Update the label in database
    const updated = await Label.updateLabelForUser(userId, labelId, cleanName);

    if (!updated) {
      return res.status(404).json({ 
        error: "Update failed",
        message: "Unable to update the label. It may have been deleted or you may not have permission."
      });
    }

    // Update all emails that use this label
    console.log(`[LABEL UPDATE] Updating mails: "${oldName}" → "${newName}" for user ${userId}`);
    
    const updateResult = await Mail.updateMany(
      { 
        'labels.userEmail': userId,
        'labels.labelIds': oldName 
      },
      { 
        $set: { 'labels.$[userLabel].labelIds.$[labelElement]': newName } 
      },
      { 
        arrayFilters: [
          { 'userLabel.userEmail': userId },
          { 'labelElement': oldName }
        ] 
      }
    );

    console.log(`[LABEL UPDATE] Updated ${updateResult.modifiedCount} mails`);

    res.status(200).json({
      id: updated.labelId,
      name: updated.name,
      message: `Label renamed from "${oldLabel.name}" to "${updated.name}" successfully.`,
      emailsUpdated: updateResult.modifiedCount
    });
  } catch (err) {
    console.error("Error updating label:", err);
    if (err.code === 11000) {
      return res.status(409).json({ 
        error: "Duplicate label name",
        message: "A label with this name already exists. Please choose a different name."
      });
    }
    res.status(500).json({ 
      error: "Unable to update label",
      message: "There was an issue updating your label. Please try again."
    });
  }
}

async function remove(req, res) {
  try {
    const userId = req.user.email;
    const labelId = req.params.id;

    // Prevent deleting system labels by ID
    if (SYSTEM_LABELS.includes(labelId)) {
      return res.status(403).json({ 
        error: "Cannot delete system label",
        message: `System labels like "${labelId}" cannot be deleted. Only custom labels can be removed. System labels include: ${SYSTEM_LABELS.join(", ")}.`
      });
    }

    // Get the label before deletion
    const labelToDelete = await Label.getLabelById(userId, labelId);
    if (!labelToDelete) {
      return res.status(404).json({ 
        error: "Label not found",
        message: "The label you're trying to delete doesn't exist or you don't have permission to remove it."
      });
    }

    // Additional check: prevent deleting if name is system label
    if (isSystemLabel(labelToDelete.name)) {
      return res.status(403).json({ 
        error: "Cannot delete system label",
        message: `"${labelToDelete.name}" is a system label and cannot be deleted.`
      });
    }

    const labelName = labelToDelete.name.toLowerCase();

    // Delete the label from database
    const deleted = await Label.deleteLabelForUser(userId, labelId);

    if (!deleted) {
      return res.status(404).json({ 
        error: "Deletion failed",
        message: "Unable to delete the label. It may have already been removed."
      });
    }

    // Remove the label from all emails
    console.log(`[LABEL DELETE] Removing label "${labelName}" from all mails for user ${userId}`);

    const removeResult = await Mail.updateMany(
      { 
        'labels.userEmail': userId,
        'labels.labelIds': labelName 
      },
      { 
        $pull: { 'labels.$[userLabel].labelIds': labelName } 
      },
      { 
        arrayFilters: [
          { 'userLabel.userEmail': userId }
        ] 
      }
    );

    console.log(`[LABEL DELETE] Removed label from ${removeResult.modifiedCount} mails`);

    // Clean up legacy MailLabel collection
    await MailLabel.deleteMany({ userId, labelId });

    res.status(200).json({
      message: `Label "${labelToDelete.name}" deleted successfully.`,
      emailsUpdated: removeResult.modifiedCount
    });
  } catch (err) {
    console.error("Error deleting label:", err);
    res.status(500).json({ 
      error: "Unable to delete label",
      message: "There was an issue deleting your label. Please try again."
    });
  }
}

// Search labels by substring in name
async function search(req, res) {
  try {
    const userId = req.user.email;
    const query = req.params.query?.toLowerCase() || "";

    if (query.length === 0) {
      return res.status(400).json({ 
        error: "Search query required",
        message: "Please provide a search term to find labels."
      });
    }

    const labels = await Label.searchLabelsForUser(userId, query);

    const formattedLabels = labels.map(label => ({
      id: label.labelId,
      name: label.name
    }));

    if (formattedLabels.length === 0) {
      return res.status(404).json({ 
        error: "No labels found",
        message: `No labels found matching "${query}". Try a different search term.`
      });
    }

    res.json({
      query: query,
      count: formattedLabels.length,
      labels: formattedLabels
    });
  } catch (err) {
    console.error("Error searching labels:", err);
    res.status(500).json({ 
      error: "Search failed",
      message: "There was an issue searching your labels. Please try again."
    });
  }
}

// Add a label to an email
async function tagMail(req, res) {
  try {
    const userId = req.user.email;
    const { mailId, labelId } = req.body;

    console.log(">> Entered tagMail! mailId =", mailId, "labelId =", labelId);

    if (!mailId || !labelId) {
      return res.status(400).json({ 
        error: "Missing required information",
        message: "Both email ID and label ID are required to add a label to an email."
      });
    }

    // Verify label exists and belongs to user
    const label = await Label.getLabelById(userId, labelId);
    if (!label) {
      return res.status(404).json({ 
        error: "Label not found",
        message: "The label you're trying to add doesn't exist or you don't have permission to use it."
      });
    }

    // Verify email exists and user has access
    const mail = await Mail.findOne({ mailId });
    if (!mail) {
      return res.status(404).json({ 
        error: "Email not found",
        message: "The email you're trying to label doesn't exist or you don't have access to it."
      });
    }

    if (!mail.isAccessibleBy(userId)) {
      return res.status(403).json({ 
        error: "Access denied",
        message: "You don't have permission to modify labels on this email."
      });
    }

    // Add label to email in MailLabel collection
    await MailLabel.findOneAndUpdate(
      { userId, mailId, labelId },
      { userId, mailId, labelId },
      { upsert: true, new: true }
    );

    // Update Mail document labels array for this user
    await Mail.findOneAndUpdate(
      { mailId, 'labels.userEmail': userId },
      { $addToSet: { 'labels.$.labelIds': labelId } }
    );

    // Create labels entry if none exists for this user
    const mailDoc = await Mail.findOne({ mailId, 'labels.userEmail': userId });
    if (!mailDoc) {
      await Mail.findOneAndUpdate(
        { mailId },
        { $push: { labels: { userEmail: userId, labelIds: [labelId] } } }
      );
    }

    // If user tagged email as spam, add URLs to blacklist
    if (label.name.toLowerCase() === "spam") {
      console.log("[SPAM] Mail tagged as spam, extracting URLs for blacklist");
      const urls = extractUrls(`${mail.subject} ${mail.content}`);
      console.log("[SPAM] Extracted URLs:", urls);

      for (const url of urls) {
        try {
          await addUrlToBlacklist(url);
          console.log(`[SPAM] Added URL to blacklist: ${url}`);
        } catch (err) {
          console.error("Failed to add URL to blacklist:", url, err.message);
        }
      }
    }

    res.status(200).json({ 
      success: true,
      message: `Label "${label.name}" added to email successfully.`
    });
  } catch (err) {
    console.error("Error tagging mail:", err);
    res.status(500).json({ 
      error: "Unable to add label",
      message: "There was an issue adding the label to your email. Please try again."
    });
  }
}

// Remove a label from an email
async function untagMail(req, res) {
  try {
    const userId = req.user.email;
    const { mailId, labelId } = req.body;

    if (!mailId || !labelId) {
      return res.status(400).json({ 
        error: "Missing required information",
        message: "Both email ID and label ID are required to remove a label from an email."
      });
    }

    // Remove from MailLabel collection
    const removed = await MailLabel.findOneAndDelete({ userId, mailId, labelId });

    if (!removed) {
      return res.status(404).json({ 
        error: "Label not found on email",
        message: "The label you're trying to remove is not currently applied to this email."
      });
    }

    // Remove from Mail document labels array
    await Mail.findOneAndUpdate(
      { mailId, 'labels.userEmail': userId },
      { $pull: { 'labels.$.labelIds': labelId } }
    );

    res.status(200).json({ 
      success: true,
      message: "Label removed from email successfully."
    });
  } catch (err) {
    console.error("Error untagging mail:", err);
    res.status(500).json({ 
      error: "Unable to remove label",
      message: "There was an issue removing the label from your email. Please try again."
    });
  }
}

// Get all emails associated with a label
async function getMailsByLabel(req, res) {
  try {
    const userId = req.user.email;
    const { labelId } = req.params;

    if (!labelId) {
      return res.status(400).json({ 
        error: "Label ID required",
        message: "Please provide a valid label ID to retrieve emails."
      });
    }

    const mailLabels = await MailLabel.getMailsByLabel(userId, labelId);
    const mailIds = mailLabels.map(ml => ml.mailId);

    res.json({
      labelId: labelId,
      count: mailIds.length,
      mailIds: mailIds
    });
  } catch (err) {
    console.error("Error fetching mails by label:", err);
    res.status(500).json({ 
      error: "Unable to retrieve emails",
      message: "There was an issue retrieving emails for this label. Please try again."
    });
  }
}

// Get all labels for a specific email
async function getLabelsForMail(req, res) {
  try {
    const userId = req.user.email;
    const { mailId } = req.params;

    if (!mailId) {
      return res.status(400).json({ 
        error: "Email ID required",
        message: "Please provide a valid email ID to retrieve labels."
      });
    }

    const mailLabels = await MailLabel.getLabelsForMail(userId, mailId);
    const labelIds = mailLabels.map(ml => ml.labelId);

    res.json({
      mailId: mailId,
      count: labelIds.length,
      labelIds: labelIds
    });
  } catch (err) {
    console.error("Error fetching labels for mail:", err);
    res.status(500).json({ 
      error: "Unable to retrieve labels",
      message: "There was an issue retrieving labels for this email. Please try again."
    });
  }
}

module.exports = {
  getAll,
  getById,
  create,
  update,
  remove,
  search,
  tagMail,
  untagMail,
  getMailsByLabel,
  getLabelsForMail,
  // Export helper functions for testing
  isSystemLabel,
  normalizeLabelName
};