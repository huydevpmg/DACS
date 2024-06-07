const { db } = require('../config/firebase');

const addSeminar = async (req, res) => {
    const { seminarId, userId, dayOfWeek, date, time, room, subject } = req.body;
    if (!userId || !dayOfWeek || !time || !room || !subject || !date) {
        return res.status(400).json({ message: 'Missing required fields' });
    }

    try {
        const seminarData = { seminarId, userId, dayOfWeek, date, time, room, subject };
        const userRef = db.collection('users').doc(userId);
        await userRef.collection('seminars').doc(seminarId).set(seminarData);
        return res.status(200).json({ message: 'Seminar added successfully' });
    } catch (err) {
        console.error('Error adding seminar: ', err);
        return res.status(500).json({ message: 'Internal server error' });
    }
};

const deleteSeminar = async (req, res) => {
    const { seminarId, userId } = req.body;
    if (!seminarId || !userId) {
        return res.status(400).json({ message: 'Missing required fields' });
    }

    try {
        const seminarRef = db.collection('users').doc(userId).collection('seminars').doc(seminarId);
        const doc = await seminarRef.get();

        if (!doc.exists) {
            return res.status(404).json({ message: 'Seminar not found' });
        }

        await seminarRef.delete();
        return res.status(200).json({ message: 'Seminar deleted successfully' });
    } catch (err) {
        console.error('Error deleting seminar: ', err);
        return res.status(500).json({ message: 'Internal server error' });
    }
};

const updateSeminar = async (req, res) => {
    const { seminarId, userId, dayOfWeek, date, time, room, subject } = req.body;
    if (!userId || !dayOfWeek || !time || !room || !subject || !date) {
        return res.status(400).json({ message: 'Missing required fields' });
    }

    try {
        const seminarRef = db.collection('users').doc(userId).collection('seminars').doc(seminarId);
        const doc = await seminarRef.get();

        if (!doc.exists) {
            return res.status(404).json({ message: 'Seminar not found' });
        }

        const updatedSeminar = { dayOfWeek, date, time, room, subject };
        await seminarRef.update(updatedSeminar);
        return res.status(200).json({ message: 'Seminar updated successfully' });
    } catch (err) {
        console.error('Error updating seminar: ', err);
        return res.status(500).json({ message: 'Internal server error' });
    }
};

const getAllSeminars = async (req, res) => {
    const userId = req.params.userId;
    try {
        const seminarsRef = db.collection('users').doc(userId).collection('seminars');
        const snapshot = await seminarsRef.get();

        let seminars = [];
        snapshot.forEach(doc => {
            seminars.push(doc.data());
        });

        return res.status(200).json(seminars);
    } catch (err) {
        console.error('Error getting seminars: ', err);
        return res.status(500).json({ message: 'Internal server error' });
    }
};

module.exports = {
    addSeminar,
    deleteSeminar,
    updateSeminar,
    getAllSeminars
};
