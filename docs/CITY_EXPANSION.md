# City Expansion Playbook (Phase 9)

Expand hoght one city at a time. Do **not** open the next city until Bangalore (or the current focus city) hits all gates.

## Gates (per city)

| Metric | Target |
|--------|--------|
| DAU | 1,000 |
| Organic posts/day | 100 |
| D1 retention | 30% |

## Expansion order

1. **Bangalore** — launch city (marketing focus Month 1)
2. **Pune** — Month 2
3. **Hyderabad** — Month 3
4. **Chennai**
5. **Mumbai**
6. **Delhi**

## Checklist per new city

### 1. Data & rules
- Add city id to `AppCity.kt` (already in codebase)
- Add localities to `AppLocality.kt` (city-specific list)
- Update `validCity()` in `firestore.rules` if not already present
- Add locality whitelist for the new city in rules

### 2. Seed content (before marketing)
- Run `scripts/seed_firestore.js` adapted for the city:
  - 500 thoughts
  - ~2,000 comments
  - Categories: work, rent, traffic, college, relationships, food, startup
- Stagger `createdAt` over 30 days

### 3. Prompts
- Add city prompts in `DailyPrompts.kt`
- Deploy daily prompt Cloud Function path: `prompts/{city}/daily/{date}`

### 4. Marketing
- Update Play Store geo-targeting
- WhatsApp / college / office channel campaigns in that city only
- Invite links with city-specific copy

### 5. App UX
- Default onboarding city can stay user-selected; marketing drives one city
- Feed query: `whereEqualTo("city", cityId)` (already implemented)

### 6. Monitor (2 weeks)
- DAU, posts/day, D1 retention, share rate
- If gates not met: more seed + prompts, do not expand

## Code references

- Cities: `app/src/main/java/com/prod/singles_date/model/AppCity.kt`
- Localities: `app/src/main/java/com/prod/singles_date/model/AppLocality.kt`
- Seed script: `scripts/seed_firestore.js`
- Firestore indexes: `firestore.indexes.json`
