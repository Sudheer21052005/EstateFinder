package com.example.estatefinder.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(FavoriteEntity favorite);

    @Delete
    void delete(FavoriteEntity favorite);

    @Query("SELECT propertyId FROM favorites")
    LiveData<List<Long>> getAllFavoriteIds();

    @Query("SELECT propertyId FROM favorites WHERE propertyId = :propertyId")
    LiveData<List<Long>> getFavoriteId(long propertyId);
}