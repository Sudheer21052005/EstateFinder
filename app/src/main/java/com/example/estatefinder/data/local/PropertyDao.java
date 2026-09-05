package com.example.estatefinder.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PropertyDao {

    @Query("SELECT * FROM properties")
    LiveData<List<PropertyEntity>> getAll();

    @Query("SELECT * FROM properties")
    List<PropertyEntity> getAllSync();

    @Query("SELECT * FROM properties WHERE featured = 1")
    LiveData<List<PropertyEntity>> getFeatured();

    @Query("SELECT * FROM properties WHERE featured = 1")
    List<PropertyEntity> getFeaturedSync();

    @Query("SELECT * FROM properties WHERE id = :id")
    LiveData<PropertyEntity> getById(long id);

    @Query("SELECT * FROM properties WHERE id = :id")
    PropertyEntity getByIdSync(long id);

    @Query("SELECT * FROM properties WHERE (title LIKE :query OR location LIKE :query) " +
            "AND (listingType = :listingType OR :listingType = 'All') " +
            "AND (propertyType = :propertyType OR :propertyType = 'All')")
    LiveData<List<PropertyEntity>> search(String query, String listingType, String propertyType);

    @Query("SELECT * FROM properties WHERE (title LIKE :query OR location LIKE :query) " +
            "AND (listingType = :listingType OR :listingType = 'All') " +
            "AND (propertyType = :propertyType OR :propertyType = 'All')")
    List<PropertyEntity> searchSync(String query, String listingType, String propertyType);

    // ----- Sort variants: same filter WHERE, static ORDER BY -----
    // Room cannot bind a column name (ORDER BY :param), and @RawQuery is avoided for stability,
    // so each sort order is its own static @Query. Only the trailing ORDER BY differs.
    // "Newest" = id DESC: ids are assigned as seed insertion order (autoGenerate = false, seeded 1..15),
    // so highest id == most-recently-added first. (§16: newest defined as insertion order.)
    @Query("SELECT * FROM properties WHERE (title LIKE :query OR location LIKE :query) " +
            "AND (listingType = :listingType OR :listingType = 'All') " +
            "AND (propertyType = :propertyType OR :propertyType = 'All') ORDER BY id DESC")
    LiveData<List<PropertyEntity>> searchNewest(String query, String listingType, String propertyType);

    @Query("SELECT * FROM properties WHERE (title LIKE :query OR location LIKE :query) " +
            "AND (listingType = :listingType OR :listingType = 'All') " +
            "AND (propertyType = :propertyType OR :propertyType = 'All') ORDER BY price ASC")
    LiveData<List<PropertyEntity>> searchPriceAsc(String query, String listingType, String propertyType);

    @Query("SELECT * FROM properties WHERE (title LIKE :query OR location LIKE :query) " +
            "AND (listingType = :listingType OR :listingType = 'All') " +
            "AND (propertyType = :propertyType OR :propertyType = 'All') ORDER BY price DESC")
    LiveData<List<PropertyEntity>> searchPriceDesc(String query, String listingType, String propertyType);

    @Query("SELECT * FROM properties WHERE (title LIKE :query OR location LIKE :query) " +
            "AND (listingType = :listingType OR :listingType = 'All') " +
            "AND (propertyType = :propertyType OR :propertyType = 'All') ORDER BY area ASC")
    LiveData<List<PropertyEntity>> searchAreaAsc(String query, String listingType, String propertyType);

    @Query("SELECT * FROM properties WHERE (title LIKE :query OR location LIKE :query) " +
            "AND (listingType = :listingType OR :listingType = 'All') " +
            "AND (propertyType = :propertyType OR :propertyType = 'All') ORDER BY area DESC")
    LiveData<List<PropertyEntity>> searchAreaDesc(String query, String listingType, String propertyType);

    /** Reactive favorites: JOIN properties with the favorites table (no per-id lookups). */
    @Query("SELECT p.* FROM properties p " +
            "INNER JOIN favorites f ON p.id = f.propertyId")
    LiveData<List<PropertyEntity>> getFavoriteProperties();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PropertyEntity> entities);
}