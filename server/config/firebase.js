const admin = require("firebase-admin");
const serviceAccount = require("../serviceAccountKey.json"); // Path to the downloaded service account key JSON file

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://vku-app-cecd3-default-rtdb.firebaseio.com"
  });

  
const db = admin.firestore();

module.exports = { admin, db };