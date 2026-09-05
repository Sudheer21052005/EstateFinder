# PHASE 4 – RETROFIT + REST API (READ‑ONLY PROPOSAL)

---

## 1. Concrete REST API Source  

**Chosen option – Local static‑JSON HTTP server (Python `http.server` or Node `http-server`) served from the development machine.**  

| Item | Detail |
|------|--------|
| **Base URL (emulator)** | `http://10.0.2.2:8080`  (the emulator maps `10.0.2.2` → host‑machine localhost) |
| **Endpoint** | `GET /properties`  → returns the full property list as JSON |
| **How the data is created** | A single `properties.json` file (checked‑in under `api‑mock/`) that mirrors the current 15‑item `SampleData`. The file is generated once from `SampleData` (a one‑off script) and then committed. |
| **How the Pixel 8 emulator reaches it** | The emulator runs on the same host; `10.0.2.2` is the special alias for the host’s loopback. Starting the mock server with `python -m http.server 8080 --directory api-mock` makes `http://10.0.2.2:8080/properties` reachable from the emulator without any extra network configuration. |
| **Internet required?** | **No** – the mock server runs locally. The app works completely offline (Room cache) if the server is not started. |
| **Examiner can run / view it** | 1. Open a terminal in the project root. <br>2. `cd api-mock && python -m http.server 8080`  (or `npx http-server -p 8080`). <br>3. Open a browser → `http://localhost:8080/properties` to see the exact JSON the app will consume. |
| **If the API is unavailable** | The app continues to show the Room‑cached data; a toast “Working offline – showing cached listings” is displayed. No crash, no empty list. |
| **Suitability for final college demo** | Fully self‑contained, deterministic, no external dependency, works on the supplied emulator, and demonstrates the complete offline‑first + explicit‑refresh flow. |

---

## 2. JSON CONTRACT (exact schema)

```json
[
  {
    "id": 1,
    "title": "Modern 2 BHK Apartment",
    "description": "Bright east‑facing apartment in the heart of Andheri West, walking distance from the metro. Gated society with gym, kids play area and covered parking.",
    "price": 7500000,
    "location": "Andheri West, Mumbai",
    "propertyType": "Apartment",
    "listingType": "Sale",
    "bedrooms": 2,
    "bathrooms": 2,
    "area": 950,
    "latitude": 19.1362,
    "longitude": 72.8296,
    "imageUrl": "https://example.com/img_apartment_1.jpg",
    "featured": true
  },
  …
]
```

*Fields correspond 1‑to‑1 with `Property.java`.*  
**No Android‑specific `imageRes`** – only `imageUrl` (optional HTTPS URL). `featured` mirrors the “Sale = featured” rule.

---

## 3. IMAGE ARCHITECTURE  

| Field | Location | Purpose |
|-------|----------|---------|
| `imageRes` (int) | `Property` / `PropertyEntity` | Local vector drawable resource id – **unchanged**, stays the reliable fallback. |
| `imageUrl` (String) | `Property` / `PropertyEntity` | Remote HTTPS URL supplied by the API (may be null). |

**Behaviour in UI (`PropertyAdapter`, `PropertyDetailsActivity`)**

```java
if (property.getImageUrl() != null && !property.getImageUrl().isEmpty()) {
    Glide.with(img.getContext())
         .load(property.getImageUrl())
         .placeholder(R.drawable.img_placeholder)
         .error(property.getImageRes())
         .into(img);
} else {
    img.setImageResource(property.getImageRes());
}
```

Local drawables are never removed.

---

## 4. RETROFIT ENDPOINTS  

```java
// ApiService.java
public interface ApiService {
    @GET("properties")
    Call<List<PropertyResponse>> getAllProperties();
}
```

*Only `GET /properties` is needed.*  
`GET /properties/{id}` is **not required** – the Details screen already has the full object via Room.

Search / filter remain **local** (Room `searchLive`).

---

## 5. DATA FLOW  

