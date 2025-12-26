package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LiveMap_fragment extends BaseFragment {

    private MapView map;
    private static final String ARG_ADDRESS = "target_address";
    private String targetAddress;

    public LiveMap_fragment() {
        // Required empty public constructor
    }

    // ⭐ NEW: Accept Address as Argument ⭐
    public static LiveMap_fragment newInstance(String address) {
        LiveMap_fragment fragment = new LiveMap_fragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS, address);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetAddress = getArguments().getString(ARG_ADDRESS);
        }
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

        // Default Point (Barangay Sta. Lucia / Placeholder)
        GeoPoint startPoint = new GeoPoint(14.7036, 121.0543);
        map.getController().setZoom(18.0);

        // ⭐ NEW: Geocode the Address Logic ⭐
        if (targetAddress != null && !targetAddress.isEmpty()) {
            locateAddressOnMap(targetAddress, startPoint);
        } else {
            // No address provided, just show default
            map.getController().setCenter(startPoint);
            addColoredMarker(startPoint.getLatitude(), startPoint.getLongitude(), "Barangay Hall", "Center", Color.RED);
        }

        // Add other context markers (Floods, etc)
        addColoredMarker(14.7025, 121.0535, "Flood Risk", "Creek Side", Color.BLUE);
        addColoredMarker(14.7040, 121.0550, "Fire Alert", "Sitio 1", Color.RED);

        Button btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
    }

    private void locateAddressOnMap(String addressStr, GeoPoint defaultPoint) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            // Try to find the coordinates from the string
            List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                GeoPoint foundPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                // Move map to found address
                map.getController().setCenter(foundPoint);

                // Add a special pin for "My Location"
                addColoredMarker(foundPoint.getLatitude(), foundPoint.getLongitude(), "My Household", addressStr, Color.parseColor("#2E7D32")); // Green Pin
            } else {
                // If address not found, fallback
                map.getController().setCenter(defaultPoint);
                Toast.makeText(getContext(), "Address not found on map. Showing Barangay center.", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback for emulator/network errors
            map.getController().setCenter(defaultPoint);
            addColoredMarker(defaultPoint.getLatitude(), defaultPoint.getLongitude(), "Simulated Location", addressStr, Color.parseColor("#2E7D32"));
        }
    }

    private void addColoredMarker(double lat, double lon, String title, String snippet, int color) {
        if (map == null) return;
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        marker.setSnippet(snippet);

        Drawable icon = ContextCompat.getDrawable(requireContext(), org.osmdroid.library.R.drawable.marker_default);
        if (icon != null) {
            Drawable tintedIcon = icon.getConstantState().newDrawable().mutate();
            DrawableCompat.setTint(tintedIcon, color);
            marker.setIcon(tintedIcon);
        }
        map.getOverlays().add(marker);
    }
}