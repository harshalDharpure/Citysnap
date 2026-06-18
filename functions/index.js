"use strict";

/**
 * hoght Cloud Functions — notifications, prompts, trending, badges, city mood.
 *
 * Deploy: firebase deploy --only functions,firestore:rules,firestore:indexes
 */

const { onDocumentCreated, onDocumentDeleted } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

const FEEL_MILESTONES = [10, 50, 100, 500, 1000];
const CITIES = ["bangalore", "pune", "hyderabad", "chennai", "mumbai", "delhi"];

const CATEGORY_EMOJI = {
  traffic: "😤",
  rent: "😩",
  work: "😐",
  relationship: "💬",
  college: "🎓",
  startup: "🚀",
  food: "🍜",
  life: "💭",
};

const CATEGORY_LABEL = {
  traffic: "Traffic",
  rent: "Rent",
  work: "Layoffs",
  relationship: "Relationships",
  college: "College",
  startup: "Startups",
  food: "Food",
  life: "Life",
};

function cityDisplay(city) {
  return city.charAt(0).toUpperCase() + city.slice(1);
}

function weekId() {
  const now = new Date();
  const onejan = new Date(now.getFullYear(), 0, 1);
  const week = Math.ceil(((now - onejan) / 86400000 + onejan.getDay() + 1) / 7);
  return `${now.getFullYear()}-W${week}`;
}

async function notifyUser(uid, title, body, data = {}) {
  if (!uid) return;
  const userSnap = await db.doc(`users/${uid}`).get();
  if (!userSnap.exists) return;

  const type = data.type || "";
  if (type) {
    const feelsOn = userSnap.get("notifyFeels") !== false;
    const commentsOn = userSnap.get("notifyComments") !== false;
    const promptsOn = userSnap.get("notifyPrompts") !== false;
    const allowed = {
      feel: feelsOn,
      milestone: feelsOn,
      trending: feelsOn,
      comment: commentsOn,
      daily_prompt: promptsOn,
      locality_topic: promptsOn,
    }[type];
    if (allowed === false) return;
  }

  const token = userSnap.get("fcmToken");
  if (!token) return;

  try {
    await messaging.send({
      token,
      notification: { title, body },
      data: {
        type: data.type || "",
        thoughtId: data.thoughtId || "",
        promptText: data.promptText || "",
      },
      android: { priority: "high" },
    });

    if (type) {
      await db.collection(`users/${uid}/notifications`).add({
        type,
        title,
        body,
        thoughtId: data.thoughtId || "",
        read: false,
        createdAt: Date.now(),
      });
    }
  } catch (err) {
    logger.warn(`Failed to send to ${uid}: ${err.message}`);
    if (
      err.code === "messaging/registration-token-not-registered" ||
      err.code === "messaging/invalid-registration-token"
    ) {
      await db.doc(`users/${uid}`).update({
        fcmToken: admin.firestore.FieldValue.delete(),
      });
    }
  }
}

async function notifyCityUsers(city, title, body, data = {}) {
  const usersSnap = await db.collection("users").where("city", "==", city).get();
  const batch = [];
  for (const doc of usersSnap.docs) {
    batch.push(notifyUser(doc.id, title, body, data));
    if (batch.length >= 20) {
      await Promise.all(batch);
      batch.length = 0;
    }
  }
  if (batch.length) await Promise.all(batch);
}

async function notifyLocalityUsers(city, locality, title, body, data = {}) {
  const usersSnap = await db
    .collection("users")
    .where("city", "==", city)
    .where("locality", "==", locality)
    .get();
  for (const doc of usersSnap.docs) {
    await notifyUser(doc.id, title, body, data);
  }
}

/** When a new thought is posted, award contributor badges server-side. */
exports.onThoughtCreated = onDocumentCreated("thoughts/{thoughtId}", async (event) => {
  const thought = event.data && event.data.data();
  if (!thought) return;
  const authorId = thought.authorId;
  if (!authorId || authorId.startsWith("seed_") || authorId === "sponsor_system") return;

  const thoughtsSnap = await db.collection("thoughts").where("authorId", "==", authorId).get();
  const count = thoughtsSnap.size;
  if (count >= 50) {
    await db.doc(`users/${authorId}`).update({
      badges: admin.firestore.FieldValue.arrayUnion("top_contributor"),
    });
  }

  const category = thought.category;
  const categoryBadge = {
    work: "office_insider",
    traffic: "traffic_philosopher",
    rent: "rent_survivor",
  }[category];
  if (!categoryBadge) return;

  const catCount = thoughtsSnap.docs.filter((d) => d.get("category") === category).length;
  if (catCount >= 10) {
    await db.doc(`users/${authorId}`).update({
      badges: admin.firestore.FieldValue.arrayUnion(categoryBadge),
    });
  }
});

