const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const passport = require('passport');
const firebaseService = require('./config/firebase');
const { db } = require('./config/firebase');

const crawlRoutes = require('./routes/crawlRoutes');
const scheduleRoutes = require('./routes/scheduleRoutes');
const seminarRoutes = require('./routes/seminarRoutes');
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

app.use('/', scheduleRoutes);
app.use('/', seminarRoutes);
app.use('/', crawlRoutes);

// Start server
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
