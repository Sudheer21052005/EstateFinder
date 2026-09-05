package com.example.estatefinder.ui.map;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.estatefinder.R;
import com.example.estatefinder.viewmodel.PropertyViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Locale;

/**
 * In-app coordinate preview, used as a graceful fallback when no external maps
 * app is available to handle the {@code geo:} Intent from the details screen.
 *
 * <p>The property is loaded <b>reactively</b> from Room via {@code getPropertyLive(id)};
 * the previous version issued a synchronous Room query on the main thread.
 */
public class MapPlaceholderActivity extends AppCompatActivity {

    public static final String EXTRA_PROPERTY_ID = "extra_property_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_placeholder);

        long id = getIntent().getLongExtra(EXTRA_PROPERTY_ID, -1);
        if (id == -1) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvName = findViewById(R.id.tvPropertyName);
        TextView tvLocation = findViewById(R.id.tvPropertyLocation);
        TextView tvCoordinates = findViewById(R.id.tvCoordinates);

        PropertyViewModel viewModel = new ViewModelProvider(this).get(PropertyViewModel.class);
        viewModel.getPropertyLive(id).observe(this, property -> {
            if (property == null) {
                Toast.makeText(this, R.string.no_property_data, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            tvName.setText(property.getTitle());
            tvLocation.setText(property.getLocation());
            tvCoordinates.setText(String.format(Locale.US, "%.4f° N, %.4f° E",
                    property.getLatitude(), property.getLongitude()));
        });
    }
}
