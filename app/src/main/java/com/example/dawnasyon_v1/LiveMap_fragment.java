package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class LiveMap_fragment extends BaseFragment {

    private MapView map;

    public LiveMap_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context ctx = requireContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        return inflater.inflate(R.layout.fragment_live_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        map = view.findViewById(R.id.osmmap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // 1. Center Map on Barangay Sta. Lucia
        GeoPoint startPoint = new GeoPoint(14.7036, 121.0543);
        map.getController().setZoom(16.5);
        map.getController().setCenter(startPoint);

        // 2. Add FIRE Marker (Red)
        addColoredMarker(14.7040, 121.0550, "Active Fire", "Sitio 1", Color.RED);

        // 3. Add FLOOD Marker (Blue)
        addColoredMarker(14.7025, 121.0535, "Flood Risk", "Creek Side", Color.BLUE);

        // 4. Add EARTHQUAKE Marker (Orange)
        addColoredMarker(14.7050, 121.0520, "Earthquake Damage", "Road Blocked", Color.parseColor("#FFA500"));

        Button btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    // Helper to add a marker with a specific color
    private void addColoredMarker(double lat, double lon, String title, String snippet, int color) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        marker.setSnippet(snippet);

        // Load the default marker icon
        Drawable icon = ContextCompat.getDrawable(requireContext(), org.osmdroid.library.R.drawable.marker_default);
        if (icon != null) {
            // Tint it to the requested color (Red, Blue, Orange)
            Drawable tintedIcon = icon.getConstantState().newDrawable().mutate();
            DrawableCompat.setTint(tintedIcon, color);
            marker.setIcon(tintedIcon);
        }

        map.getOverlays().add(marker);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}