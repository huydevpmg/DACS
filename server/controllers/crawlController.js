const axios = require('axios');
const cheerio = require('cheerio');
const { db } = require('../config/firebase');

const crawlAndSaveData = async (url, docId) => {
    try {
        const response = await axios.get(url);
        const $ = cheerio.load(response.data);
        let items = [];

        $("body > div.container.body-content > div > div > div.col-md-9 > div > div:nth-child(1) > div > div > div.item-list > ul > li").each((i, element) => {
            const linkTitle = $(element).find("a").text().trim();
            const linkHref = $(element).find("a").attr("href");
            let spanText = $(element).find("span").text().trim();
            const dateMatch = spanText.match(/\d{2}-\d{2}-\d{4}/);
            if (dateMatch) {
                spanText = dateMatch[0];
            } else {
                spanText = "";
            }
            items.push({
                title: linkTitle,
                href: linkHref,
                spanText: spanText,
            });
        });

        const docRef = db.collection('Notification').doc(docId);
        await docRef.set({ items });
        return items;
    } catch (error) {
        console.error("Error crawling data:", error);
        throw error;
    }
};

exports.crawlAndSaveDataDaoTao = async (req, res) => {
    try {
        const data = await crawlAndSaveData("https://daotao.vku.udn.vn/vku-thong-bao-chung", "Daotao");
        return res.status(200).json(data);
    } catch (error) {
        return res.status(500).send({ status: "Failed", msg: "Failed to crawl and save data" });
    }
};

exports.crawlAndSaveCTSV = async (req, res) => {
    try {
        const data = await crawlAndSaveData("https://daotao.vku.udn.vn/vku-thong-bao-ctsv", "CTSV");
        return res.status(200).json(data);
    } catch (error) {
        return res.status(500).send({ status: "Failed", msg: "Failed to crawl and save data" });
    }
};

exports.crawlAndSaveKHTC = async (req, res) => {
    try {
        const data = await crawlAndSaveData("https://daotao.vku.udn.vn/vku-thong-bao-khtc", "KHTC");
        return res.status(200).json(data);
    } catch (error) {
        return res.status(500).send({ status: "Failed", msg: "Failed to crawl and save data" });
    }
};

exports.crawlAndSaveKTDBCL = async (req, res) => {
    try {
        const data = await crawlAndSaveData("https://daotao.vku.udn.vn/vku-thong-bao-ktdbcl", "KTDBCL");
        return res.status(200).json(data);
    } catch (error) {
        return res.status(500).send({ status: "Failed", msg: "Failed to crawl and save data" });
    }
};
