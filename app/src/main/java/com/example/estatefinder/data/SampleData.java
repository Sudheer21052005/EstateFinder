package com.example.estatefinder.data;

import com.example.estatefinder.R;
import com.example.estatefinder.model.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Temporary in-memory sample data (Phase 2).
 * Phase 3 replaces this with Room; Phase 4 adds Retrofit — UI code will not change.
 * Coordinates are real Mumbai-area values so Phase 5's map demo looks believable.
 */
public final class SampleData {

    private SampleData() { /* no instances */ }

    // Unsplash URL parts. Canonical image set — kept identical in EstateDatabase.MIGRATION_2_3
    // and api-mock/properties.json.
    private static final String U = "https://images.unsplash.com/photo-";
    private static final String UQ = "?auto=format&fit=crop&w=800&q=80";

    /** Real photo URL mirroring the bundled drawable, so the CDN image and the offline fallback
     *  are the same property type. R.drawable ids are not compile-time constants -> if-chain. */
    private static String unsplashFor(int imageRes) {
        if (imageRes == R.drawable.img_apartment_1) return U + "1502672260266-1c1ef2d93688" + UQ;
        if (imageRes == R.drawable.img_apartment_2) return U + "1522708323590-d24dbb6b0267" + UQ;
        if (imageRes == R.drawable.img_house_1)     return U + "1580587771525-78b9dba3b914" + UQ;
        if (imageRes == R.drawable.img_house_2)     return U + "1600596542815-ffad4c1539a9" + UQ;
        if (imageRes == R.drawable.img_villa_1)     return U + "1613977257365-aaae5a9817ff" + UQ;
        if (imageRes == R.drawable.img_villa_2)     return U + "1582610116397-edb318620f90" + UQ;
        if (imageRes == R.drawable.img_office_1)    return U + "1497366754035-f200968a6e72" + UQ;
        if (imageRes == R.drawable.img_office_2)    return U + "1497366811353-6870744d04b2" + UQ;
        return null;
    }

