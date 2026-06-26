# Citysnap — Child Safety Policy (README)

This folder contains the **Child Safety Standards** documents required for Google Play’s CSAE/CSAM compliance. They are published via **GitHub Pages** from this `docs/` folder.

---

## Files

| File | Purpose |
|------|---------|
| [`child-safety-policy.md`](./child-safety-policy.md) | Markdown source — easy to edit or copy into Play Console |
| [`child-safety.html`](./child-safety.html) | Public web page (GitHub Pages) |
| [`privacy.html`](./privacy.html) | Privacy policy (GitHub Pages) |
| This README | Setup and Play Console reference |

---

## Public URLs (GitHub Pages)

After Pages is enabled (Settings → Pages → branch **main**, folder **/docs**):

| Document | URL |
|----------|-----|
| Privacy policy | https://harshalDharpure.github.io/Citysnap/privacy.html |
| Child safety | https://harshalDharpure.github.io/Citysnap/child-safety.html |

**Contact:** citysnap.play.review@gmail.com  
**Effective date:** March 31, 2026  

---

## Enable GitHub Pages (one time)

1. Open https://github.com/harshalDharpure/Citysnap/settings/pages
2. **Source:** Deploy from a branch
3. **Branch:** `main` → `/docs` → **Save**
4. Wait ~1 minute, then open the child-safety URL above in a browser

To update policies: edit files in `docs/`, commit, and push to `main`.

---

## Google Play Console

1. **Policy** → **App content** → **Privacy policy**  
   URL: `https://harshalDharpure.github.io/Citysnap/privacy.html`

2. **Child safety** (CSAE/CSAM)  
   URL: `https://harshalDharpure.github.io/Citysnap/child-safety.html`  
   Contact: `citysnap.play.review@gmail.com`

3. Confirm in-app **Report** and **Block** work on a release build.

---

## Quick checklist

- [ ] GitHub Pages enabled (`/docs` on `main`)
- [ ] Privacy URL loads in browser (no login)
- [ ] Child safety URL loads in browser
- [ ] Play Console contact: `citysnap.play.review@gmail.com`
- [ ] Data safety form matches Firebase, S3, FCM, Analytics

---

## Questions?

**citysnap.play.review@gmail.com**
