package com.example.estatefinder.model;

/**
 * Plain data class representing one property listing.
 * All screens read properties through the repository — never hardcode data in UI code.
 */
public class Property {

    private final long id;
    private final String title;
    private final String description;
    private final double price;          // Sale: total ₹, Rent: ₹ per month
    private final String location;
    private final String propertyType;   // Apartment / House / Villa / Office
    private final String listingType;    // Sale / Rent
    private final int bedrooms;
    private final int bathrooms;
    private final int area;              // sq.ft.
    private final int imageRes;          // Phase 2: bundled drawable; Phase 4 adds remote imageUrl
    private String imageUrl;             // Phase 4: remote image URL (nullable)
    private boolean featured;            // Final phase: real featured flag (independent of listingType)
    private String sellerName;           // Final phase: seller / agent contact (nullable)
    private String sellerPhone;
    private String sellerEmail;
    private final double latitude;
    private final double longitude;

    public Property(long id, String title, String description, double price, String location,
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

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getLocation() { return location; }
    public String getPropertyType() { return propertyType; }
    public String getListingType() { return listingType; }
    public int getBedrooms() { return bedrooms; }
    public int getBathrooms() { return bathrooms; }
    public int getArea() { return area; }
    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSellerPhone() { return sellerPhone; }
    public void setSellerPhone(String sellerPhone) { this.sellerPhone = sellerPhone; }
    public String getSellerEmail() { return sellerEmail; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