```
REST API (static JSON)
      ↓  Retrofit (Gson)
PropertyResponse (DTO)
      ↓  PropertyMapper.toDomain()
Property (domain model)
      ↓  PropertyMapper.toEntity()
PropertyEntity
      ↓  PropertyDao.insertAll()  (upsert)
Room Database
      ↓  LiveData<List<Property>>
PropertyRepository
      ↓  LiveData
PropertyViewModel
      ↓  LiveData
UI (MainActivity, PropertyListActivity, …)
```

Room stays the **runtime source of truth**; UI never touches Retrofit.

---

## 6. REPOSITORY ARCHITECTURE  

```
PropertyRepository
 ├─ PropertyDao          (Room)
 ├─ FavoriteDao          (Room)
 └─ RemoteDataSource     (internal, thin wrapper around ApiService)
```

* `RemoteDataSource` is **not** a second repository – it is a private data‑layer component used only by `PropertyRepository.refreshFromNetwork()`.  
* `PropertyViewModel` continues to call **only** `PropertyRepository`.

---

## 7. SYNC STRATEGY – CACHE‑FIRST + EXPLICIT REFRESH  

| Moment | Action |
|--------|--------|
| **App start** | Room already seeded → UI shows data instantly (no network). |
| **User taps Refresh** (toolbar icon / menu item) | `PropertyViewModel.refresh()` → `PropertyRepository.refreshFromNetwork()` → `RemoteDataSource.fetchAll()` → map → `PropertyDao.insertAll()` (ON CONFLICT REPLACE) → Room LiveData notifies UI. |
| **Network fails** | Log warning, keep existing Room data, toast “Refresh failed – showing cached data”. |

No background scheduler, TTL, pagination, or `WorkManager`.

---

## 8. REFRESH UI  

*Add a single **refresh menu item** (icon `ic_refresh`) to the toolbar of `MainActivity` and `PropertyListActivity`.*  
Click → `viewModel.refresh()`. No `SwipeRefreshLayout` (not present in current UI).

---

## 9. ERROR HANDLING  

| Condition | Handling |
|-----------|----------|
| No internet / DNS / timeout | Toast “Working offline”, keep Room data. |
| HTTP 4xx / 5xx | Log, keep Room data, optional toast. |
| Malformed JSON (`JsonSyntaxException`) | Log, keep Room data. |
| Empty response (`[]`) | Treat as “no new data”, keep Room data. |
| Any exception in mapping/upsert | Log, keep Room data. |

**Never** clear the Room cache because a network request failed.

---

## 10. FAVORITES  

*Remain completely local.*  
* Remote sync **never touches** the `favorites` table.  
* `FavoriteDao` unchanged.

---

## 11. TESTING PLAN  

| Test | Tool |
|------|------|
| JSON → `PropertyResponse` parsing | JUnit + Gson |
| `PropertyMapper` (DTO → Domain → Entity) | JUnit |
| Retrofit success (200 + JSON) | `MockWebServer` (enqueue 200) |
| Retrofit HTTP error (500) | `MockWebServer` (enqueue 500) |
| Room upsert after remote fetch | In‑memory Room DB (`Room.inMemoryDatabaseBuilder`) |
| Offline fallback (no network) | Disable Wi‑Fi in emulator, verify list still shows |
| Glide remote load + fallback | Instrumented test with `MockWebServer` serving an image |
| Favorites unchanged after sync | Verify `FavoriteDao` rows untouched |

Only **MockWebServer** and **Room in‑memory DB** are needed – both compatible with current Gradle.

---

## 12. EXACT IMPLEMENTATION FILES  

| New files | Description |
|-----------|-------------|
| `app/src/main/java/com/example/estatefinder/data/remote/ApiService.java` | Retrofit interface |
| `app/src/main/java/com/example/estatefinder/data/remote/dto/PropertyResponse.java` | DTO matching JSON |
| `app/src/main/java/com/example/estatefinder/data/remote/mapper/PropertyMapper.java` | `toDomain`, `toEntity` |
| `app/src/main/java/com/example/estatefinder/data/remote/RemoteDataSource.java` | thin wrapper calling `ApiService` |
| `app/src/androidTest/…/RemoteDataSourceTest.java` (optional) | MockWebServer tests |
| `app/src/debug/AndroidManifest.xml` | `android:usesCleartextTraffic="true"` (only for local HTTP) |

