const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');

router.post('/verify-id-token', authController.verifyIdToken);

module.exports = router;
