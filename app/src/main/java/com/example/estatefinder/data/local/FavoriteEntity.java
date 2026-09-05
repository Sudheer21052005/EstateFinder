package com.example.estatefinder.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Simple join table storing favorite property IDs.
 */
@Entity(tableName = "favorites")
public class FavoriteEntity {

    @PrimaryKey
    @NonNull
    public long propertyId;

    public FavoriteEntity() {}

    @Ignore
    public FavoriteEntity(long propertyId) {
        this.propertyId = propertyId;
    }
}