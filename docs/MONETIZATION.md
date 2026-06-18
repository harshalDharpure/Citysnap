# Monetization (Phase 10)

Gates from the product plan — enable each layer only after hitting DAU targets.

| Stage | DAU gate | Feature | Implementation |
|-------|----------|---------|----------------|
| 1 | 1K | No ads | Default — no ad SDK |
| 2 | 10K | Sponsored thoughts | `Thought.isSponsored`, `sponsorLabel`; seed via Admin SDK only |
| 3 | 50K | Premium ₹99/mo | `User.isPremium`; `users/{uid}/saved/{thoughtId}` subcollection |
| 4 | 100K+ | Jobs + City Insights | Not built — future API/reports |

## Sponsored thoughts

Create via Firebase Admin SDK or `scripts/seed_sponsored.js` (not from the Android client).

```js
await db.collection("thoughts").add({
  text: "New coworking space opening in Whitefield.",
  city: "bangalore",
  locality: "whitefield",
  category: "startup",
  isSponsored: true,
  sponsorLabel: "Local coworking",
  feelCount: 0,
  commentCount: 0,
  shareCount: 0,
  anonymous: true,
  authorId: "sponsor_system",
  createdAt: Date.now(),
});
```

Feed shows **Sponsored · {label}** on matching cards.

## Premium

Set `isPremium: true` on `users/{uid}` via Admin SDK for beta testers.

Premium users can save thoughts to `users/{uid}/saved/{thoughtId}`.

Payment (Play Billing / Razorpay) is not wired yet — profile shows upsell placeholder.
