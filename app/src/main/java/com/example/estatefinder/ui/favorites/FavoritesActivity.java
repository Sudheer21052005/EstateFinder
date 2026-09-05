package com.example.estatefinder.ui.favorites;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.estatefinder.R;
import com.example.estatefinder.model.Property;
import com.example.estatefinder.ui.details.PropertyDetailsActivity;
import com.example.estatefinder.ui.property.PropertyAdapter;
import com.example.estatefinder.viewmodel.PropertyViewModel;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Favorites screen (Phase 2): shows in-memory favorites.
 * Persistence arrives in Phase 3 with Room.
 */
public class FavoritesActivity extends AppCompatActivity implements PropertyAdapter.Listener {

    private PropertyViewModel viewModel;
    private PropertyAdapter adapter;
    private RecyclerView rvFavorites;
    private LinearLayout layoutEmpty;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        viewModel = new ViewModelProvider(this).get(PropertyViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        rvFavorites = findViewById(R.id.rvFavorites);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new PropertyAdapter(this);
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        rvFavorites.setAdapter(adapter);

        viewModel.getFavoriteProperties().observe(this, properties -> {
            adapter.submitList(properties);
            rvFavorites.scheduleLayoutAnimation();
            boolean empty = properties == null || properties.isEmpty();
            layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvFavorites.setVisibility(empty ? View.GONE : View.VISIBLE);
            int count = properties == null ? 0 : properties.size();
            toolbar.setSubtitle(getString(R.string.result_count, count));
        });
        viewModel.getFavoriteIds().observe(this, ids -> adapter.setFavoriteIds(ids));
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadFavorites();
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
