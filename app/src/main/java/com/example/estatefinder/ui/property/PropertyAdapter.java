package com.example.estatefinder.ui.property;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.estatefinder.R;
import com.example.estatefinder.model.Property;
import com.example.estatefinder.util.FormatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * One RecyclerView adapter reused by Home (featured), Property List and Favorites.
 * Hearts are driven by a favorite-id set so all screens stay in sync.
 */
public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder> {

    /** Implemented by each screen that shows property cards. */
    public interface Listener {
        void onPropertyClick(Property property);

        void onFavoriteClick(Property property);
    }

    private final List<Property> items = new ArrayList<>();
    private final Listener listener;
    private Set<Long> favoriteIds = Collections.emptySet();

    public PropertyAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Property> properties) {
        items.clear();
        if (properties != null) {
            items.addAll(properties);
        }
        notifyDataSetChanged();
    }

    public void setFavoriteIds(Set<Long> ids) {
        this.favoriteIds = ids == null ? Collections.emptySet() : ids;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_property_card, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class PropertyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgProperty;
        private final ImageView imgFeatured;
        private final ImageButton btnFavorite;
        private final TextView tvBadge, tvTitle, tvLocation, tvPrice, tvSpecs;

        PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProperty = itemView.findViewById(R.id.imgProperty);
            imgFeatured = itemView.findViewById(R.id.imgFeatured);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSpecs = itemView.findViewById(R.id.tvSpecs);
        }

        void bind(Property property) {
            // Load image: remote URL via Glide with local drawable fallback
            String imageUrl = property.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(imgProperty.getContext())
                        .load(imageUrl)
                        .placeholder(property.getImageRes())
                        .error(property.getImageRes())
                        .into(imgProperty);
            } else {
                imgProperty.setImageResource(property.getImageRes());
            }

            // Featured star: shown only for rows with the real featured flag set (§Feature 1).
            imgFeatured.setVisibility(property.isFeatured() ? View.VISIBLE : View.GONE);

            tvBadge.setText(property.getListingType());
            int badgeColor = ContextCompat.getColor(itemView.getContext(),
                    "Rent".equals(property.getListingType())
                            ? R.color.rent_blue : R.color.primary);
            tvBadge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));

            tvTitle.setText(property.getTitle());
            tvLocation.setText(property.getLocation());
            tvPrice.setText(FormatUtils.formatListingPrice(property));

            if (property.getBedrooms() > 0) {
                tvSpecs.setText(itemView.getContext().getString(R.string.bd, property.getBedrooms())
                        + " · " + property.getPropertyType());
            } else {
                // Offices have no bedrooms — show type only
                tvSpecs.setText(property.getPropertyType());
            }

            boolean favorite = favoriteIds.contains(property.getId());
            btnFavorite.setImageResource(favorite
                    ? R.drawable.ic_heart_filled : R.drawable.ic_heart);

            itemView.setOnClickListener(v -> listener.onPropertyClick(property));
            btnFavorite.setOnClickListener(v -> listener.onFavoriteClick(property));
        }
    }
}
