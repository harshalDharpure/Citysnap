# hoght Firebase scripts

## Prerequisites

1. [Firebase CLI](https://firebase.google.com/docs/cli): `npm install -g firebase-tools`
2. Log in: `firebase login`
3. Service account JSON from Firebase Console → Project settings → Service accounts → Generate new private key
4. Set env var (PowerShell):

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\serviceAccount.json"
```

## Deploy rules, indexes, and Cloud Functions

From project root:

```powershell
cd C:\Users\HARSHAL\AndroidStudioProjects\InnerCircleTalks
firebase deploy --only firestore:rules,firestore:indexes,functions
```

Requires Blaze plan for Cloud Functions (FCM + scheduled jobs).

## Seed content (run before marketing)

```powershell
# 500 thoughts + ~2000 comments
node scripts/seed_firestore.js

# This week's city mood + today's prompt (instant feed testing)
node scripts/seed_city_mood.js
node scripts/seed_city_mood.js --city=pune

# Optional sponsored posts
node scripts/seed_sponsored.js
```

## Premium test user (Firebase Console or script)

Set on `users/{uid}`:

```json
{ "isPremium": true }
```

Then saved thoughts work at `users/{uid}/saved/{thoughtId}`.

## Scheduled Cloud Functions (after deploy)

| Function | Schedule | Purpose |
|----------|----------|---------|
| `onDailyPrompt` | Daily 9 AM IST | Push daily writing prompt |
| `onWeeklyLocalityTopic` | Sunday | Locality topic notification |
| `onWeeklyCityMood` | Sunday | Generate city mood card |
| `onTrendingRank` | Nightly | Trending rank + notification |
| `onMonthlyVoiceBadge` | 1st of month | Voice of Bangalore badge |
