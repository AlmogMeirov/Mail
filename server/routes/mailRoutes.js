const express = require('express');
const router = express.Router();
const { createMail } = require('../controllers/mailController');

router.post('/', createMail);

module.exports = router;
