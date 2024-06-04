const { OAuth2Client } = require('google-auth-library');
const { db } = require('../config/firebase');
const googleClientId = '580572019975-h4j2s53sv1grsntub9filro93pk8gcao.apps.googleusercontent.com';
const googleClient = new OAuth2Client(googleClientId);

exports.verifyIdToken = async (req, res) => {
  const idToken = req.body.idToken.json();
  console.log(idToken.json())
  if (!idToken) {
    console.error('ID token is undefined');
    return res.status(400).send('ID token is required');
  }

  try {
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: YOUR_GOOGLE_CLIENT_ID,
    });

    const payload = ticket.getPayload();
    const userId = payload['sub'];

    // Lưu user vào Firestore
    const userRef = admin.firestore().collection('users').doc(userId);
    await userRef.set(payload, { merge: true });

    res.status(200).json(payload);
  } catch (error) {
    console.error('Error verifying ID token:', error);
    res.status(401).send('Invalid token');
  }
}
