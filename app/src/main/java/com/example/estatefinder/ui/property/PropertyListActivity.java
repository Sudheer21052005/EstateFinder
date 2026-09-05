package com.example.estatefinder.ui.property;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.estatefinder.R;
import com.example.estatefinder.data.repository.PropertyRepository;
import com.example.estatefinder.model.Property;
import com.example.estatefinder.ui.details.PropertyDetailsActivity;
import com.example.estatefinder.viewmodel.PropertyViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;

/**
 * Full property list with live search + Buy/Rent + property-type filters (Phase 2).
 * Receives the query/filters chosen on Home via intent extras.
 */
public class PropertyListActivity extends AppCompatActivity implements PropertyAdapter.Listener {

    public static final String EXTRA_QUERY = "extra_query";
    public static final String EXTRA_LISTING = "extra_listing";
    public static final String EXTRA_TYPE = "extra_type";

    private PropertyViewModel viewModel;
    private PropertyAdapter adapter;

    private EditText etSearch;
    private MaterialButtonToggleGroup toggleListing;
    private ChipGroup chipGroupType;
    private RecyclerView rvProperties;
    private LinearLayout layoutEmpty;
    private MaterialToolbar toolbar;

    private boolean restoringFilters = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_list);

        viewModel = new ViewModelProvider(this).get(PropertyViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        toggleListing = findViewById(R.id.toggleListing);
        chipGroupType = findViewById(R.id.chipGroupType);
        rvProperties = findViewById(R.id.rvProperties);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        toolbar.setNavigationOnClickListener(v -> finish());

        // Sort menu: inflate directly onto the toolbar (NoActionBar theme -> onCreateOptionsMenu
        // items never render). Tint the icons white to match the title/nav on the green toolbar.
        toolbar.inflateMenu(R.menu.menu_property_list);
        tintToolbarMenuIcons();
        toolbar.setOnMenuItemClickListener(this::onSortMenuItem);

        adapter = new PropertyAdapter(this);
        rvProperties.setLayoutManager(new LinearLayoutManager(this));
        rvProperties.setAdapter(adapter);

        // Restore filters passed from Home
        String query = getIntent().getStringExtra(EXTRA_QUERY);
        String listing = getIntent().getStringExtra(EXTRA_LISTING);
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        applyFiltersToUi(query, listing, type);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch();
                return true;
            }
            return false;
        });
        toggleListing.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && !restoringFilters) runSearch();
        });
        chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!restoringFilters) runSearch();
        });

        viewModel.getSearchResults().observe(this, properties -> {
            adapter.submitList(properties);
            rvProperties.scheduleLayoutAnimation();
            boolean empty = properties == null || properties.isEmpty();
            layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvProperties.setVisibility(empty ? View.GONE : View.VISIBLE);
            int count = properties == null ? 0 : properties.size();
            toolbar.setSubtitle(getString(R.string.result_count, count));
        });
        viewModel.getFavoriteIds().observe(this, ids -> adapter.setFavoriteIds(ids));
    }

    /** Applies the chosen sort while preserving the active search text + Buy/Rent + type filters. */
    private boolean onSortMenuItem(MenuItem item) {
        PropertyRepository.SortOrder order;
        int id = item.getItemId();
        if (id == R.id.sort_price_low) order = PropertyRepository.SortOrder.PRICE_ASC;
        else if (id == R.id.sort_price_high) order = PropertyRepository.SortOrder.PRICE_DESC;
        else if (id == R.id.sort_area_low) order = PropertyRepository.SortOrder.AREA_ASC;
        else if (id == R.id.sort_area_high) order = PropertyRepository.SortOrder.AREA_DESC;
        else if (id == R.id.sort_newest) order = PropertyRepository.SortOrder.NEWEST;
        else return false; // action_sort parent — let the toolbar open the submenu
        item.setChecked(true);
        viewModel.setSort(order);
        return true;
    }

    /** Tints toolbar menu icons to on_primary so they read as white on the green toolbar. */
    private void tintToolbarMenuIcons() {
        Menu menu = toolbar.getMenu();
        int color = ContextCompat.getColor(this, R.color.on_primary);
        for (int i = 0; i < menu.size(); i++) {
            Drawable icon = menu.getItem(i).getIcon();
            if (icon != null) {
                icon.setTint(color);
            }
        }
    }

    private void applyFiltersToUi(String query, String listing, String type) {
        restoringFilters = true;
        if (query != null) {
            etSearch.setText(query);
            etSearch.setSelection(query.length());
        }
        if ("Sale".equals(listing)) {
            toggleListing.check(R.id.btnBuyListing);
        } else if ("Rent".equals(listing)) {
            toggleListing.check(R.id.btnRentListing);
        } else {
            toggleListing.check(R.id.btnAllListing);
        }
        int chipId;
        if ("Apartment".equals(type)) chipId = R.id.chipApartment;
        else if ("House".equals(type)) chipId = R.id.chipHouse;
        else if ("Villa".equals(type)) chipId = R.id.chipVilla;
        else if ("Office".equals(type)) chipId = R.id.chipOffice;
        else chipId = R.id.chipAll;
        chipGroupType.check(chipId);
        restoringFilters = false;
        runSearch();
    }

    private void runSearch() {
        String listing = currentListing();
        String type = currentType();
        viewModel.search(listing, type, etSearch.getText().toString());
    }

    private String currentListing() {
        int id = toggleListing.getCheckedButtonId();
        if (id == R.id.btnBuyListing) return "Sale";
        if (id == R.id.btnRentListing) return "Rent";
        return PropertyViewModel.FILTER_ALL;
    }

    private String currentType() {
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