/** When someone reacts, notify author; milestone at 10/50/100/500/1000. */
exports.onFeel = onDocumentCreated("thoughts/{thoughtId}/feels/{uid}", async (event) => {
  const { thoughtId, uid: feelerUid } = event.params;
  const thoughtSnap = await db.doc(`thoughts/${thoughtId}`).get();
  if (!thoughtSnap.exists) return;

  const authorId = thoughtSnap.get("authorId");
  if (!authorId || authorId === feelerUid) return;

  const feelCount = thoughtSnap.get("feelCount") || 0;
  const cityName = cityDisplay(thoughtSnap.get("city") || "bangalore");

  if (FEEL_MILESTONES.includes(feelCount)) {
    await notifyUser(
      authorId,
      `${feelCount} people feel this`,
      `Your thought is resonating across ${cityName}.`,
      { type: "milestone", thoughtId },
    );
    return;
  }

  await notifyUser(
    authorId,
    `${feelCount} people feel this`,
    "Your thought just resonated with someone.",
    { type: "feel", thoughtId },
  );
});

/** Referral badge at 3 invites. */
exports.onReferral = onDocumentCreated(
  "users/{referrerUid}/referrals/{referredUid}",
  async (event) => {
    const { referrerUid } = event.params;
    const snap = await db.collection(`users/${referrerUid}/referrals`).get();
    const count = snap.size;
    const updates = { referralCount: count };
    if (count >= 3) {
      updates.badges = admin.firestore.FieldValue.arrayUnion("early_bangalore_voice");
    }
    await db.doc(`users/${referrerUid}`).update(updates);
  },
);

/** Comment notification. */
exports.onComment = onDocumentCreated(
  "thoughts/{thoughtId}/comments/{commentId}",
  async (event) => {
    const { thoughtId } = event.params;
    const comment = event.data && event.data.data();
    if (!comment) return;

    const thoughtSnap = await db.doc(`thoughts/${thoughtId}`).get();
    if (!thoughtSnap.exists) return;

    const authorId = thoughtSnap.get("authorId");
    if (!authorId || authorId === comment.userId) return;

    const preview = (comment.text || "").slice(0, 120);
    await notifyUser(
      authorId,
      "Someone replied to your thought",
      preview || "Open hoght to read the reply.",
      { type: "comment", thoughtId },
    );
  },
);

/** Nightly: write trendingRank on top thoughts + notify authors. */
exports.onTrendingRank = onSchedule("0 20 * * *", async () => {
  for (const city of CITIES) {
    const snap = await db
      .collection("thoughts")
      .where("city", "==", city)
      .orderBy("feelCount", "desc")
      .limit(10)
      .get();

    let rank = 1;
    for (const doc of snap.docs) {
      await doc.ref.update({ trendingRank: rank });
      const authorId = doc.get("authorId");
      if (authorId) {
        await notifyUser(
          authorId,
          `You're #${rank} in ${cityDisplay(city)}`,
          "Your thought is trending today.",
          { type: "trending", thoughtId: doc.id },
        );
      }
      rank += 1;
    }
  }
});

/** Daily 9 AM IST: store prompt + push to city users. */
exports.onDailyPrompt = onSchedule("0 3 * * *", async () => {
  const prompts = [
    "What is one thing nobody admits about Bangalore?",
    "What's frustrating you today?",
    "What's something everyone pretends to like?",
    "What happened at work today?",
  ];
  const dayIndex = Math.floor(Date.now() / 86400000) % prompts.length;
  const prompt = prompts[dayIndex];
  const date = new Date().toISOString().slice(0, 10);
  const city = "bangalore";

  await db.doc(`prompts/${city}/daily/${date}`).set({
    text: prompt,
    city,
    date,
    createdAt: Date.now(),
  });

  await notifyCityUsers(
    city,
    "Today's prompt",
    prompt,
    { type: "daily_prompt", promptText: prompt },
  );

  logger.info(`Daily prompt pushed for ${city}: ${prompt}`);
});

