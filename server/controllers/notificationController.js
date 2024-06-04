const { db } = require('../config/firebase');

exports.searchForNotification = async (req, res) => {
    const searchQuery = req.query.title;

    if (!searchQuery) {
        return res.status(400).send({ status: "Failed", msg: "Missing search query" });
    }

    try {
        const collections = ['Daotao', 'CTSV', 'KHTC', 'KTDBCL'];
        let results = [];

        for (let collection of collections) {
            const snapshot = await db.collection('Notification').doc(collection).get();
            if (snapshot.exists) {
                const data = snapshot.data();
                const filteredItems = data.items.filter(item => 
                    item.title.toLowerCase().includes(searchQuery.toLowerCase())
                );
                results = results.concat(filteredItems);
            }
        }

        return res.status(200).json(results);
    } catch (error) {
        console.error("Error searching for notifications:", error);
        return res.status(500).send({ status: "Failed", msg: "Failed to search for notifications" });
    }
};
