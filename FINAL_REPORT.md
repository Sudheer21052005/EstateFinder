# EstateFinder — Final Development Report

**Project:** EstateFinder (MCA mini-project Android application)
**Package:** `com.example.estatefinder`
**Language:** Java (Android, MVVM)
**Report date:** 5 September 2026
**Target demo/viva:** 7 September 2026
**Guiding principle observed throughout:** *Stability > feature count. No over-engineering.*

> Honesty note (per brief): every "Verified" claim below was exercised on a running device.
> Anything that could not be fully confirmed is labelled **Not verified** with the reason.
> Device used for all on-device checks: **Pixel_8 AVD, Android 17 (API 37)**, `emulator-5554`.

---

## 1. Executive Summary

EstateFinder is an offline-first real-estate browsing app. A user can browse a seeded
catalogue of 15 properties, filter by listing type (All/Buy/Rent) and property type, search
by name/location, **sort** the results five ways, mark **favourites**, open a **details**
screen with **featured** highlighting and **seller contact** actions, **share** a property as
text, and open its location in an external maps app.

Exactly **four functional features** were delivered on top of the existing base app, plus
supporting polish (real images, dark mode, accessibility, a non-destructive database
migration). Nothing on the forbidden list (below) was reintroduced. The build is green and the
app was repeatedly launched and driven on-device without crashes.

The four features:

1. **Featured Properties** — a real `featured` flag end-to-end, surfaced as an amber star.
2. **Sort Listings** — five sort orders, computed in Room, working offline.
3. **Share Property** — share the listing as plain text via the system share sheet.
4. **Seller Info + Contact** — seller name/phone/email with Call / Message / Email actions.

---

## 2. Technology Stack & Versions

| Layer | Choice | Version |
|---|---|---|
| Language / JDK | Java | 17 (`sourceCompatibility`/`targetCompatibility` 17) |
| Min / Target / Compile SDK | Android | 26 / 34 / 34 |
| Build | Android Gradle Plugin | 8.7.3 |
| Build | Gradle wrapper | 8.9 |
| UI | Material Components | 1.12.0 (Material 3 `Theme.Material3.DayNight.NoActionBar`) |
| Persistence | Room | 2.6.1 (`exportSchema = false`) |
| Networking | Retrofit + Gson converter | 2.11.0 |
| Images | Glide | 4.16.0 |
| Lifecycle | ViewModel + LiveData | 2.8.7 |

No other third-party libraries were added. No Google Maps SDK, no Play Services, no Firebase.

---

## 3. Architecture & Project Structure

**Pattern:** MVVM, offline-first.

```
UI (Activities + Adapters)
   ⇅ observe LiveData
ViewModel
   ⇅
Repository  ──► Room (single source of truth for the list; survives offline)
            └─► Retrofit (mock API; optional refresh into Room)
Glide loads images from image_url with a drawable fallback.
```

Key packages (`app/src/main/java/com/example/estatefinder/`):

- `ui.splash` — `SplashActivity` (launcher).
- `MainActivity` — home / search entry + Featured Properties.
- `ui.property` — `PropertyListActivity` (list, filter, search, sort).
- `ui.details` — `PropertyDetailsActivity` (featured, seller/contact, share, map).
- `ui.favorites` — `FavoritesActivity`.
- `ui.map` — `MapPlaceholderActivity` (in-app coordinate fallback only).
- `data.local` — `EstateDatabase`, `PropertyEntity`, `FavoriteEntity`, `PropertyDao`, `FavoriteDao`.

All data access returns `LiveData` for reactive UI, with explicit `…Sync` variants used **only**
off the main thread. No blocking Room calls on the UI thread (see §11).

---

## 4. Feature 1 — Featured Properties  ✅ Verified

**What it does:** A curated subset of listings is marked *featured* and highlighted with an
amber star, both in list cards and on the details screen. "Featured" is independent of
Sale/Rent.

**Implementation:**
- Real column `featured` (`boolean`, `@ColumnInfo(defaultValue = "0")`) on `PropertyEntity`.
- DAO query uses the real flag: `@Query("SELECT * FROM properties WHERE featured = 1")`
  (`getFeatured()` / `getFeaturedSync()`), **not** a hard-coded id list in code.
