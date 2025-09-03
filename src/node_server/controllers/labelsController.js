// controllers/labelsController.js - Updated for MongoDB
const { Label, MailLabel } = require("../models/labels");
const { extractUrls } = require("../utils/extractUrls");
const { addUrlToBlacklist } = require("../utils/blacklistClient");
const Mail = require("../models/Mail");
// תוויות מערכת שלא ניתן ליצור/לערוך/למחוק
const SYSTEM_LABELS = ['inbox', 'sent', 'spam', 'drafts', 'starred', 'trash', 'important'];

async function getAll(req, res) {
  try {
    console.log("=== getAll function called ===");
    console.log("SYSTEM_LABELS:", SYSTEM_LABELS);
    
    const userId = req.user.email;
    const userLabels = await Label.getAllLabelsForUser(userId);
    
    console.log("userLabels from DB:", userLabels);

    // תוויות מערכת (תמיד זמינות)
    const systemLabels = SYSTEM_LABELS.map(name => ({
      id: name,
      name: name,
      isSystem: true
    }));
    
    console.log("systemLabels:", systemLabels);

    // תוויות מותאמות אישית  
    const customLabels = userLabels.map(label => ({
      id: label.labelId,
      name: label.name,
      isSystem: false
    }));

    console.log("Final result:", [...systemLabels, ...customLabels]);
    
    res.json([...systemLabels, ...customLabels]);
  } catch (err) {
    console.error("Error fetching labels:", err);
    res.status(500).json({ error: "Internal server error" });
  }
}

// Get a label by ID for the current user
async function getById(req, res) {
  try {
    const userId = req.user.email;
    const label = await Label.getLabelById(userId, req.params.id);

    if (!label) {
      return res.status(404).json({ error: "Label not found" });
    }

    res.json({
      id: label.labelId,
      name: label.name
    });
  } catch (err) {
    console.error("Error fetching label:", err);
    res.status(500).json({ error: "Internal server error" });
  }
}

// הוסף בתחילת labelsController.js:



// פונקציה לבדיקה אם זו תווית מערכת
function isSystemLabel(labelName) {
    return SYSTEM_LABELS.includes(labelName.toLowerCase());
}

// עדכן את פונקציה create:
async function create(req, res) {
  try {
    const userId = req.user.email;
    const { name } = req.body;

    if (!name || !name.trim()) {
      return res.status(400).json({ error: "Name is required" });
    }

    // בדוק אם זו תווית מערכת
    if (isSystemLabel(name.trim())) {
      return res.status(400).json({ 
        error: "Cannot create system label. System labels are: " + SYSTEM_LABELS.join(", ") 
      });
    }

    // Check if label with this name already exists for user
    const existingLabel = await Label.findOne({
      userId,
      name: name.trim()
    });

    if (existingLabel) {
      return res.status(409).json({ error: "Label with this name already exists" });
    }

    const label = await Label.createLabelForUser(userId, name);

    res.status(201).json({
      id: label.labelId,
      name: label.name
    });
  } catch (err) {
    console.error("Error creating label:", err);
    if (err.code === 11000) { // Duplicate key error
      return res.status(409).json({ error: "Label with this name already exists" });
    }
    res.status(500).json({ error: "Internal server error" });
  }
}

async function update(req, res) {
  try {
    const userId = req.user.email;
    const labelId = req.params.id;
    const { name } = req.body;

    if (!name || !name.trim()) {
      return res.status(400).json({ error: "Name is required" });
    }

    // בדוק אם מנסים לערוך תווית מערכת (לפני חיפוש במסד)
    if (SYSTEM_LABELS.includes(labelId)) {
      return res.status(403).json({ 
        error: "Cannot edit system labels" 
      });
    }

    // בדוק אם השם החדש הוא תווית מערכת
    if (isSystemLabel(name.trim())) {
      return res.status(400).json({ 
        error: "Cannot use system label name. System labels are: " + SYSTEM_LABELS.join(", ") 
      });
    }

    // *** שלב 1: קבל את התווית הישנה לפני העדכון ***
    const oldLabel = await Label.getLabelById(userId, labelId);
    if (!oldLabel) {
      return res.status(404).json({ error: "Label not found" });
    }

    const oldName = oldLabel.name.toLowerCase(); // השם הישן
    const newName = name.trim().toLowerCase();   // השם החדש

    // *** שלב 2: עדכן את התווית ב-Labels collection ***
    const updated = await Label.updateLabelForUser(userId, labelId, name);

    if (!updated) {
      return res.status(404).json({ error: "Label not found" });
    }

    // *** שלב 3: עדכן את כל המיילים שמשתמשים בתווית הישנה ***
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
      name: updated.name
    });
  } catch (err) {
    console.error("Error updating label:", err);
    if (err.code === 11000) {
      return res.status(409).json({ error: "Label with this name already exists" });
    }
    res.status(500).json({ error: "Internal server error" });
  }
}

