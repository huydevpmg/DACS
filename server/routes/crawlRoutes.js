const express = require('express');
const router = express.Router();
const crawlController = require('../controllers/crawlController');
const { searchForNotification } = require('../controllers/notificationController');

router.get('/dao-tao', crawlController.crawlAndSaveDataDaoTao);
router.get('/ctsv', crawlController.crawlAndSaveCTSV);
router.get('/khtc', crawlController.crawlAndSaveKHTC);
router.get('/ktdbcl', crawlController.crawlAndSaveKTDBCL);
router.get('/search', searchForNotification);

module.exports = router;
