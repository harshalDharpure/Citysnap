/**
 * Seed a few sponsored thoughts for Phase 10 testing.
 *
 * Usage:
 *   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
 *   node scripts/seed_sponsored.js
 */

"use strict";

const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

const SPONSORED = [
  {
    text: "New specialty coffee spot opening in Indiranagar — quiet corners for WFH.",
    city: "bangalore",
    locality: "indiranagar",
    category: "food",
    sponsorLabel: "Local café",
  },
  {
    text: "Flexible desks available in Whitefield. No broker drama.",
    city: "bangalore",
    locality: "whitefield",
    category: "startup",
    sponsorLabel: "Coworking space",
  },
  {
    text: "Weekend tech meetup in Koramangala — free entry for builders.",
    city: "bangalore",
    locality: "koramangala",
    category: "startup",
    sponsorLabel: "Local events",
  },
];

async function seed() {
  for (const s of SPONSORED) {
    await db.collection("thoughts").add({
      ...s,
      feelCount: Math.floor(Math.random() * 40) + 10,
      commentCount: Math.floor(Math.random() * 8),
      shareCount: Math.floor(Math.random() * 5),
      authorId: "sponsor_system",
      authorName: "",
      anonymous: true,
      identityLabel: "startup_employee",
      isSponsored: true,
      trendingRank: 0,
      createdAt: Date.now() - Math.floor(Math.random() * 86400000 * 3),
    });
    console.log(`Sponsored: ${s.sponsorLabel}`);
  }
  console.log("Done.");
  process.exit(0);
}

seed().catch((e) => {
  console.error(e);
  process.exit(1);
});
