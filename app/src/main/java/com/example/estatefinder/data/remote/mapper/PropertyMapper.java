package com.example.estatefinder.data.remote.mapper;

import com.example.estatefinder.data.local.PropertyEntity;
import com.example.estatefinder.data.remote.dto.PropertyResponse;
import com.example.estatefinder.model.Property;

public class PropertyMapper {

    public static Property toDomain(PropertyResponse dto) {
        Property p = new Property(
                dto.id,
                dto.title,
                dto.description,
                dto.price,
                dto.location,
                dto.propertyType,
                dto.listingType,
                dto.bedrooms,
                dto.bathrooms,
                dto.area,
                0, // imageRes will be set via fallback; not from API
                dto.latitude,
                dto.longitude
        );
        p.setImageUrl(dto.imageUrl);
        p.setFeatured(dto.featured);
        p.setSellerName(dto.sellerName);
        p.setSellerPhone(dto.sellerPhone);
        p.setSellerEmail(dto.sellerEmail);
        return p;
    }

    public static PropertyEntity toEntity(Property domain) {
        PropertyEntity e = new PropertyEntity(
                domain.getId(),
                domain.getTitle(),
                domain.getDescription(),
                domain.getPrice(),
                domain.getLocation(),
                domain.getPropertyType(),
                domain.getListingType(),
                domain.getBedrooms(),
                domain.getBathrooms(),
                domain.getArea(),
                domain.getImageRes(),
                domain.getLatitude(),
                domain.getLongitude()
        );
        e.imageUrl = domain.getImageUrl();
        e.featured = domain.isFeatured();
        e.sellerName = domain.getSellerName();
        e.sellerPhone = domain.getSellerPhone();
        e.sellerEmail = domain.getSellerEmail();
        return e;
    }
}