# Citysnap Production Roadmap

Prioritized checklist for shipping and scaling. Use each section as GitHub issue groups.

**Legend:** `[x]` done · `[~]` in progress · `[ ]` not started

---

## Phase 1 — Ship-safe (beta → public launch)

| # | Task | Priority | Status |
|---|------|----------|--------|
| 1.1 | Firebase Crashlytics + Analytics | P0 | [x] |
| 1.2 | Wire notification prefs to Firestore + Cloud Functions | P0 | [x] |
| 1.3 | Account deletion cascade (client + Cloud Function) | P0 | [x] |
| 1.4 | GitHub Actions CI (assemble + unit tests) | P0 | [x] |
| 1.5 | Critical unit tests (FeedRanking, AppCity, ThoughtCategory) | P0 | [x] |
| 1.6 | Unify branding (Citysnap package/service names) | P1 | [ ] |
| 1.7 | Tighten backup rules (`data_extraction_rules.xml`) | P1 | [ ] |
| 1.8 | Firebase App Check | P1 | [ ] |
| 1.9 | Email verification before posting | P1 | [ ] |

---

## Phase 2 — Retention & product completeness

| # | Task | Priority | Status |
|---|------|----------|--------|
| 2.1 | Wire **Hot feed** (`FeedSortMode.HOT`) in UI | P0 | [ ] |
| 2.2 | Category picker in composer + feed filter | P0 | [ ] |
| 2.3 | Real pull-to-refresh (invalidate Firestore listener) | P1 | [ ] |
| 2.4 | **Public user profiles** (tap author → posts + badges) | P0 | [ ] |
| 2.5 | **In-app notification inbox** | P0 | [ ] |
| 2.6 | Feed pagination / cursor-based loading | P1 | [ ] |
| 2.7 | City expansion gates (`isExpansionUnlocked`) in UI | P2 | [ ] |
| 2.8 | Onboarding tooltips (first Snap, feels, locality filter) | P2 | [ ] |
| 2.9 | Guest comment read UX (align rules or show sign-in CTA) | P2 | [ ] |

---

## Phase 3 — Monetization (50K DAU gate)

| # | Task | Priority | Status |
|---|------|----------|--------|
| 3.1 | Premium save UI (`toggleSaveThought`) | P0 | [ ] |
| 3.2 | Google Play Billing (₹99/mo) | P0 | [ ] |
| 3.3 | Sponsored post ops workflow (beyond seed script) | P1 | [ ] |
| 3.4 | Premium upsell on profile (replace placeholder) | P1 | [ ] |

See also: [MONETIZATION.md](./MONETIZATION.md)

---

## Phase 4 — Scale & trust (100K+ DAU)

| # | Task | Priority | Status |
|---|------|----------|--------|
| 4.1 | Admin moderation dashboard (report queue) | P0 | [ ] |
| 4.2 | Server-side rate limits (posts, comments, reports) | P0 | [ ] |
| 4.3 | Image moderation (Vision API / manual review) | P1 | [ ] |
| 4.4 | Profanity / spam detection | P1 | [ ] |
| 4.5 | Jobs + City Insights API | P2 | [ ] |
| 4.6 | Multi-device FCM tokens | P2 | [ ] |
| 4.7 | Staging Firebase project + Remote Config flags | P1 | [ ] |

---

## Engineering quality

| # | Task | Priority | Status |
|---|------|----------|--------|
| E.1 | ViewModel + repository unit tests (mock Firebase) | P1 | [ ] |
| E.2 | Compose UI tests (login, post, block) | P2 | [ ] |
| E.3 | Hilt DI for testability | P2 | [ ] |
| E.4 | Room offline cache for feed | P3 | [ ] |
| E.5 | Image CDN / resize variants | P3 | [ ] |
| E.6 | Accessibility audit (TalkBack, contrast) | P2 | [ ] |
| E.7 | Full Hindi localization | P3 | [ ] |

---

## Explicitly out of scope (unless product pivots)

- Direct messaging / chat
- Stories / Reels
- iOS / Web clients
- Follow graph / @mentions (planned Phase 2+ if needed)

---

## Deploy checklist (after Phase 1 changes)

```bash
# Android
./gradlew assembleRelease testDebugUnitTest

# Firebase (rules, indexes, functions)
firebase deploy --only firestore:rules,firestore:indexes,functions
```

Enable **Crashlytics** and **Analytics** in Firebase Console for the project tied to `google-services.json`.