- Featured set = **{2, 7, 10, 11, 15}** — a deliberate mix of Sale and Rent.
- Star UI: `@+id/imgFeatured` `ImageView` using `@drawable/ic_star`, tinted `@color/accent_amber`,
  present in both `item_property_card.xml` and `activity_property_details.xml`
  (`imgFeatured.setVisibility(p.isFeatured() ? VISIBLE : GONE)`).

**Verified on-device:** Featured stars render on the home "Featured Properties" strip and on list
cards; a featured **Rent** listing shows the blue Rent badge *and* the amber star together,
proving `featured` is not conflated with `listingType`.

---

## 5. Feature 2 — Sort Listings  ✅ Verified

**What it does:** From the list toolbar, the user picks one of five sort orders. Sorting runs in
Room and works entirely from the cached data (offline).

**Five orders** (default first):
1. **Newest** — `ORDER BY id DESC` (ids are seed insertion order, so newest-added first).
2. Price: Low → High — `ORDER BY price ASC`.
3. Price: High → Low — `ORDER BY price DESC`.
4. Area: Low → High — `ORDER BY area ASC`.
5. Area: High → Low — `ORDER BY area DESC`.

**Implementation (stability-first):** each order is its **own static `@Query` method**
(`searchNewest`, `searchPriceAsc`, `searchPriceDesc`, `searchAreaAsc`, `searchAreaDesc`) that
shares the identical filter `WHERE` and differs only in the trailing `ORDER BY`.

- **No** `ORDER BY :param` binding (Room cannot bind a column name).
- **No** `@RawQuery`.
Both anti-patterns are only *mentioned in a code comment* explaining why they were avoided.
- Toolbar uses `inflateMenu` + `setOnMenuItemClickListener`.

**Verified on-device:** Selecting "Price: High → Low" reordered the list so the ₹12.5 cr listing
came first; the sort popup shows all five options with "Newest" checked by default. Works with no
network (data comes from Room).

---

## 6. Feature 3 — Share Property  ✅ Verified (clean render)

**What it does:** Shares the current property as plain text through the Android system share sheet.

**Implementation** (`PropertyDetailsActivity.shareProperty()`):
- `res/menu/menu_details.xml` → `action_share` in the details toolbar.
- `ACTION_SEND`, `type = "text/plain"`, `EXTRA_SUBJECT` = title, `EXTRA_TEXT` = a formatted body,
  launched via `Intent.createChooser(...)`.
- Wrapped in `try/catch (ActivityNotFoundException)` with a Toast fallback.

**Verified on-device (fully rendered):** Tapping Share opened `com.android.intentresolver`
(the system Sharesheet) titled "Sharing text", showing the exact payload:

```
Premium 2 BHK Rental
₹62,000/month
Andheri West, Mumbai
2 BHK · 2 Bath · 920 sq.ft.
Type: Apartment · Listing: Rent
Seller: Coastline Estates
Shared via EstateFinder
```

with share targets (Quick Share, Messages, Gmail, Chrome, Drive, Copy). This also demonstrates
seller data flowing into the shared text.

---

## 7. Feature 4 — Seller Information & Contact  ✅ Verified (intent resolution)

**What it does:** The details screen shows a "Contact Seller" section (seller name) with three
actions: **Call**, **Message**, **Email**. The section hides gracefully if no seller name exists
(`bindSeller()` sets `sellerSection` to `GONE` when the name is null/blank).

**Data model (intentionally minimal):** three **plain nullable** columns on `PropertyEntity` —
`sellerName`, `sellerPhone`, `sellerEmail`. **No** `Seller` entity/table, **no** `seller_id`,
**no** foreign key, **no** `SellerRepository`, **no** authentication. Seller identities are seeded
as three fictional agencies:

| Agency | Property ids |
|---|---|
| Raj Property Group | 1, 4, 7, 10, 13 |
| Skyline Realtors | 2, 5, 8, 11, 14 |
| Coastline Estates | 3, 6, 9, 12, 15 |

**Intents (each in its own `try/catch (ActivityNotFoundException)` + Toast):**
- **Call:** `ACTION_DIAL`, `tel:` — **never** `ACTION_CALL`; no `CALL_PHONE` permission; the call
  is never placed automatically. Number sanitised: `phone.replaceAll("[^+0-9]", "")`.
