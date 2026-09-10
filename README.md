# 🏠 EstateFinder — Real Estate Property Finder

A native Android app to **browse, search, filter, sort and shortlist** real-estate
listings, view seller details, and open a property's location in a maps app.
Built as a Mobile Computing mini-project on a clean **MVVM** architecture with an
offline-first **Room** database and a **Retrofit** REST layer.

> **Java · XML layouts · Material 3 · MVVM · Room · Retrofit · Glide** — no Kotlin, no Jetpack Compose.

## ✨ Features

| # | Feature | Description |
|---|---|---|
| 1 | Featured properties | Curated listings highlighted on the Home screen. |
| 2 | Search & filter | Search by title/location; filter by **Buy/Rent** and property type (Apartment / House / Villa / Office). |
| 3 | Sort listings | Newest, Price (low→high / high→low), Area (low→high / high→low). |
| 4 | Property details | Full specs, description, photo and a featured badge. |
| 5 | Favorites | Shortlist properties — **persisted in Room** and kept in sync across every screen. |
| 6 | Share property | Share the listing as text through the Android share sheet. |
| 7 | Seller contact | **Call** (opens the dialer), **Message** (SMS) and **Email** the seller. |
| 8 | View on map | Opens the location in an external maps app via a `geo:` intent, with an in-app coordinate fallback. |
| 9 | Refresh from API | Pulls listings from a REST endpoint (Retrofit) into the local Room cache. |

## 🧱 Architecture

Single-source-of-truth **MVVM**. The UI only talks to the ViewModel; the Repository
owns the data and merges the local + remote layers.

```
UI  (Activities + RecyclerView)
        │  observes LiveData
        ▼
PropertyViewModel   ── AndroidViewModel, survives rotation
        │
        ▼
PropertyRepository  ── single source of truth
    ├── Room     (local)  ← offline cache + favorites   [primary read source]
    └── Retrofit (remote) ← refresh listings from a REST API
```

- **Offline-first** — the app reads from Room, so it works with no network. *Refresh*
  fetches from the API and upserts into Room; the UI updates automatically via LiveData.
- **Reactive favorites** — favorites live in a `favorites` table and are resolved with a
  Room `JOIN`, so the heart icon stays consistent on Home, List, Details and Favorites.
- **No blocking work on the UI thread** — all database and network calls run on a
  background executor.

## 🗺️ Maps — no SDK, no API key

This app **does not** use the Google Maps SDK, the Fused Location Provider, a Maps API
key, or any location permission. Tapping **View on Map** builds a `geo:lat,lng` intent
and hands it to whatever maps app the device has, falling back to a simple in-app
coordinate screen. The **only** permission the app requests is `INTERNET` (for the REST
API and image loading).

## ⚙️ Tech stack

| Layer | Library / Tool |
|---|---|
| Language / UI | Java, XML layouts, Material 3 (DayNight light/dark) |
| Architecture | MVVM — ViewModel + LiveData (`androidx.lifecycle` 2.8.7) |
| Local database | Room 2.6.1 (entities, DAOs, migrations, seeding) |
| Networking | Retrofit 2.11.0 + Gson converter, OkHttp logging-interceptor 4.12.0 |
| Images | Glide 4.16.0 |
| UI widgets | AppCompat 1.7.0, ConstraintLayout 2.2.0, RecyclerView 1.3.2 |

## 🔧 Configuration

| Setting | Value |
|---|---|
| Package / applicationId | `com.example.estatefinder` |
| minSdk / targetSdk / compileSdk | 26 / 34 / 34 |
| Java | 17 |
| Gradle / AGP | 8.9 / 8.7.3 |
| Permissions | `INTERNET` only |

## ▶️ Build & run

Open in Android Studio (**File → Open →** select this folder) and let Gradle sync, or
use the wrapper:

```bash
./gradlew assembleDebug     # build the debug APK
./gradlew installDebug      # install on a running emulator/device
```

On Windows use `gradlew.bat` in place of `./gradlew`.

The app runs fully offline from 15 seeded sample properties. The optional **Refresh**
action expects a mock REST API at `http://10.0.2.2:8080/properties` (`10.0.2.2` is the
Android emulator's alias for your host machine's `localhost`); a ready-made payload is
in [`api-mock/properties.json`](api-mock/properties.json).

## 📁 Package structure

```
com.example.estatefinder
├── data/local        Room entities, DAOs, database + migrations & seeding
├── data/remote       Retrofit service, DTOs, mappers, remote data source
├── data/repository   PropertyRepository (single source of truth)
├── data/SampleData   the 15 seeded sample properties
├── model             Property domain model
├── viewmodel         PropertyViewModel (MVVM)
└── ui                splash · home · property list · details · favorites · map
```

## 📱 App flow

```
Splash → Home (featured + search)
              → Property List (filter + sort)
                    → Details (specs · favorite · share · seller contact · map)
              → Favorites
```
