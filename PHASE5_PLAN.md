# PHASE 5 – GOOGLE MAPS + GPS (READ‑ONLY REVISED PLAN)

---

## 1. Architecture  

```
UI (PropertyDetailsActivity)
      ↓  "View on Map"
MapActivity
      ↓  Intent extra: property_id
PropertyViewModel → PropertyRepository → Room (PropertyDao)
      ↓  Property (latitude, longitude)
GoogleMap
      ↓  Marker (title = property title, position = lat/long)
      ↓  Camera centered on property
```

*Room stays the single source of truth. No new tables or schema changes.*  

---

## 2. Dependencies  

| Dependency | Version (match current `build.gradle`) | Why |
|------------|----------------------------------------|-----|
| `com.google.android.gms:play-services-maps` | **19.0.0** (already declared) | Google Maps SDK |
| `com.google.android.gms:play-services-location` | **21.3.0** (already declared) | Fused Location Provider |
| **No `google-services` Gradle plugin** | – | Not needed for Maps only |

> The `google-services` plugin is **not** added unless another feature (e.g., Firebase) later requires it.

---

## 3. Emulator / Device Requirements  

| Requirement | Check |
|-------------|-------|
| System image | **Google APIs** (or **Google Play**) system image for API 34 – includes Google Play Services. |
| Current AVD (Pixel 8) | Verify it is a **Google APIs** image (`system-images;android-34;google_apis;x86_64`). If it is a plain “Android” image, create a new AVD with Google APIs. |
| Google Play Services version | Must be ≥ 21.3.0 (matches `play-services-location`). |

*If the current AVD lacks Google APIs, create a new AVD before implementation; do **not** replace the existing one without confirmation.*

---

## 4. API‑Key Strategy  

| Step | Detail |
|------|--------|
| **Storage** | `local.properties` → `MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY` |
| **Gradle** | `manifestPlaceholders = [MAPS_API_KEY: localProperties.getProperty('MAPS_API_KEY','')]` (already present). |
| **Manifest** | `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}" />` (already in `AndroidManifest.xml`). |
| **Git** | `local.properties` is already in `.gitignore`. No key is committed. |
| **Debug vs Release** | Same placeholder; the real key is only in the developer’s local `local.properties`. No separate debug/release keys required unless the project later uses restricted keys. |

> No hard‑coded key in Java/Kotlin source. Placeholder `YOUR_GOOGLE_MAPS_API_KEY` used in documentation.

---

## 5. MapActivity Design  

| Item | Choice |
|------|--------|
| **Base class** | `AppCompatActivity` |
| **Map container** | `SupportMapFragment` (simpler, handles lifecycle). |
| **Layout** | `activity_map.xml` – `<fragment class="com.google.android.gms.maps.SupportMapFragment" …/>`. |
| **Intent extra** | `EXTRA_PROPERTY_ID` (same constant used by `MapPlaceholderActivity`). |
| **Toolbar** | `MaterialToolbar` with back navigation (`setNavigationOnClickListener`). |
| **Lifecycle** | `onMapReady(GoogleMap)` → fetch property → add marker → move camera. |
| **Error handling** | `try / catch` around `getMapAsync`; on failure start `MapPlaceholderActivity` with same intent. |

---

## 6. Property Marker Flow  

1. `MapActivity.onCreate` reads `propertyId` from intent.  
2. `PropertyViewModel.getProperty(id)` (synchronous `getById`) returns `Property`.  
3. Extract `latitude`, `longitude`, `title`.  
4. `googleMap.addMarker(new MarkerOptions().position(latLng).title(title))`.  
5. `googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))`.  

*Only the selected property is shown – no clustering, routes, or extra markers.*

---

## 7. User‑Location Flow (Optional, separate)  