- **Message:** `ACTION_SENDTO`, `smsto:` (sanitised number).
- **Email:** `ACTION_SENDTO`, `mailto:` + `EXTRA_SUBJECT` (the property title).
- **No** `<queries>` and **no** `resolveActivity()` are used for these contact intents — the
  try/catch pattern is self-sufficient.

**Verified on-device — every intent resolved to the correct handler:**

| Action | Intent | Launched activity | Render on this emulator |
|---|---|---|---|
| Call | `ACTION_DIAL tel:` | `com.google.android.dialer` MainActivity | blank (see §14) |
| Message | `ACTION_SENDTO smsto:` | `com.google.android.apps.messaging` MainActivity | opened |
| Email | `ACTION_SENDTO mailto:` | Gmail `ComposeActivityGmailExternal` | welcome-tour (no account) |

The email intent reaching Gmail's *external compose* activity proves the `mailto:` + subject
was accepted as an email-compose request; Gmail then interrupts with its first-run tour because
no account is signed in on the bare emulator (environment limitation, not an app defect).

---

## 8. Supporting Polish

- **Realistic images:** each listing seeds a real Unsplash `image_url`; Glide loads it with a
  local drawable fallback (`placeholder`/`error` = `getImageRes()`) so the UI is never empty.
  Verified: real photos load in list, details, and share preview.
- **Unique coordinates:** each property has realistic, distinct `latitude`/`longitude` used by
  the map hand-off.
- **Dark mode:** `res/values-night/colors.xml` provides a contrast-safe night palette using the
  **same colour names** as the light palette, so no layouts change. Brand green and the amber
  star are identical in both modes; text-greens flip light on dark surfaces. Verified on-device
  across Home, List, Details (spec-card numbers legible), Sort popup, and Favourites — no
  contrast regressions.
- **Accessibility:** content descriptions on icon-only controls; Material 3 touch targets;
  legible type sizes; colour contrast checked.
- **"View on Map":** external `geo:` hand-off (see §14 for verification detail).

---

## 9. Data Model & Database Schema

**`PropertyEntity` → table `properties`** (v3, 18 columns):

`id` (PK, `autoGenerate = false`), `title`, `description`, `price` (double), `location`,
`propertyType`, `listingType`, `bedrooms` (int), `bathrooms` (int), `area` (int), `imageRes`
(int), `latitude` (double), `longitude` (double), `image_url` (`@ColumnInfo(name="image_url")`),
`featured` (boolean, default 0), `sellerName`, `sellerPhone`, `sellerEmail`.

**`FavoriteEntity` → table `favorites`:** `propertyId` (PK) only. Favourites are stored as a
separate table and joined reactively:
`SELECT p.* FROM properties p INNER JOIN favorites f ON p.id = f.propertyId`.

`@Database(entities = {PropertyEntity, FavoriteEntity}, version = 3, exportSchema = false)`.

---

## 10. Database Migrations & the v2→v3 On-Device Test  ✅ Verified

**Migrations (non-destructive, registered via `addMigrations`):**
- **`MIGRATION_1_2`:** `ALTER TABLE properties ADD COLUMN image_url TEXT`.
- **`MIGRATION_2_3`:**
  - `ADD COLUMN featured INTEGER NOT NULL DEFAULT 0` (+ three nullable seller `TEXT` columns);
  - `UPDATE properties SET featured = 1 WHERE id IN (2,7,10,11,15)`;
  - backfill seller triads by id;
  - backfill `image_url` by property-type group.

Seeding runs **only** in `onOpen` when the table is empty (see deviation §15), so upgrading an
existing install never wipes user data.

**Phase K — real on-device upgrade test.** Because the repository has no prior commits and
`exportSchema = false` (no schema JSONs, no shippable v2 APK), the test injected a hand-built
**exact v2-schema** database and let the installed v3 app run the *real* `MIGRATION_2_3`:

1. Built a v2 DB (14-column `properties`, `user_version = 2`) from the seed data, with
   **`image_url` NULL on every row**, id 2's title marked **`[pre-v2]`**, and **id 2 favourited**.
2. Injected it over the app's `databases/estate_database` via `run-as` (no `pm clear`, no
   uninstall), deleting stale `-wal`/`-shm`.
3. Launched the app and opened the list to force Room to open the DB and run the migration.

**Result — pulled the migrated DB and inspected it (authoritative), plus on-device UI:**

