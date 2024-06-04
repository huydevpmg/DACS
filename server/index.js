const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const passport = require('passport');
const crawlRoutes = require('./routes/crawlRoutes');
const firebaseService = require('./config/firebase');
const { OAuth2Client } = require('google-auth-library');
const googleClientId = '580572019975-h4j2s53sv1grsntub9filro93pk8gcao.apps.googleusercontent.com';
const googleClient = new OAuth2Client(googleClientId);
const { db } = require('./config/firebase');


const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());  // Middleware để phân tích cú pháp JSON
app.use(bodyParser.json());
app.use(passport.initialize());

// Initialize Firebase
firebaseService.initialize

app.post('/verify-user', async (req, res) => {
    const { username ,email, userId, profilePictureUrl } = req.body;
    if (!email || !userId) {
      return res.status(400).send('Missing user data');
    }
    console.log( req.body)
    try {
      const userRef = db.collection('users').doc(userId);
      const doc = await userRef.get();
      if (doc.exists) {
        return res.status(200).json(doc.data());
      } else {
        const userData = { username,email, userId, profilePictureUrl };
        console.log(userData)
        await userRef.set(userData);
        return res.status(201).json(userData);
      }
    } catch (error) {
      console.error('Error verifying user:', error);
      return res.status(500).send('Internal Server Error');
    }
  });


  app.post('/add-schedule', async (req, res) => {
      const { scheduleId, userId, dayOfWeek, time, room, subject } = req.body;
      console.log(req.body)
      if (!userId || !dayOfWeek || !time || !room || !subject) {
          return res.status(400).json({ message: 'Missing required fields' });
      }
  
      try {
          const scheduleData = { scheduleId, dayOfWeek, time, room, subject };
  
          const userRef = db.collection('users').doc(userId);
          await userRef.collection('schedules').doc(scheduleId).set(scheduleData);
  
          console.log('Schedule added successfully');
          return res.status(200).json({ message: 'Schedule added successfully' });
      } catch (err) {
          console.error('Error adding schedule: ', err);
          return res.status(500).json({ message: 'Internal server error' });
      }
  });

// Delete schedule
app.post('/delete-schedule', async (req, res) => {
  const { scheduleId, userId } = req.body;
  console.log(req.body)
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
    console.log('Schedule deleted successfully');
    return res.status(200).json({ message: 'Schedule deleted successfully' });
  } catch (err) {
    console.error('Error deleting schedule: ', err);
    return res.status(500).json({ message: 'Internal server error' });
  }
});

app.post('/update-schedule', async (req, res) => {
  const { scheduleId, userId, dayOfWeek, time, room, subject } = req.body;

  if (!scheduleId || !userId || !dayOfWeek || !time || !room || !subject) {
    return res.status(400).json({ message: 'Missing required fields' });
  }

  try {
    const scheduleRef = db.collection('users').doc(userId).collection('schedules').doc(scheduleId);
    const doc = await scheduleRef.get();

    if (!doc.exists) {
      return res.status(404).json({ message: 'Schedule not found' });
    }

    const updatedSchedule = {
      dayOfWeek,
      time,
      room,
      subject
    };

    await scheduleRef.update(updatedSchedule);
    console.log('Schedule updated successfully');
    return res.status(200).json({ message: 'Schedule updated successfully' });
  } catch (err) {
    console.error('Error updating schedule: ', err);
    return res.status(500).json({ message: 'Internal server error' });
  }
});

app.get('/get-all-schedules/:userId', async (req, res) => {
  const userId = req.params.userId;

  try {
      const schedulesRef = db.collection('users').doc(userId).collection('schedules');
      const snapshot = await schedulesRef.get();

      let schedules = [];
      snapshot.forEach(doc => {
          const scheduleData = doc.data();
          schedules.push(scheduleData); // Each schedule already includes scheduleId
      });

      console.log('Schedules retrieved successfully');
      return res.status(200).json(schedules);
  } catch (err) {
      console.error('Error getting schedules: ', err);
      return res.status(500).json({ message: 'Internal server error' });
  }
});

app.use('/', crawlRoutes);

// Start server
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
