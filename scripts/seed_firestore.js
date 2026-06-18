/**
 * Seed hoght Firestore with realistic Bangalore thoughts and comments.
 *
 * Prerequisites:
 *   npm install firebase-admin
 *   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
 *
 * Usage:
 *   node scripts/seed_firestore.js
 *   node scripts/seed_firestore.js --thoughts=500 --comments-per-thought=4
 */

"use strict";

const admin = require("firebase-admin");

const THOUGHT_COUNT = parseInt(process.argv.find((a) => a.startsWith("--thoughts="))?.split("=")[1] || "500", 10);
const COMMENTS_PER = parseInt(process.argv.find((a) => a.startsWith("--comments-per-thought="))?.split("=")[1] || "4", 10);

admin.initializeApp();
const db = admin.firestore();

const LOCALITIES = ["whitefield", "hsr", "koramangala", "electronic_city", "indiranagar"];
const CATEGORIES = ["work", "rent", "traffic", "college", "relationship", "startup", "life", "food"];
const IDENTITIES = [
  "bangalore_engineer",
  "whitefield_resident",
  "startup_employee",
  "student",
  "office_insider",
  "traffic_philosopher",
  "rent_survivor",
];

const THOUGHT_TEMPLATES = {
  work: [
    "Managers say we're family. Families don't schedule meetings at 9 PM.",
    "Salary increased 10%. Rent increased 40%.",
    "Hybrid means you commute two hours for two days in office.",
    "My manager asked for a status update on my status update.",
    "Layoff rumors are louder than the coffee machine.",
  ],
  rent: [
    "Rent is increasing faster than salaries.",
    "Broker fee is a month's salary now.",
    "PG owners treat deposits like donations.",
    "Flat hunting feels like a full-time job.",
  ],
  traffic: [
    "Bangalore traffic feels worse every week.",
    "Silk Board is a personality test.",
    "Two hours commute for a hybrid job.",
    "ORR at 6 PM should count as extreme sport.",
  ],
  college: [
    "Placements feel like a lottery with better marketing.",
    "Internship stipend doesn't cover auto fare.",
    "Everyone says enjoy college. Nobody says enjoy traffic to college.",
  ],
  relationship: [
    "Dating in Bangalore is scheduling around traffic.",
    "Marriage pressure arrived before my first salary hike.",
    "Long distance in the same city because of rent.",
  ],
  startup: [
    "We're pre-revenue but post-pitch-deck.",
    "Equity is the new 'we'll talk at appraisal'.",
    "Startup culture is free snacks and unpaid overtime.",
  ],
  life: [
    "Bangalore weather is perfect until you step outside.",
    "Everyone pretends to love filter coffee.",
    "Weekend plans die in Silk Board traffic.",
  ],
  food: [
    "Dosa at 11 PM hits different after a bad day.",
    "Every area claims the best biryani. All are right.",
  ],
};

const COMMENT_TEMPLATES = [
  "This is too real.",
  "Felt this in my soul.",
  "Why is this accurate?",
  "Same here.",
  "Needed to hear this.",
  "Bangalore things.",
  "Couldn't have said it better.",
  "This should be on a billboard.",
];

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomPastMs(daysBack) {
  const now = Date.now();
  const span = daysBack * 24 * 60 * 60 * 1000;
  return now - Math.floor(Math.random() * span);
}

async function seed() {
  console.log(`Seeding ${THOUGHT_COUNT} thoughts with ~${COMMENTS_PER} comments each...`);

  const batchSize = 400;
  let batch = db.batch();
  let ops = 0;
  let thoughtIds = [];

  for (let i = 0; i < THOUGHT_COUNT; i++) {
    const category = pick(CATEGORIES);
    const templates = THOUGHT_TEMPLATES[category] || THOUGHT_TEMPLATES.life;
    const text = pick(templates);
    const ref = db.collection("thoughts").doc();
    const feelCount = Math.floor(Math.random() * 200);
    const commentCount = COMMENTS_PER;
    const shareCount = Math.floor(Math.random() * 30);

    batch.set(ref, {
      id: ref.id,
      text,
      feelCount,
      commentCount,
      shareCount,
      authorId: `seed_user_${i % 50}`,
      authorName: "",
      anonymous: true,
      city: "bangalore",
      locality: pick(LOCALITIES),
      category,
      identityLabel: pick(IDENTITIES),
      trendingRank: 0,
      createdAt: randomPastMs(30),
    });
    thoughtIds.push(ref.id);
    ops++;

    if (ops >= batchSize) {
      await batch.commit();
      batch = db.batch();
      ops = 0;
      console.log(`  ${i + 1} thoughts written...`);
    }
  }
  if (ops > 0) await batch.commit();

  console.log("Seeding comments...");
  let commentOps = 0;
  batch = db.batch();

  for (const thoughtId of thoughtIds) {
    for (let c = 0; c < COMMENTS_PER; c++) {
      const cref = db.collection("thoughts").doc(thoughtId).collection("comments").doc();
      batch.set(cref, {
        id: cref.id,
        userId: `seed_commenter_${c % 20}`,
        userName: "Anonymous",
        text: pick(COMMENT_TEMPLATES),
        createdAt: randomPastMs(25),
      });
      commentOps++;
      if (commentOps >= batchSize) {
        await batch.commit();
        batch = db.batch();
        commentOps = 0;
      }
    }
  }
  if (commentOps > 0) await batch.commit();

  console.log("Done.");
  process.exit(0);
}

seed().catch((err) => {
  console.error(err);
  process.exit(1);
});
