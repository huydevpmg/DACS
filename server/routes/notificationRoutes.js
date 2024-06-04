const express = require('express');
const router = express.Router();
const { searchForNotification } = require('../controllers/notificationController');

router.get('/search', searchForNotification);

module.exports = router;
