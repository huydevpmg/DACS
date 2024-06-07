const express = require('express');
const { addSchedule, deleteSchedule, updateSchedule, getAllSchedules } = require('../controllers/scheduleController');
const router = express.Router();

router.post('/add-schedule', addSchedule);
router.post('/delete-schedule', deleteSchedule);
router.post('/update-schedule', updateSchedule);
router.get('/get-all-schedules/:userId', getAllSchedules);

module.exports = router;