| Trigger | Steps |
|---------|-------|
| User taps **“My Location”** button (floating action button or menu item) | 1. Check `ContextCompat.checkSelfPermission(ACCESS_FINE_LOCATION)`. <br>2. If not granted → `ActivityCompat.requestPermissions` with `ACCESS_FINE_LOCATION` (and `ACCESS_COARSE_LOCATION` for older APIs). |
| Permission granted | `googleMap.setMyLocationEnabled(true)`; optionally `googleMap.moveCamera` to current location via `FusedLocationProviderClient.getLastLocation()`. |
| Permission denied (first time) | Show Snackbar/Toast: “Location permission needed for My‑Location feature”. |
| Permission permanently denied | Show dialog with “Open Settings” button linking to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`. |

*Property marker **never** requests location permission.*

---

## 8. Permission Strategy  

| Permission | Declaration | Runtime request |
|------------|-------------|-----------------|
| `ACCESS_FINE_LOCATION` | `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />` | Requested **only** when user taps “My Location”. |
| `ACCESS_COARSE_LOCATION` | Same as above (fallback for API < 29). | Same request. |
| `ACCESS_BACKGROUND_LOCATION` | **Not declared**. | Never requested. |

*Property marker **never** requests location permission.*

---

## 9. Fallback Strategy  

*If any of the following occurs:*  

* Google Play Services unavailable / outdated  
* Maps SDK initialization throws `GooglePlayServicesNotAvailableException` / `SecurityException`  
* API key invalid / quota exceeded  

**Behaviour:**  

1. Catch the exception in `MapActivity.onCreate` / `onMapReady`.  
2. Start `MapPlaceholderActivity` with the same `property_id` intent.  
3. `MapPlaceholderActivity` already shows property name, address, lat/long and the note “Google Maps and GPS arrive in Phase 5”.  

*App never crashes; user still sees property details.*

---

## 10. Navigation  

| From | To | Mechanism |
|------|----|-----------|
| `PropertyDetailsActivity` → “View on Map” | `MapActivity` | `Intent.putExtra(EXTRA_PROPERTY_ID, id)` |
| `MapActivity` → back | `PropertyDetailsActivity` | Toolbar back button (`finish()`) |
| Failure path | `MapPlaceholderActivity` | Same intent extras, started from `MapActivity` catch block. |

*No new back‑stack anomalies; toolbar back works as in other screens.*

---

## 11. Testing Plan  

| Test | Type | Description |
|------|------|-------------|
| MapActivity launches | **Physical** | Launch via Details → “View on Map”. |
| Correct `property_id` received | **Code inspection** | Intent extra read. |
| Property loaded from Room | **Code inspection** | `viewModel.getProperty(id)` uses `PropertyDao.getByIdSync`. |
| Latitude/longitude used for marker | **Physical** (once map works) | Marker appears at expected coordinates. |
| Camera centers on property | **Physical** | Camera zoom/position. |
| Map failure → fallback | **Physical** (disable Play Services or use bad API key) | `MapPlaceholderActivity` shown. |
| Permission not requested for property marker | **Code inspection** | No `requestPermissions` before marker added. |
| Location permission request works | **Physical** (tap My‑Location) | Runtime dialog appears. |
| Permission granted → current location shown | **Physical** | Blue dot + camera move. |
| Permission denied → graceful message | **Physical** | Snackbar / dialog, map still usable. |
| Back navigation works | **Physical** | Toolbar back returns to Details. |
| No crash when Play Services missing | **Physical** (emulator without Play Services) | Fallback shown. |

*Only tests that can be run on the current emulator are marked **Physical**; the rest are **Code inspection**.*

---

## 12. Exact Files to Create / Modify  

| New Files | Purpose |
|-----------|---------|
| `app/src/main/java/com/example/estatefinder/ui/map/MapActivity.java` | Main map screen. |
| `app/src/main/res/layout/activity_map.xml` | Layout with `SupportMapFragment` + toolbar + optional FAB. |
| `app/src/main/res/menu/menu_map.xml` (optional) | “My Location” menu item. |
| `app/src/main/res/drawable/ic_my_location.xml` (optional) | Icon for FAB/menu. |

| Modified Files | Change |
|----------------|--------|
| `AndroidManifest.xml` | Add `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />` and `<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />`. Ensure `<meta-data android:name="com.google.android.geo.API_KEY" …>` already present. |
| `PropertyDetailsActivity.java` | No change (already launches map via intent). |
| `MapPlaceholderActivity.java` | No change (fallback target). |
| `app/build.gradle` | No new dependencies (maps & location already declared). |
| `local.properties` (developer machine) | Add `MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY`. |
| `.gitignore` | Already ignores `local.properties`. |

---

## 13. Risks & Mitigations  

| Risk | Mitigation |
|------|------------|
| Emulator lacks Google Play Services | Verify AVD uses **Google APIs** image; create new AVD if needed before coding. |
| API key leak | Keep key only in `local.properties` (git‑ignored). |
| Runtime permission denial loops | Follow Android best practice: request once, then show rationale, then direct to settings. |
| Maps SDK version mismatch | Use the same version already in `build.gradle` (`19.0.0`). |
| Migration of Room not required | Latitude/longitude already in `PropertyEntity`; confirm by inspecting `PropertyEntity.java`. |
| Clear‑text traffic for map tiles | Map tiles served over HTTPS by Google; no clear‑text config needed. |

---

## 14. Viva Explanation (ready to recite)

> “Property coordinates are stored with each property. When the user selects **View on Map**, the app retrieves the selected property from Room and passes its ID to the map screen. The map screen loads the coordinates and places a marker at that location. User location is a separate optional feature requiring runtime permission through Android’s Fused Location Provider. If Google Maps cannot be displayed, the app falls back to the existing placeholder screen.”

---

## 15. Step‑by‑Step Implementation Order  

1. **Verify AVD** – ensure a Google APIs system image is used.  
2. **Add permissions** to `AndroidManifest.xml`.  
3. **Create `activity_map.xml`** with `SupportMapFragment`, toolbar, optional FAB.  
4. **Implement `MapActivity.java`**  
   * `onCreate` → read `property_id`.  
   * `getMapAsync` → on ready: fetch property via ViewModel → add marker → move camera.  
   * Error handling → start `MapPlaceholderActivity`.  
5. **Add “My Location” FAB / menu** → request `ACCESS_FINE_LOCATION` → enable `setMyLocationEnabled`.  
6. **Update `local.properties`** with `MAPS_API_KEY` (developer adds real key).  
7. **Test on emulator** – cold start → Details → View on Map → marker → back → refresh → offline fallback.  
8. **Run clean build** `.\gradlew.bat clean assembleDebug`.  
9. **Run emulator acceptance test** (see table in §11).  
10. **Document any issues** and iterate.

---

*No code has been written yet. Await explicit approval before starting Phase 5 implementation.*