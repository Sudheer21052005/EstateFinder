package com.example.estatefinder.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.estatefinder.data.SampleData;
import com.example.estatefinder.data.local.EstateDatabase;
import com.example.estatefinder.data.local.FavoriteEntity;
import com.example.estatefinder.data.local.PropertyDao;
import com.example.estatefinder.data.local.PropertyEntity;
import com.example.estatefinder.data.remote.RemoteDataSource;
import com.example.estatefinder.data.remote.dto.PropertyResponse;
import com.example.estatefinder.data.remote.mapper.PropertyMapper;
import com.example.estatefinder.model.Property;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source of truth for property data.
 * Phase 3: reads properties from Room via LiveData, favorites persisted in Room.
 */
public class PropertyRepository {

    /** Sort options for the property list. NEWEST maps to id DESC (id = seed insertion order). */
    public enum SortOrder { NEWEST, PRICE_ASC, PRICE_DESC, AREA_ASC, AREA_DESC }

    private static volatile PropertyRepository instance;

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new PropertyRepository(context.getApplicationContext());
        }
    }

    public static PropertyRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PropertyRepository not initialized. Call init(context) first.");
        }
        return instance;
    }

    private final EstateDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final RemoteDataSource remoteDataSource = new RemoteDataSource();

    // LiveData for favorite IDs (reactive)
    private final LiveData<Set<Long>> favoriteIdsLiveData;

    private PropertyRepository(Context context) {
        this.database = EstateDatabase.getInstance(context);

        // Transform FavoriteDao LiveData<List<Long>> to LiveData<Set<Long>>
        favoriteIdsLiveData = Transformations.map(database.favoriteDao().getAllFavoriteIds(),
                list -> {
                    Set<Long> set = new HashSet<>();
                    if (list != null) {
                        set.addAll(list);
                    }
                    return set;
                });
    }

    /** Refresh property list from remote API and upsert into Room. */
    public void refreshFromNetwork() {
        executor.execute(() -> {
            try {
                List<PropertyResponse> remote = remoteDataSource.fetchAllProperties();
                if (remote == null || remote.isEmpty()) {
                    android.util.Log.w("PropertyRepository", "Remote fetch returned empty list");
                    return;
                }
                List<PropertyEntity> entities = new ArrayList<>(remote.size());
                for (PropertyResponse dto : remote) {
                    Property domain = PropertyMapper.toDomain(dto);
                    entities.add(PropertyMapper.toEntity(domain));
                }
                database.propertyDao().insertAll(entities);
                android.util.Log.i("PropertyRepository", "Remote sync completed, " + entities.size() + " properties upserted");
            } catch (IOException e) {
                android.util.Log.e("PropertyRepository", "Network error during refresh", e);
            } catch (Exception e) {
                android.util.Log.e("PropertyRepository", "Unexpected error during refresh", e);
            }
        });
    }

    // ----- Mapping -----
    private Property mapToDomain(PropertyEntity e) {
        Property p = new Property(
                e.id,
                e.title,
                e.description,
                e.price,
                e.location,
                e.propertyType,
                e.listingType,
                e.bedrooms,
                e.bathrooms,
                e.area,
                e.imageRes,
                e.latitude,
                e.longitude
        );
        p.setImageUrl(e.imageUrl);
        p.setFeatured(e.featured);
        p.setSellerName(e.sellerName);
        p.setSellerPhone(e.sellerPhone);
        p.setSellerEmail(e.sellerEmail);
        return p;
    }

    private PropertyEntity mapToEntity(Property p) {
        PropertyEntity e = new PropertyEntity(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getPrice(),
                p.getLocation(),
                p.getPropertyType(),
                p.getListingType(),
                p.getBedrooms(),
                p.getBathrooms(),
                p.getArea(),
                p.getImageRes(),
                p.getLatitude(),
                p.getLongitude()
        );
        e.imageUrl = p.getImageUrl();
        e.featured = p.isFeatured();
        e.sellerName = p.getSellerName();
        e.sellerPhone = p.getSellerPhone();
        e.sellerEmail = p.getSellerEmail();
        return e;
    }

    // Helper to transform LiveData<List<PropertyEntity>> -> LiveData<List<Property>>
    private LiveData<List<Property>> mapEntities(LiveData<List<PropertyEntity>> source) {
        return Transformations.map(source, entities -> {
            List<Property> result = new ArrayList<>();
            if (entities != null) {
                for (PropertyEntity e : entities) {
                    result.add(mapToDomain(e));
                }
            }
            return result;
        });
    }

    // ----- Public API (LiveData) -----
    public LiveData<List<Property>> getAllLive() {
        return mapEntities(database.propertyDao().getAll());
    }

    public LiveData<List<Property>> getFeaturedLive() {
        return mapEntities(database.propertyDao().getFeatured());
    }

    public LiveData<List<Property>> searchLive(String query, String listingType, String propertyType, SortOrder sort) {
        String q = query == null ? "" : "%" + query.trim().toLowerCase() + "%";
        String lt = listingType == null ? "All" : listingType;
        String pt = propertyType == null ? "All" : propertyType;
        PropertyDao dao = database.propertyDao();
        if (sort == null) sort = SortOrder.NEWEST;
        switch (sort) {
            case PRICE_ASC:  return mapEntities(dao.searchPriceAsc(q, lt, pt));
            case PRICE_DESC: return mapEntities(dao.searchPriceDesc(q, lt, pt));
            case AREA_ASC:   return mapEntities(dao.searchAreaAsc(q, lt, pt));
            case AREA_DESC:  return mapEntities(dao.searchAreaDesc(q, lt, pt));
            case NEWEST:
            default:         return mapEntities(dao.searchNewest(q, lt, pt));
        }
    }

    /**
     * Reactive single-property lookup for the details / map screens.
     * Runs the query on Room's background executor, so callers on the UI thread
     * never hit the "Cannot access database on the main thread" crash.
     */
    public LiveData<Property> getPropertyLive(long id) {
        return Transformations.map(database.propertyDao().getById(id),
                e -> e != null ? mapToDomain(e) : null);
    }

    /** Reactive favorites list via a Room JOIN (replaces synchronous per-id lookups). */
    public LiveData<List<Property>> getFavoritePropertiesLive() {
        return mapEntities(database.propertyDao().getFavoriteProperties());
    }

    // Synchronous single-item lookup (used by Details and Favorites)
    public Property getById(long id) {
        PropertyEntity e = database.propertyDao().getByIdSync(id);
        return e != null ? mapToDomain(e) : null;
    }

    // ----- Favorites (Room-backed) -----
    public List<Property> getFavoriteProperties() {
        Set<Long> ids = favoriteIdsLiveData.getValue();
        if (ids == null) return new ArrayList<>();
        List<Property> favs = new ArrayList<>();
        for (Long fid : ids) {
            Property p = getById(fid);
            if (p != null) favs.add(p);
        }
        return favs;
    }

    public boolean isFavorite(long id) {
        Set<Long> ids = favoriteIdsLiveData.getValue();
        return ids != null && ids.contains(id);
    }

    /** @return true if the property is now a favorite. */
    public boolean toggleFavorite(long id) {
        boolean currently = isFavorite(id);
        if (currently) {
            executor.submit(() -> database.favoriteDao().delete(new FavoriteEntity(id)));
        } else {
            executor.submit(() -> database.favoriteDao().insert(new FavoriteEntity(id)));
        }
        return !currently;
    }

    /** Expose favorite IDs LiveData for reactive UI */
    public LiveData<Set<Long>> getFavoriteIdsLiveData() {
        return favoriteIdsLiveData;
    }

    // Cleanup
    public void shutdown() {
        executor.shutdown();
    }
}
