# Play Store launch checklist

## GitHub Pages (your policy host)

Repo: [github.com/harshalDharpure/Citysnap](https://github.com/harshalDharpure/Citysnap)

### One-time setup

1. GitHub → **Citysnap** repo → **Settings** → **Pages**
2. **Build and deployment** → Source: **Deploy from a branch**
3. Branch: **main** (or **master**), folder: **/docs**
4. Save — site goes live in 1–2 minutes at:

| Page | Public URL |
|------|------------|
| Privacy policy | https://harshalDharpure.github.io/Citysnap/privacy.html |
| Privacy (alt) | https://harshalDharpure.github.io/Citysnap/ |
| Child safety | https://harshalDharpure.github.io/Citysnap/child-safety.html |
| Delete account | https://harshalDharpure.github.io/Citysnap/delete-account.html |

Push changes to `docs/` on the default branch; GitHub redeploys automatically.

**Contact for Play Console, privacy, and support:** `citysnap.play.review@gmail.com`

## Firebase App Check (required before enforcing rules)

1. Firebase Console → App Check → register Android app with **Play Integrity**
2. For debug builds: run app once, copy debug token from Logcat (`DebugAppCheckProvider`), add in Console → Manage debug tokens
3. After tokens work, enable enforcement for Firestore in Console (gradual rollout recommended)

## Deploy backend updates

```bash
firebase deploy --only firestore:rules
# Redeploy S3 Lambda (backend/s3-api/index.js) with FIREBASE_WEB_API_KEY env var set
```

## Play Console

- **Privacy policy URL:** `https://harshalDharpure.github.io/Citysnap/privacy.html`
- **Child safety URL:** `https://harshalDharpure.github.io/Citysnap/child-safety.html`
- **Delete account URL:** `https://harshalDharpure.github.io/Citysnap/delete-account.html`
- **Contact email:** `citysnap.play.review@gmail.com`
- **Data safety:** Firebase Auth/Firestore, AWS S3 photos, FCM token, Analytics, Crashlytics — no ads, no AD_ID
- **Content rating:** user-generated content; declare moderation (report/block)
- **Signed AAB:** `./gradlew bundleRelease` with `keystore.properties` filled in
- **Store listing:** use `store_listing.txt` (Citysnap branding)

## Data safety accuracy

- Photos: **Amazon S3** (not Firebase Storage)
- Account/posts: **Google Firebase** (Auth + Firestore)
- Crash/analytics: **Firebase Crashlytics + Analytics** (no advertising ID)
