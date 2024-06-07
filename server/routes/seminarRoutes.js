const express = require('express');
const { addSeminar, deleteSeminar, updateSeminar, getAllSeminars } = require('../controllers/seminarController');
const router = express.Router();

router.post('/add-seminar', addSeminar);
router.post('/delete-seminar', deleteSeminar);
router.post('/update-seminar', updateSeminar);
router.get('/get-all-seminars/:userId', getAllSeminars);

module.exports = router;
