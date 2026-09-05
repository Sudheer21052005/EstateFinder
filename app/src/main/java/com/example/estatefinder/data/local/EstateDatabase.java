package com.example.estatefinder.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.estatefinder.data.SampleData;
import com.example.estatefinder.data.remote.mapper.PropertyMapper;
import com.example.estatefinder.model.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {PropertyEntity.class, FavoriteEntity.class}, version = 3, exportSchema = false)
public abstract class EstateDatabase extends RoomDatabase {

    public abstract PropertyDao propertyDao();
    public abstract FavoriteDao favoriteDao();

    private static volatile EstateDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Unsplash URL parts, shared by the migration backfill (SampleData holds its own copy for the seed path).
    private static final String U = "https://images.unsplash.com/photo-";
    private static final String UQ = "?auto=format&fit=crop&w=800&q=80";

    // Migration from version 1 to 2: add image_url column
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE properties ADD COLUMN image_url TEXT");
        }
    };

    // Migration 2 -> 3: real featured flag + nullable seller fields, then backfill to match the fresh seed.
    // featured is a primitive boolean -> INTEGER NOT NULL; SQLite requires DEFAULT on ADD COLUMN NOT NULL
    // for a non-empty table, and PropertyEntity carries @ColumnInfo(defaultValue="0") so the migrated
    // table is byte-identical to a fresh CREATE TABLE (Room validates this at open even with exportSchema=false).
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE properties ADD COLUMN featured INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE properties ADD COLUMN sellerName TEXT");
            db.execSQL("ALTER TABLE properties ADD COLUMN sellerPhone TEXT");
            db.execSQL("ALTER TABLE properties ADD COLUMN sellerEmail TEXT");

            // Featured: curated independent set (3 Sale + 2 Rent) — NOT derived from listingType.
            db.execSQL("UPDATE properties SET featured = 1 WHERE id IN (2,7,10,11,15)");

            // Seller triads (fictional; .example domains never resolve), assigned by id % 3.
            db.execSQL("UPDATE properties SET sellerName='Raj Property Group', "
                    + "sellerPhone='+91 98200 10001', sellerEmail='contact@rajproperties.example' "
                    + "WHERE id IN (1,4,7,10,13)");
            db.execSQL("UPDATE properties SET sellerName='Skyline Realtors', "
                    + "sellerPhone='+91 98200 20002', sellerEmail='hello@skylinerealtors.example' "
                    + "WHERE id IN (2,5,8,11,14)");
            db.execSQL("UPDATE properties SET sellerName='Coastline Estates', "
                    + "sellerPhone='+91 98200 30003', sellerEmail='sales@coastlineestates.example' "
                    + "WHERE id IN (3,6,9,12,15)");

            // image_url (column added in 1->2) backfilled by the 8 type groups (2 photos per type).
            db.execSQL("UPDATE properties SET image_url='" + U + "1502672260266-1c1ef2d93688" + UQ + "' WHERE id IN (1,3,5,15)");
            db.execSQL("UPDATE properties SET image_url='" + U + "1522708323590-d24dbb6b0267" + UQ + "' WHERE id IN (2,4,14)");
            db.execSQL("UPDATE properties SET image_url='" + U + "1580587771525-78b9dba3b914" + UQ + "' WHERE id IN (6,8)");
            db.execSQL("UPDATE properties SET image_url='" + U + "1600596542815-ffad4c1539a9" + UQ + "' WHERE id = 7");
            db.execSQL("UPDATE properties SET image_url='" + U + "1613977257365-aaae5a9817ff" + UQ + "' WHERE id IN (9,11)");
            db.execSQL("UPDATE properties SET image_url='" + U + "1582610116397-edb318620f90" + UQ + "' WHERE id = 10");
            db.execSQL("UPDATE properties SET image_url='" + U + "1497366754035-f200968a6e72" + UQ + "' WHERE id = 12");
            db.execSQL("UPDATE properties SET image_url='" + U + "1497366811353-6870744d04b2" + UQ + "' WHERE id = 13");
        }
    };

    public static EstateDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (EstateDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    EstateDatabase.class, "estate_database")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            // Seeding is done ONLY in onOpen (below), on databaseWriteExecutor.
                            // Seeding synchronously inside a Room onCreate callback calls back into the
                            // same RoomDatabase (insertAll -> getWritableDatabase) while the DB is still
                            // being created, which throws "IllegalStateException: Closed during
                            // initialization" on a fresh/cleared install. onOpen fires right after onCreate
                            // for a fresh DB too, so an empty table (count == 0) is still seeded — safely,
                            // off the initialization path.
                            .addCallback(new Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    // If the properties table is empty, seed it (e.g., after migration or cleared DB)
                                    databaseWriteExecutor.execute(() -> {
                                        try {
                                            long count = db.compileStatement("SELECT COUNT(*) FROM properties").simpleQueryForLong();
                                            if (count == 0) {
                                                PropertyDao dao = INSTANCE.propertyDao();
                                                List<PropertyEntity> entities = new ArrayList<>();
                                                for (Property p : SampleData.createProperties()) {
                                                    // Map via PropertyMapper so re-seeded rows carry
                                                    // imageUrl/featured/seller (seed == mapper output).
                                                    entities.add(PropertyMapper.toEntity(p));
                                                }
                                                dao.insertAll(entities);
                                                android.util.Log.i("EstateDatabase", "Seeded properties table onOpen");
                                            }
                                        } catch (Exception e) {
                                            android.util.Log.e("EstateDatabase", "Failed to seed onOpen", e);
                                        }
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}