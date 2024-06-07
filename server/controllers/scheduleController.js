const { db } = require('../config/firebase');

const addSchedule = async (req, res) => {
    const { scheduleId, userId, dayOfWeek, date, time, room, subject } = req.body;
    if (!userId || !dayOfWeek || !time || !room || !subject || !date) {
        return res.status(400).json({ message: 'Missing required fields' });
    }

    try {
        const scheduleData = { scheduleId, userId, dayOfWeek, date, time, room, subject };
        const userRef = db.collection('users').doc(userId);
        await userRef.collection('schedules').doc(scheduleId).set(scheduleData);
        return res.status(200).json({ message: 'Schedule added successfully' });
    } catch (err) {
        console.error('Error adding schedule: ', err);
        return res.status(500).json({ message: 'Internal server error' });
    }
};

const deleteSchedule = async (req, res) => {
    const { scheduleId, userId } = req.body;
    if (!scheduleId || !userId) {
        return res.status(400).json({ message: 'Missing required fields' });
    }

    try {
        const scheduleRef = db.collection('users').doc(userId).collection('schedules').doc(scheduleId);
        const doc = await scheduleRef.get();

        if (!doc.exists) {
            return res.status(404).json({ message: 'Schedule not found' });
        }

        await scheduleRef.delete();
        return res.status(200).json({ message: 'Schedule deleted successfully' });
    } catch (err) {
        console.error('Error deleting schedule: ', err);
        return res.status(500).json({ message: 'Internal server error' });
    }
};

const updateSchedule = async (req, res) => {
    const { scheduleId, userId, dayOfWeek, date, time, room, subject } = req.body;
    if (!userId || !dayOfWeek || !time || !room || !subject || !date) {
        return res.status(400).json({ message: 'Missing required fields' });
    }

    try {
        const scheduleRef = db.collection('users').doc(userId).collection('schedules').doc(scheduleId);
        const doc = await scheduleRef.get();

        if (!doc.exists) {
            return res.status(404).json({ message: 'Schedule not found' });
        }

        const updatedSchedule = { dayOfWeek, date, time, room, subject };
        await scheduleRef.update(updatedSchedule);
        return res.status(200).json({ message: 'Schedule updated successfully' });
    } catch (err) {
        console.error('Error updating schedule: ', err);
        return res.status(500).json({ message: 'Internal server error' });
    }
};

const getAllSchedules = async (req, res) => {
    const userId = req.params.userId;
    try {
        const schedulesRef = db.collection('users').doc(userId).collection('schedules');
        const snapshot = await schedulesRef.get();

        let schedules = [];
        snapshot.forEach(doc => {
            schedules.push(doc.data());
        });

        return res.status(200).json(schedules);
    } catch (err) {
        console.error('Error getting schedules: ', err);
        return res.status(500).json({ message: 'Internal server error' });
    }
};

module.exports = {
    addSchedule,
    deleteSchedule,
    updateSchedule,
    getAllSchedules
};