| Modified files | Change |
|----------------|--------|
| `Property.java` | add `String imageUrl` getter/setter (if not present) |
| `PropertyEntity.java` | add `String imageUrl` column (nullable) |
| `PropertyRepository.java` | add `refreshFromNetwork()`; uses `RemoteDataSource`; upserts Room |
| `PropertyViewModel.java` | expose `refresh()` → calls repo; UI binds to menu item |
| `PropertyAdapter.java` / `PropertyDetailsActivity.java` | Glide load `imageUrl` with `imageRes` fallback |
| `MainActivity` / `PropertyListActivity` menu XML | add refresh menu item (`ic_refresh`) |
| `app/build.gradle` (optional) | ensure `androidTestImplementation "androidx.test:rules:1.5.0"` etc. for MockWebServer (optional). |

---

## 13. ROOM MIGRATION  

*Current DB version = 1.*  
`PropertyEntity` **does not yet contain `imageUrl`**. Adding a non‑null column would require a migration.  

**Solution:** add `imageUrl` as **nullable** (`@ColumnInfo(name = "image_url") String imageUrl;`).  
* No data loss – existing rows get `null`.  
* Increment DB version to **2** and provide a **no‑op Migration** (since only a nullable column is added).  

```java
static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override public void migrate(SupportSQLiteDatabase db) {
        db.execSQL("ALTER TABLE properties ADD COLUMN image_url TEXT");
    }
};
```

Add `.addMigrations(MIGRATION_1_2)` when building `RoomDatabase`.

---

## 14. VIVA EXPLANATION (ready to recite)

> “The app uses **Retrofit** to fetch a JSON catalogue from a local mock REST API. The JSON is parsed into **DTOs**, mapped to our **domain `Property` objects**, then written into the **Room** database. The **UI never calls Retrofit** – it only observes **LiveData** coming from Room via the **ViewModel**. This gives an **offline‑first** experience: the user always sees cached data instantly, and a manual *Refresh* silently updates the local database. Favorites stay in Room and are never sent to the server.”

---

## 15. STEP‑BY‑STEP IMPLEMENTATION ORDER  

1. **Add `imageUrl` field** to `Property` and `PropertyEntity` (nullable). Create Room migration 1→2.  
2. **Create DTO & Mapper** (`PropertyResponse`, `PropertyMapper`).  
3. **Create `ApiService`** (Retrofit) and **`RemoteDataSource`**.  
4. **Extend `PropertyRepository`** with `refreshFromNetwork()` → calls `RemoteDataSource`, maps, upserts Room.  
5. **Add `refresh()`** to `PropertyViewModel`; expose to UI.  
6. **Add refresh menu item** (`ic_refresh`) to `MainActivity` & `PropertyListActivity`.  
7. **Update `PropertyAdapter` / `PropertyDetailsActivity`** to load `imageUrl` via Glide with `imageRes` fallback.  
8. **Add `src/debug/AndroidManifest.xml`** with `usesCleartextTraffic="true"`.  
9. **Write unit / instrumented tests** (MockWebServer + in‑memory Room).  
10. **Run clean build**, install on Pixel 8, verify:  
   * cold start → Room data visible immediately,  
   * tap Refresh → network fetch → UI updates,  
   * kill network → app still works,  
   * favorites persist.  

---

## 16. RISKS & MITIGATIONS  

| Risk | Mitigation |
|------|------------|
| Emulator cannot reach `10.0.2.2` (firewall) | Ensure host firewall allows inbound on port 8080; test with `curl http://10.0.2.2:8080/properties` from emulator shell. |
| Clear‑text traffic leaks in release | `usesCleartextTraffic` only in `src/debug/AndroidManifest.xml`. |
| Schema change forgets migration | Migration code added; test by installing v1 APK, then v2 APK without uninstall. |
| Glide version clash | Already declared; verify no duplicate `annotationProcessor`. |
| Test flakiness on emulator network | All network tests run against `MockWebServer` (no real network). |

---

*No code has been written yet. Await **explicit approval** before starting Phase 4 implementation.*