package com.example.estatefinder.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room entity for the properties table.
 * Mirrors the domain {@link com.example.estatefinder.model.Property} but keeps persistence concerns separate.
 */
@Entity(tableName = "properties")
public class PropertyEntity {

    @PrimaryKey(autoGenerate = false)
    @NonNull
    public long id;

    public String title;
    public String description;
    public double price;
    public String location;
    public String propertyType; // Apartment / House / Villa / Office
    public String listingType;  // Sale / Rent
    public int bedrooms;
    public int bathrooms;
    public int area;            // sq.ft.
    public int imageRes;        // drawable resource id
    public double latitude;
    public double longitude;
    @ColumnInfo(name = "image_url")
    public String imageUrl;
    // Final phase: real featured flag. Java primitive boolean -> INTEGER NOT NULL;
    // defaultValue="0" makes the fresh CREATE TABLE match the MIGRATION_2_3 ADD COLUMN ... DEFAULT 0.
    @ColumnInfo(defaultValue = "0")
    public boolean featured;
    // Final phase: seller / agent contact info (nullable TEXT). No Seller table/FK/auth.
    public String sellerName;
    public String sellerPhone;
    public String sellerEmail;

    // No-arg constructor for Room
    public PropertyEntity() {}

    @Ignore
    public PropertyEntity(long id, String title, String description, double price, String location,
                          String propertyType, String listingType, int bedrooms, int bathrooms,
                          int area, int imageRes, double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.location = location;
        this.propertyType = propertyType;
        this.listingType = listingType;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.area = area;
        this.imageRes = imageRes;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}