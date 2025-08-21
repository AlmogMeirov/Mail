// NOTE: comments in English only

const fs = require("fs").promises;
const jwt = require("jsonwebtoken");
const bcrypt = require("bcrypt");
const User = require("../models/User"); // Mongoose model (see models/User.js)
const Labels = require("../models/labels"); // optional; keep if you already have one

// inboxMap is in-memory; remove it for production. Keeping a guarded call:
let inboxMap;
try {
  inboxMap = require("../utils/inboxMap");
} catch {
  inboxMap = null;
}

const SECRET = process.env.JWT_SECRET || "dev_only_replace";

// --- helpers ---
const dataUrlRegex = /^data:(image\/(png|jpeg|jpg|webp));base64,/i;
function parseAvatarFromDataUrlOrBase64(avatarStr) {
  if (!avatarStr) return null;
  let contentType = "image/png";
  let b64 = avatarStr;

  if (dataUrlRegex.test(avatarStr)) {
    const parts = avatarStr.split(";base64,");
    if (parts.length !== 2) throw new Error("Invalid data URL");
    contentType = parts[0].split(":")[1];
    b64 = parts[1];
  }
  const buf = Buffer.from(b64, "base64");
  if (!buf.length) throw new Error("Invalid base64");
  if (buf.length > 2 * 1024 * 1024) throw new Error("Avatar too large (max 2MB)");

  // very light magic bytes check
  const isPng = buf.slice(0, 8).toString("hex") === "89504e470d0a1a0a";
  const isJpeg = buf.slice(0, 3).toString("hex") === "ffd8ff";
  const isRiff = buf.slice(0, 4).toString("ascii").toUpperCase() === "RIFF"; // webp
  if (!isPng && !isJpeg && !isRiff) throw new Error("Invalid image data");

  return { data: buf, contentType };
}

async function parseAvatarFromUploadedFile(file) {
  if (!file) return null;
  const bytes = await fs.readFile(file.path);
  if (bytes.length > 2 * 1024 * 1024) throw new Error("Avatar too large (max 2MB)");
  const ct = file.mimetype || "application/octet-stream";

  // light magic bytes check
  const isPng = bytes.slice(0, 8).toString("hex") === "89504e470d0a1a0a";
  const isJpeg = bytes.slice(0, 3).toString("hex") === "ffd8ff";
  const isRiff = bytes.slice(0, 4).toString("ascii").toUpperCase() === "RIFF";
  if (!isPng && !isJpeg && !isRiff) throw new Error("Invalid image data");

  return { data: bytes, contentType: ct };
}

// --- Controller: POST /register ---
async function register(req, res) {
  try {
    // pull fields from body (multer text fields also come on req.body)
    const {
      firstName,
      lastName,
      password,
      email,
      birthDate,
      phone,
      gender,
    } = req.body;

    // image source priority:
    // 1) multer file (req.file)
    // 2) JSON field "profilePicture" (data URL or base64)
    const profilePicture = req.body.profilePicture || null;

    // minimal required validation (keep it simple here; you can plug Zod if you want)
    if (!firstName || !lastName || !password || !email) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    // unique email
    const exists = await User.findOne({ email: (email || "").toLowerCase().trim() }).lean();
    if (exists) {
      return res.status(400).json({ error: "Email already registered" });
    }

    // password hash
    const passwordHash = await bcrypt.hash(password, 12);

    // avatar parse (optional)
    let avatarDoc = null;
    try {
      if (req.file) {
        avatarDoc = await parseAvatarFromUploadedFile(req.file);
      } else if (profilePicture) {
        avatarDoc = parseAvatarFromDataUrlOrBase64(profilePicture);
      }
    } catch (e) {
      return res.status(400).json({ error: e.message || "Invalid avatar" });
    } finally {
      // cleanup multer tmp file if present
      if (req.file) {
        try { await fs.unlink(req.file.path); } catch { }
      }
    }

    // persist user in Mongo
    const user = await User.create({
      firstName: String(firstName).trim(),
      lastName: String(lastName).trim(),
      email: String(email).toLowerCase().trim(),
      passwordHash,
      gender: String(gender || "").toLowerCase().trim() || "other",
      birthDate: birthDate ? new Date(birthDate) : null,
      phone: String(phone || "").trim(),
      avatar: avatarDoc || undefined,
    });

    // FIXED: Add user to inboxMap (temporary until we migrate fully to MongoDB)
    if (inboxMap) {
      try {
        inboxMap.set(user.email, []);
        console.log(`Added ${user.email} to inboxMap`);
      } catch (err) {
        console.log("Failed to add user to inboxMap:", err.message);
      }
    }

    // Create default labels
    if (Labels?.createLabel) {
      try { await Labels.createLabel(user.email, "Spam"); } catch { }
    }

    // respond
    return res.status(201).json({
      id: user._id.toString(),
      email: user.email,
      name: `${user.firstName} ${user.lastName}`,
      profileImage: avatarDoc ? { contentType: avatarDoc.contentType, size: avatarDoc.data.length } : null,
    });
  } catch (err) {
    console.error("POST /register error:", err);
    return res.status(500).json({ error: "Server error" });
  }
}

// --- Controller: POST /login ---
async function login(req, res) {
  try {
    const email = (req.body.email || "").toLowerCase().trim();
    const password = req.body.password || "";

    if (!email || !password) {
      return res.status(400).json({ error: "Missing email or password" });
    }

    const user = await User.findOne({ email });
    if (!user) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    // Add user to inboxMap if not exists (for existing users)
    if (inboxMap && !inboxMap.has(user.email)) {
      try {
        inboxMap.set(user.email, []);
        console.log(`Added existing user ${user.email} to inboxMap`);
      } catch (err) {
        console.log("Failed to add existing user to inboxMap:", err.message);
      }
    }

    // sign with sub = user._id for consistency with auth middleware
    const token = jwt.sign(
      { sub: user._id.toString(), email: user.email },
      SECRET,
      { expiresIn: "1h" }
    );

    return res.status(200).json({ token, expiresIn: 3600 });
  } catch (err) {
    console.error("POST /login error:", err);
    return res.status(500).json({ error: "Server error" });
  }
}

// --- Controller: GET /me (requires authenticateToken that loads req.user/req.auth) ---
async function getCurrentUser(req, res) {
  try {
    // if you used my auth middleware, req.user is the Mongo doc already:
    if (req.user && req.user._id) {
      // sanitize
      const u = { ...req.user };
      delete u.passwordHash;
      return res.status(200).json(u);
    }

    // fallback: if middleware only set payload (e.g., req.user.userId)
    const id = req.user?.sub || req.user?.userId;
    if (!id) return res.status(401).json({ error: "Unauthorized" });

    const user = await User.findById(id).select("-passwordHash").lean();
    if (!user) return res.status(404).json({ error: "User not found" });

    return res.status(200).json(user);
  } catch (err) {
    console.error("GET /me error:", err);
    return res.status(500).json({ error: "Server error" });
  }
}

module.exports = { register, login, getCurrentUser };