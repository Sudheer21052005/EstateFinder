package com.example.estatefinder.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class PropertyResponse {
    public long id;
    public String title;
    public String description;
    public double price;
    public String location;
    @SerializedName("propertyType")
    public String propertyType;
    @SerializedName("listingType")
    public String listingType;
    public int bedrooms;
    public int bathrooms;
    public int area;
    public double latitude;
    public double longitude;
    @SerializedName("imageUrl")
    public String imageUrl;
    public boolean featured;
    public String sellerName;
    public String sellerPhone;
    public String sellerEmail;
}