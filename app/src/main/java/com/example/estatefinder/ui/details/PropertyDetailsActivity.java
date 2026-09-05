package com.example.estatefinder.ui.details;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.estatefinder.R;
import com.example.estatefinder.model.Property;
import com.example.estatefinder.ui.map.MapPlaceholderActivity;
import com.example.estatefinder.util.FormatUtils;
import com.example.estatefinder.viewmodel.PropertyViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * Property details screen (Phase 2): image with fade-in, full specs,
 * favorite toggle and a "View on Map" button.
 *
 * <p>The property is loaded <b>reactively</b> from Room via {@code getPropertyLive(id)}.
 * The previous implementation called a synchronous Room query in onCreate, which threw
 * {@code IllegalStateException: Cannot access database on the main thread} and closed the app.
 */
public class PropertyDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_PROPERTY_ID = "extra_property_id";

    private PropertyViewModel viewModel;
    private Property property;
    private long propertyId = -1;
    private boolean animatedIn = false;

    private MaterialButton btnFavorite, btnCall, btnEmail, btnMessage;
    private ImageView imgDetail, imgFeatured;
    private View sellerSection;
    private TextView tvBadge, tvPrice, tvTitle, tvLocation, tvBedrooms, tvBathrooms, tvArea,
            tvTypeListing, tvDescription, tvSellerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_details);

        viewModel = new ViewModelProvider(this).get(PropertyViewModel.class);

        propertyId = getIntent().getLongExtra(EXTRA_PROPERTY_ID, -1);
        if (propertyId == -1) {
            Toast.makeText(this, R.string.no_property_data, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Share action on the toolbar (inflated directly; NoActionBar theme -> no onCreateOptionsMenu).
        toolbar.inflateMenu(R.menu.menu_details);
        tintToolbarMenuIcons(toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                shareProperty();
                return true;
            }
            return false;
        });

        imgDetail = findViewById(R.id.imgDetail);
        imgFeatured = findViewById(R.id.imgFeatured);
        tvBadge = findViewById(R.id.tvBadge);
        tvPrice = findViewById(R.id.tvPrice);
        tvTitle = findViewById(R.id.tvTitle);
        tvLocation = findViewById(R.id.tvLocation);
        tvBedrooms = findViewById(R.id.tvBedrooms);
        tvBathrooms = findViewById(R.id.tvBathrooms);
        tvArea = findViewById(R.id.tvArea);
        tvTypeListing = findViewById(R.id.tvTypeListing);
        tvDescription = findViewById(R.id.tvDescription);
        btnFavorite = findViewById(R.id.btnFavorite);
        sellerSection = findViewById(R.id.sellerSection);
        tvSellerName = findViewById(R.id.tvSellerName);
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);
        btnMessage = findViewById(R.id.btnMessage);

        btnFavorite.setOnClickListener(v -> viewModel.toggleFavorite(propertyId));
        findViewById(R.id.btnMap).setOnClickListener(v -> openMap());

        // Load the property off the main thread. Room delivers the row on the UI thread once ready.
        viewModel.getPropertyLive(propertyId).observe(this, prop -> {
            if (prop == null) {
                // Genuinely missing row (bad id) — leave gracefully instead of crashing.
                Toast.makeText(this, R.string.no_property_data, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            property = prop;
            bindProperty(prop);
        });

        // Keep the favorite button in sync (updates when returning to this screen too).
        viewModel.getFavoriteIds().observe(this, ids ->
                renderFavoriteState(ids != null && ids.contains(propertyId)));
    }

    private void bindProperty(Property p) {
        // Image: remote URL via Glide with local drawable fallback.
        String imageUrl = p.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(imgDetail.getContext())
                    .load(imageUrl)
                    .placeholder(p.getImageRes())
                    .error(p.getImageRes())
                    .into(imgDetail);
        } else {
            imgDetail.setImageResource(p.getImageRes());
        }
        if (!animatedIn) {
            imgDetail.setAlpha(0f);
            imgDetail.animate().alpha(1f).setDuration(600).start();
            animatedIn = true;
        }

        // Featured star: shown only when the real featured flag is set (§Feature 1).
        imgFeatured.setVisibility(p.isFeatured() ? View.VISIBLE : View.GONE);

        tvBadge.setText(p.getListingType());
        int badgeColor = ContextCompat.getColor(this,
                "Rent".equals(p.getListingType()) ? R.color.rent_blue : R.color.primary);
        tvBadge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));

        tvPrice.setText(FormatUtils.formatListingPrice(p));
        tvTitle.setText(p.getTitle());
        tvLocation.setText(p.getLocation());
        tvBedrooms.setText(String.valueOf(p.getBedrooms()));
        tvBathrooms.setText(String.valueOf(p.getBathrooms()));
        tvArea.setText(getString(R.string.area_sqft, p.getArea()));
        tvTypeListing.setText(getString(R.string.type_listing_format,
                p.getPropertyType(), p.getListingType()));
        tvDescription.setText(p.getDescription());

        bindSeller(p);
    }

    /** Shows the seller card + wires contact buttons when seller data exists; hides it otherwise. */
    private void bindSeller(Property p) {
        String name = p.getSellerName();
        if (name == null || name.trim().isEmpty()) {
            sellerSection.setVisibility(View.GONE);
            return;
        }
        sellerSection.setVisibility(View.VISIBLE);
        tvSellerName.setText(name);

        String phone = p.getSellerPhone();
        String email = p.getSellerEmail();
        String subject = p.getTitle();
        btnCall.setOnClickListener(v -> dialSeller(phone));
        btnMessage.setOnClickListener(v -> messageSeller(phone));
        btnEmail.setOnClickListener(v -> emailSeller(email, subject));
    }

    /**
     * Opens the phone dialer pre-filled with the seller's number via {@code ACTION_DIAL}.
     * <p>Deliberately uses {@code ACTION_DIAL}, never {@code ACTION_CALL}: no {@code CALL_PHONE}
     * permission is needed and the call is never placed automatically — the user taps dial.
     */
    private void dialSeller(String phone) {
        if (phone == null || phone.trim().isEmpty()) return;
        String tel = phone.replaceAll("[^+0-9]", "");
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_dialer_app, Toast.LENGTH_SHORT).show();
        }
    }

    /** Opens the SMS composer to the seller via {@code ACTION_SENDTO} {@code smsto:}. */
    private void messageSeller(String phone) {
        if (phone == null || phone.trim().isEmpty()) return;
        String tel = phone.replaceAll("[^+0-9]", "");
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + tel));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_sms_app, Toast.LENGTH_SHORT).show();
        }
    }

    /** Opens the email composer to the seller via {@code ACTION_SENDTO} {@code mailto:} + subject. */
    private void emailSeller(String email, String subject) {
        if (email == null || email.trim().isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
        if (subject != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_email_app, Toast.LENGTH_SHORT).show();
        }
    }

    /** Shares the current property as plain text via {@code ACTION_SEND} + a chooser. */
    private void shareProperty() {
        if (property == null) {
            Toast.makeText(this, R.string.no_property_data, Toast.LENGTH_SHORT).show();
            return;
        }
        Property p = property;

        StringBuilder specs = new StringBuilder();
        if (p.getBedrooms() > 0) {
            specs.append(getString(R.string.bd, p.getBedrooms())).append(" · ");
        }
        specs.append(getString(R.string.bath, p.getBathrooms())).append(" · ")
                .append(getString(R.string.area_sqft, p.getArea()));

        StringBuilder body = new StringBuilder();
        body.append(p.getTitle()).append('\n')
                .append(FormatUtils.formatListingPrice(p)).append('\n')
                .append(p.getLocation()).append('\n')
                .append(specs).append('\n')
                .append(getString(R.string.type_listing_format, p.getPropertyType(), p.getListingType()))
                .append('\n');
        if (p.getSellerName() != null && !p.getSellerName().trim().isEmpty()) {
            body.append("Seller: ").append(p.getSellerName()).append('\n');
        }
        body.append(getString(R.string.share_footer));

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, p.getTitle());
        intent.putExtra(Intent.EXTRA_TEXT, body.toString());
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_share_app, Toast.LENGTH_SHORT).show();
        }
    }

    /** Tints toolbar menu icons to on_primary so they read as white on the green toolbar. */
    private void tintToolbarMenuIcons(MaterialToolbar toolbar) {
        Menu menu = toolbar.getMenu();
        int color = ContextCompat.getColor(this, R.color.on_primary);
        for (int i = 0; i < menu.size(); i++) {
            Drawable icon = menu.getItem(i).getIcon();
            if (icon != null) {
                icon.setTint(color);
            }
        }
    }

    /**
     * Opens the property location in an external maps app via an {@code ACTION_VIEW} geo Intent.
     * <p>Order of preference:
     * <ol>
     *   <li>Google Maps app ({@code com.google.android.apps.maps}), if installed;</li>
     *   <li>any app that handles {@code geo:} URIs (generic chooser/default);</li>
     *   <li>the in-app {@link MapPlaceholderActivity} coordinate preview as a last resort.</li>
     * </ol>
     * No embedded map SDK, no API key and no location permission are involved.
     */
    private void openMap() {
        if (property == null) {
            Toast.makeText(this, R.string.no_property_data, Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = property.getLatitude();
        double lng = property.getLongitude();
        String label = property.getTitle() != null ? property.getTitle() : "";

        // geo:lat,lng?q=lat,lng(Label) — drops a labelled pin at the property.
        String geoUri = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng
                + "(" + Uri.encode(label) + ")";
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));

        // 1) Prefer the Google Maps app when it is installed.
        try {
            startActivity(new Intent(mapIntent).setPackage("com.google.android.apps.maps"));
            return;
        } catch (ActivityNotFoundException ignored) {
            // Google Maps not installed — fall through to a generic handler.
        }

        // 2) Any other app registered for geo: URIs.
        try {
            startActivity(mapIntent);
            return;
        } catch (ActivityNotFoundException ignored) {
            // No maps app at all — fall through to the in-app preview.
        }

        // 3) Graceful in-app fallback: coordinate preview.
        Intent fallback = new Intent(this, MapPlaceholderActivity.class);
        fallback.putExtra(MapPlaceholderActivity.EXTRA_PROPERTY_ID, propertyId);
        startActivity(fallback);
    }

    private void renderFavoriteState(boolean favorite) {
        if (btnFavorite == null) return;
        if (favorite) {
            btnFavorite.setText(R.string.remove_from_favorites);
            btnFavorite.setIconResource(R.drawable.ic_heart_filled);
            btnFavorite.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.rent_blue)));
        } else {
            btnFavorite.setText(R.string.add_to_favorites);
            btnFavorite.setIconResource(R.drawable.ic_heart);
            btnFavorite.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.primary)));
        }
    }
}
