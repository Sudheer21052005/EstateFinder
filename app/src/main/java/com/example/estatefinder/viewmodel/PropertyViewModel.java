package com.example.estatefinder.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.estatefinder.data.repository.PropertyRepository;
import com.example.estatefinder.model.Property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MVVM ViewModel: the UI observes LiveData and calls methods;
 * it never touches SampleData or the repository internals directly.
 * Survives configuration changes (e.g. rotation), so lists are not refetched.
 */
public class PropertyViewModel extends AndroidViewModel {

    public static final String FILTER_ALL = "All";

    private final PropertyRepository repository;

    private final LiveData<List<Property>> featuredProperties;
    private final MediatorLiveData<List<Property>> searchResults = new MediatorLiveData<>();
    private final LiveData<List<Property>> favoriteProperties;
    private final LiveData<Set<Long>> favoriteIds;

    // Current search state (filters + sort) so search() and setSort() can rebuild consistently.
    private String curQuery = "";
    private String curListing = FILTER_ALL;
    private String curType = FILTER_ALL;
    private PropertyRepository.SortOrder curSort = PropertyRepository.SortOrder.NEWEST;
    // Tracked so we removeSource(the real source) before adding the next one (no source-stacking).
    private LiveData<List<Property>> curSource;

    public PropertyViewModel(Application application) {
        super(application);
        PropertyRepository.init(application);
        repository = PropertyRepository.getInstance();

        featuredProperties = repository.getFeaturedLive();
        favoriteIds = repository.getFavoriteIdsLiveData();
        // Favorites are resolved reactively via a Room JOIN — no main-thread per-id lookups.
        favoriteProperties = repository.getFavoritePropertiesLive();

        // Initial search (defaults: no query, all filters, newest-first)
        rebuildSearch();
    }

    /**
     * Rebuilds the search source from the current filter + sort state.
     * Removes the previously-tracked source before adding the new one — the old
     * removeSource(null) was a no-op and let sources stack every time Sort/search fired.
     */
    private void rebuildSearch() {
        LiveData<List<Property>> source =
                repository.searchLive(curQuery, curListing, curType, curSort);
        if (curSource != null) {
            searchResults.removeSource(curSource);
        }
        curSource = source;
        searchResults.addSource(source, searchResults::setValue);
    }

    public LiveData<List<Property>> getSearchResults() { return searchResults; }
    public LiveData<List<Property>> getFeaturedProperties() { return featuredProperties; }
    public LiveData<List<Property>> getFavoriteProperties() { return favoriteProperties; }
    public LiveData<Set<Long>> getFavoriteIds() { return favoriteIds; }

    /** Query = search text; both filters are "All" / "Sale" / "Rent" and "All" / property types. */
    public void search(String listingType, String propertyType, String query) {
        curListing = listingType == null ? FILTER_ALL : listingType;
        curType = propertyType == null ? FILTER_ALL : propertyType;
        curQuery = query == null ? "" : query;
        rebuildSearch();
    }

    /** Change the sort order, preserving the current search text and filters. */
    public void setSort(PropertyRepository.SortOrder sort) {
        curSort = sort == null ? PropertyRepository.SortOrder.NEWEST : sort;
        rebuildSearch();
    }

    public void loadFavorites() {
        // favoriteProperties already kept in sync via favoriteIds LiveData
    }

    /** Trigger a remote refresh. */
    public void refresh() {
        repository.refreshFromNetwork();
    }

    /** Reactive single-property lookup (details / map screens) — runs off the UI thread. */
    public LiveData<Property> getPropertyLive(long id) {
        return repository.getPropertyLive(id);
    }

    public boolean isFavorite(long id) {
        return repository.isFavorite(id);
    }

    /** Toggles and re-emits all live lists so every open screen stays in sync. */
    public void toggleFavorite(long id) {
        repository.toggleFavorite(id);
        // Notify searchResults to refresh heart icons
        List<Property> current = searchResults.getValue();
        if (current != null) {
            searchResults.setValue(new ArrayList<>(current));
        }
    }
}