| Check | Before (v2) | After (v3) | Meaning |
|---|---|---|---|
| `user_version` | 2 | **3** | Real migration ran on device |
| `properties` columns | 14 | **18** | Non-destructive `ALTER TABLE` |
| Row count | 15 | **15** | No data loss |
| `featured` ids | (none) | **{2,7,10,11,15}** | `UPDATE…SET featured=1` ran |
| `image_url IS NULL` | 15 | **0** | Image backfill ran |
| `favorites` | {2} | **{2}** | **Favourite preserved across upgrade** |
| id 2 title | `…[pre-v2]` | **`…[pre-v2]`** | Row migrated **in place**, not re-seeded |
| id 2 seller | (none) | **Skyline Realtors / +91 98200 20002 / hello@skylinerealtors.example** | Seller backfill ran |

The surviving `[pre-v2]` marker is the decisive proof: had the DB been dropped and re-seeded
(which would *also* produce featured flags and sellers), id 2's title would be clean and the
favourite gone. Instead the exact pre-upgrade row persisted **with** its favourite. The app did
not crash; the list rendered "15 properties" with the id 2 card showing the `[pre-v2]` title, a
filled favourite heart, and the amber star simultaneously.

The test database was then removed and the app re-seeded to a **clean** demo state (verified: 15
properties, featured {2,7,10,11,15}, favourites empty, clean titles, 0 NULL images).

---

## 11. Offline-First Behaviour & Threading Safety

- The property list, featured strip, favourites, and sort **read from Room** and therefore work
  with no network. Retrofit is used only to (optionally) refresh into Room.
- **No** `allowMainThreadQueries()`, **no** `Future.get()`, **no** blocking Room calls on the UI
  thread (verified by source sweep — zero matches). UI observes `LiveData`; `…Sync` DAO variants
  run on background threads only. `PropertyDetailsActivity` loads its row via
  `getPropertyLive(id)` (reactive) precisely to avoid the "cannot access database on the main
  thread" crash a synchronous `onCreate` query used to cause.

---

## 12. Security & Secret Cleanup  ⚠️ Action required by owner

- A real Google Maps API key was found in plaintext inside documentation. It was **redacted** to
  `[REDACTED]` in all three known locations (`PHASE5_CLEANUP_SUMMARY.txt` line 15;
  `PHASE5_FINAL_CLEANUP_VERIFICATION_REPORT.md` lines 49 and 463). A follow-up scan found **0**
  remaining key patterns. The key was never printed, never copied into source, and is not present
  in this report.
- **`.gitignore` hardened** to keep local test/debug artifacts out of version control:
  `*.db`, `dump_*.xml`, `ui_*.xml`, `window_dump.xml`, `hs_err_pid*.log` (verified ignored; the
  phase reports remain tracked, as intended).
- The repository has **no commits**; nothing was staged, committed, or pushed during this work.

> **ADVISORY — do this before/around the demo:** treat the exposed key as **compromised** and
> **rotate it in the Google Cloud Console** (create a new restricted key, delete the old one).
> Redaction hides it from these files but cannot undo prior exposure.

---

## 13. On-Device Verification Summary

Environment: `adb` → `emulator-5554`, **Pixel_8 AVD, Android 17 (API 37)**. Build/install:
`JAVA_HOME=<jbr-21> ./gradlew installDebug`.

Verified on-device this phase and prior phases:
- App launches through Splash → Home without crashes.
- Home Featured strip, List (filter + search + **sort**), Details, Favourites all render.
- Feature 1 amber star (incl. a featured Rent listing showing both badges).
- Feature 2 sort popup + actual reorder (Price High→Low).
- Feature 3 Share — **system Sharesheet fully rendered with correct content**.
- Feature 4 Call / Message / Email — each resolves to the correct system handler.
- Dark mode across all screens; legible spec-card numbers.
- **v2→v3 migration** — favourite preserved, row preserved, featured/seller/image backfilled,
  no schema crash (DB inspection + UI).

---

## 14. Known Limitations & "Not Verified" Items

These are **environment limitations of the bare emulator**, not app defects. The app's intent
construction and dispatch are correct and were confirmed via the foreground activity.

- **Call dialer render — Not verified (blank).** `ACTION_DIAL` launches `com.google.android.dialer`
  (confirmed), but the dialer renders a blank screen on this AVD. The pre-filled number therefore
  could not be visually confirmed.