    public static List<Property> createProperties() {
        List<Property> list = new ArrayList<>();
        list.add(new Property(1, "Modern 2 BHK Apartment",
                "Bright east-facing apartment in the heart of Andheri West, walking distance from the metro. Gated society with gym, kids play area and covered parking.",
                7500000, "Andheri West, Mumbai", "Apartment", "Sale", 2, 2, 950,
                R.drawable.img_apartment_1, 19.1362, 72.8296));
        list.add(new Property(2, "Sea-Facing 3 BHK Flat",
                "Spacious sea-facing flat in Bandra with a large balcony, premium fittings and 24x7 security. Close to Linking Road shopping and Carter Road promenade.",
                32500000, "Bandra West, Mumbai", "Apartment", "Sale", 3, 3, 1650,
                R.drawable.img_apartment_2, 19.0596, 72.8295));
        list.add(new Property(3, "Lakeside 3 BHK Apartment",
                "Airy apartment overlooking Powai Lake with clubhouse access, swimming pool and landscaped gardens. Ideal for families working in Hiranandani or SEEPZ.",
                21000000, "Powai, Mumbai", "Apartment", "Sale", 3, 3, 1420,
                R.drawable.img_apartment_1, 19.1176, 72.9060));
        list.add(new Property(4, "Cozy 1 BHK Rental",
                "Fully furnished 1 BHK in Malad West, 10 minutes from the station on foot. Rent includes maintenance; available for immediate move-in.",
                28000, "Malad West, Mumbai", "Apartment", "Rent", 1, 1, 540,
                R.drawable.img_apartment_2, 19.1861, 72.8481));
        list.add(new Property(5, "Sunny 2 BHK for Rent",
                "Well-maintained semi-furnished 2 BHK in Chembur with two balconies, modular kitchen and dedicated parking. Society has a jogging track and garden.",
                45000, "Chembur, Mumbai", "Apartment", "Rent", 2, 2, 880,
                R.drawable.img_apartment_1, 19.0522, 72.8996));
        list.add(new Property(6, "Charming 3 BHK Row House",
                "Independent row house in Thane with a private entrance, small front garden and terrace. Peaceful lane, yet close to Ghodbunder Road highways and malls.",
                14500000, "Thane West, Mumbai", "House", "Sale", 3, 3, 1750,
                R.drawable.img_house_1, 19.2183, 72.9781));
        list.add(new Property(7, "Heritage-Style Cottage",
                "Characterful cottage near Juhu beach with sloping roofs, wooden flooring and a courtyard. A rare standalone home in the middle of the city.",
                42000000, "Juhu, Mumbai", "House", "Sale", 4, 3, 2100,
                R.drawable.img_house_2, 19.0968, 72.8267));
        list.add(new Property(8, "Spacious Family House for Rent",
                "Gated 3 BHK independent house in Vashi with a backyard, power backup and covered parking for two cars. Pet-friendly society.",
                85000, "Vashi, Navi Mumbai", "House", "Rent", 3, 3, 1600,
                R.drawable.img_house_1, 19.0770, 72.9986));
        list.add(new Property(9, "Luxury 4 BHK Villa",
                "Contemporary villa in Goregaon East with a private pool, double-height living room, home theatre and smart-home automation inside a secure enclave.",
                89000000, "Goregaon East, Mumbai", "Villa", "Sale", 4, 5, 3400,
                R.drawable.img_villa_1, 19.1663, 72.8526));
        list.add(new Property(10, "Poolside 5 BHK Villa",
                "Premium villa in Colaba with sea glimpses from the terrace, landscaped lawn, staff quarters and four-car parking. Fully furnished and move-in ready.",
                125000000, "Colaba, Mumbai", "Villa", "Sale", 5, 5, 4600,
                R.drawable.img_villa_2, 18.9067, 72.8147));
        list.add(new Property(11, "Furnished Villa for Rent",
                "Elegant 4 BHK villa for long-lease rent in Powai with private pool, garden and video-door security. Ideal for expat or senior-management families.",
                275000, "Powai, Mumbai", "Villa", "Rent", 4, 4, 3200,
                R.drawable.img_villa_1, 19.1273, 72.9048));
        list.add(new Property(12, "Grade-A Office Floor",
                "Bare-shell office floor in Lower Parel business district with dedicated lifts, 30 workstations capacity and covered basement parking.",
                65000000, "Lower Parel, Mumbai", "Office", "Sale", 0, 2, 2600,
                R.drawable.img_office_1, 18.9960, 72.8258));
        list.add(new Property(13, "Ready Office Space",
                "Fully fitted office in Worli with 25 workstations, two meeting rooms, pantry and server space. Just pay the rent and start working.",
                120000, "Worli, Mumbai", "Office", "Rent", 0, 2, 1800,
                R.drawable.img_office_2, 19.0075, 72.8177));
        list.add(new Property(14, "Compact Studio Apartment",
                "Smart studio apartment in Vashi, Navi Mumbai, ideal for working professionals. Walking distance from the business park and Vashi station.",
                6500000, "Vashi, Navi Mumbai", "Apartment", "Sale", 1, 1, 420,
                R.drawable.img_apartment_2, 19.0662, 73.0007));
        list.add(new Property(15, "Premium 2 BHK Rental",
                "Newly renovated 2 BHK in Andheri West with premium flooring, false ceiling lighting and a fully equipped gym in the society.",
                62000, "Andheri West, Mumbai", "Apartment", "Rent", 2, 2, 920,
                R.drawable.img_apartment_1, 19.1291, 72.8340));

        // ---- Final-phase enrichment (loop keeps the 15 rows above readable) ----
        // Canonical values duplicated verbatim in EstateDatabase.MIGRATION_2_3 and api-mock/properties.json.
        Set<Long> featuredIds = new HashSet<>(Arrays.asList(2L, 7L, 10L, 11L, 15L));
        for (Property p : list) {
            long id = p.getId();

            // Featured: curated independent set (3 Sale + 2 Rent) — NOT derived from listingType.
            p.setFeatured(featuredIds.contains(id));

            // Seller triad by id % 3 (fictional agencies; .example domains never resolve).
            switch ((int) (id % 3)) {
                case 1: // ids 1,4,7,10,13
                    p.setSellerName("Raj Property Group");
                    p.setSellerPhone("+91 98200 10001");
                    p.setSellerEmail("contact@rajproperties.example");
                    break;
                case 2: // ids 2,5,8,11,14
                    p.setSellerName("Skyline Realtors");
                    p.setSellerPhone("+91 98200 20002");
                    p.setSellerEmail("hello@skylinerealtors.example");
                    break;
                default: // id % 3 == 0 -> ids 3,6,9,12,15
                    p.setSellerName("Coastline Estates");
                    p.setSellerPhone("+91 98200 30003");
                    p.setSellerEmail("sales@coastlineestates.example");
                    break;
            }

            // Real photo URL mirroring the bundled drawable (CDN photo == offline fallback type).
            p.setImageUrl(unsplashFor(p.getImageRes()));
        }

        return Collections.unmodifiableList(list);
    }
}
