package com.example.estatefinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.estatefinder.model.Property;
import com.example.estatefinder.ui.details.PropertyDetailsActivity;
import com.example.estatefinder.ui.favorites.FavoritesActivity;
import com.example.estatefinder.ui.property.PropertyAdapter;
import com.example.estatefinder.ui.property.PropertyListActivity;
import com.example.estatefinder.viewmodel.PropertyViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;

import android.widget.EditText;

/**
 * Home screen: search entry, Buy/Rent + property-type filters,
 * a featured list preview and access to Favorites (Phase 2).
 */
public class MainActivity extends AppCompatActivity implements PropertyAdapter.Listener {

    private PropertyViewModel viewModel;
    private PropertyAdapter featuredAdapter;

    private android.widget.EditText etSearch;
    private com.google.android.material.button.MaterialButtonToggleGroup toggleListing;
    private com.google.android.material.chip.ChipGroup chipGroupType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(PropertyViewModel.class);

        etSearch = findViewById(R.id.etSearch);
        toggleListing = findViewById(R.id.toggleListing);
        chipGroupType = findViewById(R.id.chipGroupType);

        RecyclerView rvFeatured = findViewById(R.id.rvFeatured);
        featuredAdapter = new PropertyAdapter(this);
        rvFeatured.setLayoutManager(new LinearLayoutManager(this));
        rvFeatured.setAdapter(featuredAdapter);

        findViewById(R.id.btnFavorites).setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class)));
        findViewById(R.id.btnFind).setOnClickListener(v -> openPropertyList());
        findViewById(R.id.btnViewAll).setOnClickListener(v -> openPropertyList());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                openPropertyList();
                return true;
            }
            return false;
        });

        viewModel.getFeaturedProperties().observe(this, properties -> {
            featuredAdapter.submitList(properties);
            rvFeatured.scheduleLayoutAnimation();
        });
        viewModel.getFavoriteIds().observe(this, ids -> featuredAdapter.setFavoriteIds(ids));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Reads the home filters and opens the full list with them pre-applied. */
    private void openPropertyList() {
        Intent intent = new Intent(this, PropertyListActivity.class);
        intent.putExtra(PropertyListActivity.EXTRA_QUERY, etSearch.getText().toString());
        intent.putExtra(PropertyListActivity.EXTRA_LISTING, selectedListing());
        intent.putExtra(PropertyListActivity.EXTRA_TYPE, selectedType());
        startActivity(intent);
    }

    private String selectedListing() {
        int id = toggleListing.getCheckedButtonId();
        if (id == R.id.btnBuyListing) return "Sale";
        if (id == R.id.btnRentListing) return "Rent";
        return PropertyViewModel.FILTER_ALL;
    }

    private String selectedType() {
        int id = chipGroupType.getCheckedChipId();
        if (id == R.id.chipApartment) return "Apartment";
        if (id == R.id.chipHouse) return "House";
        if (id == R.id.chipVilla) return "Villa";
        if (id == R.id.chipOffice) return "Office";
        return PropertyViewModel.FILTER_ALL;
    }

    // --- PropertyAdapter.Listener ---

    @Override
    public void onPropertyClick(Property property) {
        Intent intent = new Intent(this, PropertyDetailsActivity.class);
        intent.putExtra(PropertyDetailsActivity.EXTRA_PROPERTY_ID, property.getId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Property property) {
        viewModel.toggleFavorite(property.getId());
    }
}
