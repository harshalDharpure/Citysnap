/**
 * Permanently delete ALL posts (thoughts), their comments/feels, and post images.
 *
 * Prerequisites:
 *   cd functions && npm install
 *   Set GOOGLE_APPLICATION_CREDENTIALS to your Firebase service account JSON
 *
 * Usage:
 *   node scripts/clear_all_posts.js --yes
 */

"use strict";

const admin = require("firebase-admin");

if (!process.argv.includes("--yes")) {
  console.error(
    "This permanently deletes ALL posts and images in Storage (thoughts/).",
  );
  console.error("Re-run with: node scripts/clear_all_posts.js --yes");
  process.exit(1);
}

admin.initializeApp({
  projectId: "thoughts-3aa0c",
  storageBucket: "thoughts-3aa0c.firebasestorage.app",
});

const db = admin.firestore();
const bucket = admin.storage().bucket();

async function deleteSubcollection(ref, batchSize = 500) {
  let snap = await ref.limit(batchSize).get();
  while (!snap.empty) {
    const batch = db.batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    if (snap.size < batchSize) break;
    snap = await ref.limit(batchSize).get();
  }
}

async function deleteAllThoughts() {
  let total = 0;
  let snap = await db.collection("thoughts").limit(500).get();
  while (!snap.empty) {
    for (const doc of snap.docs) {
      await deleteSubcollection(doc.ref.collection("comments"));
      await deleteSubcollection(doc.ref.collection("feels"));
      await doc.ref.delete();
      total += 1;
      if (total % 50 === 0) {
        console.log(`  deleted ${total} posts...`);
      }
    }
    snap = await db.collection("thoughts").limit(500).get();
  }
  return total;
}

async function deleteThoughtStorage() {
  try {
    await bucket.deleteFiles({ prefix: "thoughts/", force: true });
  } catch (err) {
    console.warn(`Storage cleanup warning: ${err.message}`);
  }
}

async function clearUserPostReferences() {
  const users = await db.collection("users").get();
  for (const user of users.docs) {
    await deleteSubcollection(user.ref.collection("saved"));
    await deleteSubcollection(user.ref.collection("hidden"));
  }
}

async function main() {
  console.log("Deleting post images from Storage (thoughts/)...");
  await deleteThoughtStorage();

  console.log("Deleting Firestore posts and subcollections...");
  const count = await deleteAllThoughts();

  console.log("Clearing saved/hidden post references on user profiles...");
  await clearUserPostReferences();

  console.log(`Done. Removed ${count} posts. Feed should be empty after refresh.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
