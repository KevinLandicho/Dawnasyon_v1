package com.example.dawnasyon_v1;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class LiveMap_fragment extends BaseFragment {

    private MapView map;
    private static final String ARG_ADDRESS = "target_address";
    private String targetAddress;
    private final OkHttpClient client = new OkHttpClient();

    public LiveMap_fragment() {
        // Required empty public constructor
    }

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

        GeoPoint startPoint = new GeoPoint(14.7036, 121.0543);
        map.getController().setZoom(16.0);

        if (targetAddress != null && !targetAddress.isEmpty()) {
            locateAddressOnMap(targetAddress, startPoint);
        } else {
            map.getController().setCenter(startPoint);
        }

        // Hardcoded Risks (Using Boxes)
        addQuakeMarker(14.7025, 121.0535, "Flood Risk", "Creek Side", Color.BLUE);
        addQuakeMarker(14.7040, 121.0550, "Fire Alert", "Sitio 1", Color.RED);

        // Scanners
        fetchUSGSData();
        fetchPhivolcsData();

        Button btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
    }

    // =========================================================
    // 1. LOCATE ADDRESS (Uses "Normal Pin" Style)
    // =========================================================
    private void locateAddressOnMap(String addressStr, GeoPoint defaultPoint) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                GeoPoint foundPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                map.getController().setCenter(foundPoint);

                // ⭐ USE NORMAL PIN FOR HOME (White)
                addHomeMarker(foundPoint.getLatitude(), foundPoint.getLongitude(), "My Household", addressStr);

            } else {
                map.getController().setCenter(defaultPoint);
            }
        } catch (IOException e) {
            map.getController().setCenter(defaultPoint);
        }
    }

    // ⭐ NEW FUNCTION: Adds a NORMAL PIN (Teardrop shape) colored White
    private void addHomeMarker(double lat, double lon, String title, String snippet) {
        if (map == null) return;
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); // Normal Anchor
        marker.setTitle(title);
        marker.setSnippet(snippet);

        // Load the default pin drawable
        Drawable icon = ContextCompat.getDrawable(requireContext(), org.osmdroid.library.R.drawable.marker_default);
        if (icon != null) {
            Drawable tintedIcon = icon.getConstantState().newDrawable().mutate();
            // Set Color to WHITE (as requested)
            // Note: White might be hard to see on light maps, so I added a slight tint of Light Gray if needed,
            // but here is pure White per your request.
            DrawableCompat.setTint(tintedIcon, Color.WHITE);
            marker.setIcon(tintedIcon);
        }
        map.getOverlays().add(marker);
        map.invalidate();
    }

    // =========================================================
    // 2. EARTHQUAKE MARKERS (Uses "Small Box" Style)
    // =========================================================
    private void addQuakeMarker(double lat, double lon, String title, String snippet, int color) {
        if (map == null || getContext() == null) return;

        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); // Center Anchor for Box
        marker.setTitle(title);
        marker.setSnippet(snippet);

        GradientDrawable boxDrawable = new GradientDrawable();
        boxDrawable.setShape(GradientDrawable.RECTANGLE);
        boxDrawable.setSize(24, 24);
        boxDrawable.setBounds(0, 0, 24, 24);
        boxDrawable.setColor(color);
        boxDrawable.setStroke(2, Color.WHITE);

        marker.setIcon(boxDrawable);
        map.getOverlays().add(marker);
        map.invalidate();
    }

    // =========================================================
    // DATA FETCHERS (Updated to call addQuakeMarker)
    // =========================================================

    private void fetchPhivolcsData() {
        new Thread(() -> {
            try {
                String url = "https://earthquake.phivolcs.dost.gov.ph/";
                Document doc = Jsoup.connect(url)
                        .sslSocketFactory(socketFactory())
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                        .ignoreHttpErrors(true)
                        .timeout(60000)
                        .get();
                Elements rows = doc.select("table tr");

                new Handler(Looper.getMainLooper()).post(() -> {
                    for (Element row : rows) {
                        try {
                            Elements cols = row.select("td");
                            if (cols.size() < 6) continue;
                            String latStr = cols.get(1).text().replace("\u00A0", "").trim();
                            if (latStr.matches(".*[a-zA-Z]+.*")) continue;

                            double lat = Double.parseDouble(latStr);
                            double lon = Double.parseDouble(cols.get(2).text().replace("\u00A0", "").trim());
                            double mag = Double.parseDouble(cols.get(4).text().replace("\u00A0", "").trim());
                            String dateStr = cols.get(0).text().replace("\u00A0", " ").trim();
                            String location = cols.get(5).text().replace("\u00A0", " ").trim();

                            int color = getMarkerColor(mag);
                            // ⭐ Call addQuakeMarker for boxes
                            addQuakeMarker(lat, lon, "PHIVOLCS: Mag " + mag, dateStr + " - " + location, color);
                        } catch (Exception e) { continue; }
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void fetchUSGSData() {
        String url = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson";
        Request request = new Request.Builder().url(url).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            JSONObject root = new JSONObject(jsonData);
                            JSONArray features = root.getJSONArray("features");
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());

                            for (int i = 0; i < features.length(); i++) {
                                JSONObject feature = features.getJSONObject(i);
                                JSONObject properties = feature.getJSONObject("properties");
                                JSONArray coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates");

                                double lon = coordinates.getDouble(0);
                                double lat = coordinates.getDouble(1);
                                double mag = properties.getDouble("mag");
                                String place = properties.getString("place");
                                String dateStr = sdf.format(new Date(properties.getLong("time")));

                                if (lat > 4 && lat < 22 && lon > 116 && lon < 127) {
                                    int color = getMarkerColor(mag);
                                    // ⭐ Call addQuakeMarker for boxes
                                    addQuakeMarker(lat, lon, "USGS: Mag " + mag, dateStr + " - " + place, color);
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private int getMarkerColor(double mag) {
        if (mag >= 4.0) return 0xFFFF00FF;
        else return 0xFF2E7D32;
    }

    private SSLSocketFactory socketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        }};
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}