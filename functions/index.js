"use strict";

/**
 * hoght Cloud Functions — notifications, prompts, trending, badges, city mood.
 *
 * Deploy: firebase deploy --only functions,firestore:rules,firestore:indexes
 */

const { onDocumentCreated, onDocumentDeleted, onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();
const bucket = admin.storage().bucket();

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
    const messagesOn = userSnap.get("notifyMessages") !== false;
    const allowed = {
      feel: feelsOn,
      milestone: feelsOn,
      trending: feelsOn,
      comment: commentsOn,
      daily_prompt: promptsOn,
      locality_topic: promptsOn,
      message: messagesOn,
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
        conversationId: data.conversationId || "",
        senderId: data.senderId || "",
      },
      android: { priority: "high" },
    });

    if (type) {
      await db.collection(`users/${uid}/notifications`).add({
        type,
        title,
        body,
        thoughtId: data.thoughtId || "",
        conversationId: data.conversationId || "",
        senderId: data.senderId || "",
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

async function updateCounter(ref, field, delta) {
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return 0;
    const current = snap.get(field) || 0;
    const next = Math.max(0, current + delta);
    tx.update(ref, { [field]: next });
    return next;
  });
}

function publicProfileForUser(uid, user) {
  return {
    uid,
    name: user.name || "User",
    photoUrl: user.photoUrl || "",
    city: user.city || "",
    locality: user.locality || "",
    postStreak: user.postStreak || 0,
    voiceScore: user.voiceScore || 0,
    badges: Array.isArray(user.badges) ? user.badges : [],
  };
}

async function syncReferralCode(uid, before, after) {
  const beforeCode = before && before.referralCode ? before.referralCode : "";
  const afterCode = after && after.referralCode ? after.referralCode : "";
  if (beforeCode && beforeCode !== afterCode) {
    await db.doc(`referral_codes/${beforeCode}`).delete();
  }
  if (afterCode) {
    await db.doc(`referral_codes/${afterCode}`).set({
      uid,
      code: afterCode,
      updatedAt: Date.now(),
    });
  }
}

/** Keep public profile data separate from private user documents. */
exports.onUserProfileWritten = onDocumentWritten("users/{uid}", async (event) => {
  const uid = event.params.uid;
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;

  if (!after) {
    await db.doc(`public_profiles/${uid}`).delete();
    await syncReferralCode(uid, before, null);
    return;
  }

  await db.doc(`public_profiles/${uid}`).set(publicProfileForUser(uid, after));
  await syncReferralCode(uid, before, after);
});

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
  const thoughtRef = db.doc(`thoughts/${thoughtId}`);
  const feelCount = await updateCounter(thoughtRef, "feelCount", 1);
  const thoughtSnap = await thoughtRef.get();
  if (!thoughtSnap.exists) return;

  const authorId = thoughtSnap.get("authorId");
  if (!authorId || authorId === feelerUid) return;

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

/** Keep public counters server-owned when a reaction is removed. */
exports.onFeelDeleted = onDocumentDeleted("thoughts/{thoughtId}/feels/{uid}", async (event) => {
  const { thoughtId } = event.params;
  await updateCounter(db.doc(`thoughts/${thoughtId}`), "feelCount", -1);
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

    const thoughtRef = db.doc(`thoughts/${thoughtId}`);
    await updateCounter(thoughtRef, "commentCount", 1);
    if (comment.userId) {
      await updateCounter(db.doc(`users/${comment.userId}`), "totalCommentsWritten", 1);
    }

    const thoughtSnap = await thoughtRef.get();
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

/** Keep comment counters correct when a comment is deleted. */
exports.onCommentDeleted = onDocumentDeleted(
  "thoughts/{thoughtId}/comments/{commentId}",
  async (event) => {
    const { thoughtId } = event.params;
    const comment = event.data && event.data.data();
    await updateCounter(db.doc(`thoughts/${thoughtId}`), "commentCount", -1);
    if (comment && comment.userId) {
      await updateCounter(db.doc(`users/${comment.userId}`), "totalCommentsWritten", -1);
    }
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

async function deleteStoragePrefix(prefix) {
  try {
    await bucket.deleteFiles({ prefix, force: true });
  } catch (err) {
    logger.warn(`Failed to delete storage prefix ${prefix}: ${err.message}`);
  }
}

function conversationIdFor(uidA, uidB) {
  const sorted = [uidA, uidB].sort();
  return `${sorted[0]}_${sorted[1]}`;
}

async function isBlockedPair(uidA, uidB) {
  const [a, b] = await Promise.all([
    db.doc(`users/${uidA}/blocked/${uidB}`).get(),
    db.doc(`users/${uidB}/blocked/${uidA}`).get(),
  ]);
  return a.exists || b.exists;
}

async function checkMessageRateLimit(senderId) {
  const hourBucket = Math.floor(Date.now() / 3600000);
  const ref = db.doc(`users/${senderId}/rate_limits/message_hour_${hourBucket}`);
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const count = snap.exists ? snap.get("count") || 0 : 0;
    if (count >= 30) return false;
    tx.set(ref, { count: count + 1, updatedAt: Date.now() }, { merge: true });
    return true;
  });
}

/** Sync inbox metadata and notify recipient when a DM is sent. */
exports.onMessageCreated = onDocumentCreated(
  "conversations/{conversationId}/messages/{messageId}",
  async (event) => {
    const { conversationId, messageId } = event.params;
    const message = event.data?.data();
    if (!message) return;

    const senderId = message.senderId || "";
    const text = (message.text || "").trim();
    if (!senderId || !text) return;

    const convoRef = db.doc(`conversations/${conversationId}`);
    const convoSnap = await convoRef.get();
    if (!convoSnap.exists) {
      await event.data.ref.delete();
      return;
    }

    const participantIds = convoSnap.get("participantIds") || [];
    if (participantIds.length !== 2 || !participantIds.includes(senderId)) {
      await event.data.ref.delete();
      return;
    }

    const recipientId = participantIds.find((id) => id !== senderId);
    if (!recipientId) return;

    if (await isBlockedPair(participantIds[0], participantIds[1])) {
      await event.data.ref.delete();
      return;
    }

    const allowed = await checkMessageRateLimit(senderId);
    if (!allowed) {
      await event.data.ref.delete();
      logger.warn(`Rate limit exceeded for sender ${senderId}`);
      return;
    }

    const createdAt = message.createdAt || Date.now();
    const preview = text.length > 120 ? `${text.slice(0, 117)}...` : text;

    const senderProfile = await db.doc(`public_profiles/${senderId}`).get();
    const senderName = senderProfile.get("name") || "Someone";

    await notifyUser(recipientId, senderName, preview, {
      type: "message",
      conversationId,
      senderId,
    });
  },
);

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

  const convos = await db.collection("conversations").where("participantIds", "array-contains", uid).get();
  for (const doc of convos.docs) {
    await deleteSubcollection(doc.ref.collection("messages"));
    await doc.ref.delete();
  }

  for (const sub of ["blocked", "hidden", "referrals", "saved", "notifications", "conversation_meta", "rate_limits"]) {
    await deleteSubcollection(db.collection(`users/${uid}/${sub}`));
  }
  await deleteStoragePrefix(`profiles/${uid}/`);
  await deleteStoragePrefix(`thoughts/${uid}/`);
});