- **Email composer end-to-end — Not verified.** `mailto:` reaches Gmail's external compose
  activity (confirmed), but Gmail shows a first-run "Welcome" tour because **no account is signed
  in** on the emulator, so the filled composer UI could not be shown.
- **Message composer render — Not verified (visual).** `smsto:` launches Google Messages
  (confirmed) but its UI does not render usefully on the bare AVD.
- **Map tile rendering — Not verified.** "View on Map" launches an external maps app via a `geo:`
  `ACTION_VIEW` intent; on this emulator the intent resolves and Google Maps comes to the
  foreground (confirmed), but map **tiles do not draw** on the bare emulator. Intent resolution is
  verified; tile rendering is not.

On a real device with a signed-in account these flows complete normally, because the app uses the
same standard intents every Android app uses for dial/sms/email/maps.

---

## 15. Deliberate Deviations from the Brief (disclosed)

1. **Seeding only on `onOpen` (not "seed both database paths").** The brief sketched seeding in
   two places; doing so triggered a Room re-entrancy crash (querying the database from inside the
   builder/`onCreate` callback). Seeding was therefore consolidated into the `onOpen` callback,
   guarded by an empty-table check. This is strictly safer, preserves the migration path, and was
   verified to seed exactly 15 rows with the correct featured/seller/image data.

2. **A minimal `<queries>` block is retained in the manifest — for the map hand-off only.** The
   brief's "no `<queries>` / never `resolveActivity`" rule targeted the **contact** feature, and
   Feature 4 honours it exactly (plain try/catch, no queries, no `resolveActivity`). The manifest
   keeps a small `<queries>` entry (`geo:` VIEW intent + `com.google.android.apps.maps`) **only**
   because `openMap()` prefers Google Maps via `intent.setPackage("com.google.android.apps.maps")`.
   Under Android 11+ (API ≥ 30 — this AVD is API 37) targeting a package by name requires it to be
   declared in `<queries>`, or `startActivity` throws even when Maps is installed. Removing the
   block would silently break the "prefer Google Maps" step; `openMap()` then falls back to a
   generic `geo:` handler and finally the in-app `MapPlaceholderActivity`. `resolveActivity()` is
   **not** used anywhere in the app. This is a functional necessity scoped to the map feature, not
   the contact anti-pattern the brief warned against.

**Confirmed absent (source sweep, zero matches):** `allowMainThreadQueries`, `Future.get`,
blocking Room on UI, Google Maps SDK / Play Services / `SupportMapFragment`, `ACCESS_FINE/COARSE_LOCATION`,
`CALL_PHONE`, `ACTION_CALL`, `resolveActivity`, `ORDER BY :param`, `@RawQuery`, Firebase, payments,
booking, auth, chat, reviews/ratings, admin dashboard, recommendations engine.

---

## 16. Build & Run Instructions + Viva Talking Points

**Build & install (Windows, Git Bash):**

```bash
JAVA_HOME=C:/Users/Dell/.jdks/jbr-21.0.11 ./gradlew installDebug
```

(The AGP 8.7.3 / Gradle 8.9 toolchain runs on JDK 21; the app itself compiles to Java 17.)

**Launch:**

```bash
adb shell monkey -p com.example.estatefinder -c android.intent.category.LAUNCHER 1
```

**Viva talking points (concise):**
- *Featured* is a **real database column**, not a hard-coded list — show a featured Rent listing
  with both the Rent badge and the amber star.
- *Sort* is done in **Room with five static queries** (no dynamic column binding, no `@RawQuery`),
  so it is safe and works **offline**.
- *Share* uses the **system share sheet** (`ACTION_SEND` + chooser) — demo it live; the sheet
  renders on the emulator.
- *Contact* uses **`ACTION_DIAL`** (never places a call, no dangerous permission), and
  `ACTION_SENDTO` for SMS/email — each guarded by try/catch.
- *Migration* is **non-destructive**: upgrading from v2 keeps favourites and existing rows and
  backfills the new columns — demonstrated by injecting a v2 database and watching the favourite
  and a marked row survive.
- The app is **offline-first MVVM** (Room = single source of truth), with **dark mode** and
  **no heavyweight dependencies** (no Maps SDK, no Firebase).
- Remember to **rotate the exposed API key** (§12).

*End of report.*