/** Weekly Sunday: locality topic notification. */
exports.onWeeklyLocalityTopic = onSchedule("0 5 * * 0", async () => {
  const city = "bangalore";
  const localities = ["whitefield", "hsr", "koramangala", "electronic_city", "indiranagar"];
  const since = Date.now() - 7 * 86400000;

  for (const locality of localities) {
    const snap = await db
      .collection("thoughts")
      .where("city", "==", city)
      .where("locality", "==", locality)
      .where("createdAt", ">=", since)
      .get();

    const counts = {};
    snap.docs.forEach((doc) => {
      const cat = doc.get("category") || "life";
      counts[cat] = (counts[cat] || 0) + 1;
    });

    const topCategory = Object.entries(counts).sort((a, b) => b[1] - a[1])[0];
    if (!topCategory) continue;

    const [category, count] = topCategory;
    if (count < 3) continue;

    const label = CATEGORY_LABEL[category] || category;
    const localityName = locality.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());

    await notifyLocalityUsers(
      city,
      locality,
      `${localityName} is discussing ${label.toLowerCase()} today`,
      `${count} thoughts this week. See what people are saying.`,
      { type: "locality_topic", promptText: `What's your take on ${label.toLowerCase()} in ${localityName}?` },
    );
  }
});

/** Weekly Sunday: city mood card for sharing. */
exports.onWeeklyCityMood = onSchedule("0 6 * * 0", async () => {
  for (const city of CITIES) {
    const since = Date.now() - 7 * 86400000;
    const snap = await db
      .collection("thoughts")
      .where("city", "==", city)
      .where("createdAt", ">=", since)
      .get();

    const counts = {};
    snap.docs.forEach((doc) => {
      const cat = doc.get("category") || "life";
      counts[cat] = (counts[cat] || 0) + 1;
    });

    const sorted = Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 4);
    const lines = sorted.map(([cat]) => {
      const emoji = CATEGORY_EMOJI[cat] || "📊";
      const label = CATEGORY_LABEL[cat] || cat;
      return `${emoji} ${label}`;
    });

    if (lines.length === 0) continue;

    const mood = {
      city,
      weekId: weekId(),
      lines,
      topCategory: sorted[0] ? sorted[0][0] : "",
      createdAt: Date.now(),
    };

    await db.doc(`city_mood/${city}/weekly/${mood.weekId}`).set(mood);
    logger.info(`City mood written for ${city}`);
  }
});

/** Monthly: Voice of Bangalore badge for top 10 contributors. */
exports.onMonthlyVoiceBadge = onSchedule("0 4 1 * *", async () => {
  const city = "bangalore";
  const usersSnap = await db.collection("users").where("city", "==", city).get();

  const scored = [];
  for (const doc of usersSnap.docs) {
    const thoughtsSnap = await db.collection("thoughts").where("authorId", "==", doc.id).get();
    const feels = thoughtsSnap.docs.reduce((sum, t) => sum + (t.get("feelCount") || 0), 0);
    const comments = doc.get("totalCommentsWritten") || 0;
    const score = thoughtsSnap.size * 10 + comments * 5 + feels;
    scored.push({ uid: doc.id, score });
  }

  scored.sort((a, b) => b.score - a.score);
  const top = scored.slice(0, 10);
  for (const { uid, score } of top) {
    await db.doc(`users/${uid}`).update({
      badges: admin.firestore.FieldValue.arrayUnion("voice_of_bangalore"),
      voiceScore: score,
    });
  }
});

async function deleteSubcollection(ref, batchSize = 500) {
  const snap = await ref.limit(batchSize).get();
  if (snap.empty) return;
  const batch = db.batch();
  snap.docs.forEach((doc) => batch.delete(doc.ref));
  await batch.commit();
  if (snap.size >= batchSize) {
    await deleteSubcollection(ref, batchSize);
  }
}

async function deleteThoughtWithSubcollections(thoughtRef) {
  await deleteSubcollection(thoughtRef.collection("comments"));
  await deleteSubcollection(thoughtRef.collection("feels"));
  await thoughtRef.delete();
}

async function deleteCollectionGroupByField(collectionId, field, value) {
  let snap = await db.collectionGroup(collectionId).where(field, "==", value).limit(500).get();
  while (!snap.empty) {
    const batch = db.batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    if (snap.size < 500) break;
    snap = await db.collectionGroup(collectionId).where(field, "==", value).limit(500).get();
  }
}

/** Safety net: purge orphaned data when a user document is deleted. */
exports.onUserDeleted = onDocumentDeleted("users/{uid}", async (event) => {
  const uid = event.params.uid;
  logger.info(`Cleaning up data for deleted user ${uid}`);

  const thoughts = await db.collection("thoughts").where("authorId", "==", uid).get();
  for (const doc of thoughts.docs) {
    await deleteThoughtWithSubcollections(doc.ref);
  }

  await deleteCollectionGroupByField("comments", "userId", uid);
  await deleteCollectionGroupByField("feels", "uid", uid);
});
