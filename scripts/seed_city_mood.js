/**
 * Seed this week's city mood card for immediate feed testing.
 *
 * Usage:
 *   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
 *   node scripts/seed_city_mood.js
 *   node scripts/seed_city_mood.js --city=bangalore
 */

"use strict";

const admin = require("firebase-admin");

const city = (process.argv.find((a) => a.startsWith("--city="))?.split("=")[1] || "bangalore").toLowerCase();

admin.initializeApp();
const db = admin.firestore();

function weekId() {
  const now = new Date();
  const onejan = new Date(now.getFullYear(), 0, 1);
  const week = Math.ceil(((now - onejan) / 86400000 + onejan.getDay() + 1) / 7);
  return `${now.getFullYear()}-W${week}`;
}

const MOOD_BY_CITY = {
  bangalore: ["😤 Traffic", "😩 Rent", "😐 Layoffs", "😊 Metro"],
  pune: ["😤 Commute", "😩 Rent", "🌧️ Monsoon", "😊 Food"],
  hyderabad: ["😤 ORR Traffic", "😩 Rent", "💼 Office", "😊 Biryani"],
  chennai: ["😤 Heat", "😩 Rent", "🌊 Beach", "😊 Filter coffee"],
  mumbai: ["😤 Local train", "😩 Rent", "💼 Work", "😊 Street food"],
  delhi: ["😤 Metro", "😩 Rent", "🌫️ Pollution", "😊 Winter"],
};

async function seed() {
  const lines = MOOD_BY_CITY[city] || MOOD_BY_CITY.bangalore;
  const id = weekId();
  const mood = {
    city,
    weekId: id,
    lines,
    topCategory: "traffic",
    createdAt: Date.now(),
  };

  await db.doc(`city_mood/${city}/weekly/${id}`).set(mood);

  const today = new Date().toISOString().slice(0, 10);
  const prompt = "What is one thing nobody admits about your city?";
  await db.doc(`prompts/${city}/daily/${today}`).set({
    text: prompt,
    city,
    date: today,
    createdAt: Date.now(),
  });

  console.log(`City mood written: city_mood/${city}/weekly/${id}`);
  console.log(`Daily prompt written: prompts/${city}/daily/${today}`);
  console.log("Lines:", lines.join(" · "));
  process.exit(0);
}

seed().catch((e) => {
  console.error(e);
  process.exit(1);
});