async function remove(req, res) {
  try {
    const userId = req.user.email;
    const labelId = req.params.id;

    // בדוק אם זו תווית מערכת קודם (לפני חיפוש במסד)
    if (SYSTEM_LABELS.includes(labelId)) {
      return res.status(403).json({ 
        error: "Cannot delete system labels. System labels are: " + SYSTEM_LABELS.join(", ") 
      });
    }

    // *** שלב 1: קבל את התווית לפני המחיקה ***
    const labelToDelete = await Label.getLabelById(userId, labelId);
    if (!labelToDelete) {
      return res.status(404).json({ error: "Label not found" });
    }

    const labelName = labelToDelete.name.toLowerCase();

    // *** שלב 2: מחק את התווית מ-Labels collection ***
    const deleted = await Label.deleteLabelForUser(userId, labelId);

    if (!deleted) {
      return res.status(404).json({ error: "Label not found" });
    }

    // *** שלב 3: הסר את התווית מכל המיילים ***
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

    // *** שלב 4: נקה גם את MailLabel collection (תאימות לאחור) ***
    await MailLabel.deleteMany({ userId, labelId });

    res.status(204).end();
  } catch (err) {
    console.error("Error deleting label:", err);
    res.status(500).json({ error: "Internal server error" });
  }
}
// Search labels by substring in name
async function search(req, res) {
  try {
    const userId = req.user.email;
    const query = req.params.query?.toLowerCase() || "";

    const labels = await Label.searchLabelsForUser(userId, query);

    const formattedLabels = labels.map(label => ({
      id: label.labelId,
      name: label.name
    }));

    res.json(formattedLabels);
  } catch (err) {
    console.error("Error searching labels:", err);
    res.status(500).json({ error: "Internal server error" });
  }
}

// Add a label to a mail
async function tagMail(req, res) {
  try {
    const userId = req.user.email;
    const { mailId, labelId } = req.body;

    console.log(">> Entered tagMail! mailId =", mailId, "labelId =", labelId);

    if (!mailId || !labelId) {
      return res.status(400).json({ error: "mailId and labelId are required" });
    }

    // Verify label exists and belongs to user
    const label = await Label.getLabelById(userId, labelId);
    if (!label) {
      return res.status(404).json({ error: "Label not found" });
    }

    // Verify mail exists and user has access to it
    const mail = await Mail.findOne({ mailId });
    if (!mail) {
      return res.status(404).json({ error: "Mail not found" });
    }

    if (!mail.isAccessibleBy(userId)) {
      return res.status(403).json({ error: "Not authorized for this mail" });
    }

    // Add label to mail in MailLabel collection
    await MailLabel.findOneAndUpdate(
      { userId, mailId, labelId },
      { userId, mailId, labelId },
      { upsert: true, new: true }
    );

    // Also update Mail document labels array for this user
    await Mail.findOneAndUpdate(
      { mailId, 'labels.userEmail': userId },
      { $addToSet: { 'labels.$.labelIds': labelId } }
    );

    // If no labels entry exists for this user, create one
    const mailDoc = await Mail.findOne({ mailId, 'labels.userEmail': userId });
    if (!mailDoc) {
      await Mail.findOneAndUpdate(
        { mailId },
        { $push: { labels: { userEmail: userId, labelIds: [labelId] } } }
      );
    }

    // If user tagged this mail with "Spam", add all its URLs to the blacklist
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

    res.status(200).json({ success: true });
  } catch (err) {
    console.error("Error tagging mail:", err);
    res.status(500).json({ error: "Internal server error" });
  }
}

// Remove a label from a mail
async function untagMail(req, res) {
  try {
    const userId = req.user.email;
    const { mailId, labelId } = req.body;

    if (!mailId || !labelId) {
      return res.status(400).json({ error: "mailId and labelId are required" });
    }

    // Remove from MailLabel collection
    const removed = await MailLabel.findOneAndDelete({ userId, mailId, labelId });

    if (!removed) {
      return res.status(404).json({ error: "Label not found for mail" });
    }

    // Also remove from Mail document labels array
    await Mail.findOneAndUpdate(
      { mailId, 'labels.userEmail': userId },
      { $pull: { 'labels.$.labelIds': labelId } }
    );

    res.status(200).json({ success: true });
  } catch (err) {
    console.error("Error untagging mail:", err);
    res.status(500).json({ error: "Internal server error" });
  }
}

// Get all mails associated with a label
async function getMailsByLabel(req, res) {
  try {
    const userId = req.user.email;
    const { labelId } = req.params;

    const mailLabels = await MailLabel.getMailsByLabel(userId, labelId);
    const mailIds = mailLabels.map(ml => ml.mailId);

    res.json(mailIds);
  } catch (err) {
    console.error("Error fetching mails by label:", err);
    res.status(500).json({ error: "Internal server error" });
  }
}

// Get all labels for a specific mail
async function getLabelsForMail(req, res) {
  try {
    const userId = req.user.email;
    const { mailId } = req.params;

    const mailLabels = await MailLabel.getLabelsForMail(userId, mailId);
    const labelIds = mailLabels.map(ml => ml.labelId);

    res.json(labelIds);
  } catch (err) {
    console.error("Error fetching labels for mail:", err);
    res.status(500).json({ error: "Internal server error" });
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
  getLabelsForMail
};