package com.example.dawnasyon_v1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private Marker homeMarker;
    private Marker selectedLocationMarker;
    private MyLocationNewOverlay locationOverlay;

    // TRACK MAP MODE (Satellite vs Road)
    private boolean isSatelliteMode = false;

    // API KEYS
    private static final String OPENWEATHER_API_KEY = "00572d4c95d6813ee92167727a796fab";

    // BRGY STA. LUCIA COORDINATES (Center)
    private static final double STA_LUCIA_LAT = 14.7046;
    private static final double STA_LUCIA_LON = 121.0560;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    setupCurrentLocation();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    setupCurrentLocation();
                } else {
                    Toast.makeText(getContext(), "Location permission denied.", Toast.LENGTH_SHORT).show();
                }
            });

    public LiveMap_fragment() {}

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
        Configuration.getInstance().setUserAgentValue("com.example.dawnasyon_v1");
        return inflater.inflate(R.layout.fragment_live_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        map = view.findViewById(R.id.osmmap);

        // 1. DEFAULT TO MAPNIK
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // 2. CENTER ON STA. LUCIA & HIGH ZOOM
        GeoPoint startPoint = new GeoPoint(STA_LUCIA_LAT, STA_LUCIA_LON);
        map.getController().setZoom(16.5); // Adjusted zoom to fit new border
        map.getController().setCenter(startPoint);

        // 3. ADD REALISTIC BORDER POLYGON
        drawStaLuciaBorder();

        // 4. ADD HAZARD ZONES
        addHazardZones();

        // 5. ADD TAP LISTENER
        addMapTapListener();

        setupWindTiles();
        checkAndRequestLocation();

        if (targetAddress != null && !targetAddress.isEmpty()) {
            String betterAddress = targetAddress + ", Philippines";
            locateAddressOnMap(betterAddress, startPoint);
        } else {
            fetchRegisteredAddress(startPoint);
        }

        fetchUSGSData();
        fetchPhivolcsData();
        fetchTyphoonTextAlert();

        Button btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // SATELLITE TOGGLE
        ImageButton btnLayerToggle = view.findViewById(R.id.btn_layer_toggle);
        if (btnLayerToggle != null) {
            btnLayerToggle.setOnClickListener(v -> {
                if (isSatelliteMode) {
                    map.setTileSource(TileSourceFactory.MAPNIK);
                    Toast.makeText(getContext(), "Road View", Toast.LENGTH_SHORT).show();
                } else {
                    OnlineTileSourceBase esriSatellite = new OnlineTileSourceBase(
                            "ArcGIS World Imagery",
                            0, 19, 256, "",
                            new String[] { "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/" }
                    ) {
                        @Override
                        public String getTileURLString(long pMapTileIndex) {
                            return getBaseUrl()
                                    + MapTileIndex.getZoom(pMapTileIndex) + "/"
                                    + MapTileIndex.getY(pMapTileIndex) + "/"
                                    + MapTileIndex.getX(pMapTileIndex);
                        }
                    };
                    map.setTileSource(esriSatellite);
                    Toast.makeText(getContext(), "Satellite View", Toast.LENGTH_SHORT).show();
                }
                isSatelliteMode = !isSatelliteMode;
                map.invalidate();
            });
        }

        // STREET VIEW TOGGLE
        ImageButton btnStreetView = view.findViewById(R.id.btn_street_view);
        if (btnStreetView != null) {
            btnStreetView.setOnClickListener(v -> openGoogleMapsStreetView());
        }

        applyTagalogTranslation(view);
    }

    // ⭐ NEW: DETAILED BORDER FOR BRGY STA. LUCIA, QC
    private void drawStaLuciaBorder() {
        List<GeoPoint> borderPoints = new ArrayList<>();

        // These coordinates trace the actual irregular shape of Sta. Lucia (Counter-Clockwise)
        // North-West Tip (Near Tullahan River / Sauyo)
        borderPoints.add(new GeoPoint(14.7135, 121.0550));
        borderPoints.add(new GeoPoint(14.7120, 121.0580));

        // East Boundary (Near Fairview / Commonwealth)
        borderPoints.add(new GeoPoint(14.7100, 121.0605));
        borderPoints.add(new GeoPoint(14.7070, 121.0620));
        borderPoints.add(new GeoPoint(14.7030, 121.0625));

        // South-East Corner
        borderPoints.add(new GeoPoint(14.6990, 121.0610));

        // South Boundary (Near Holy Spirit)
        borderPoints.add(new GeoPoint(14.6975, 121.0580));
        borderPoints.add(new GeoPoint(14.6970, 121.0550));

        // West Boundary (Following Creek/River)
        borderPoints.add(new GeoPoint(14.6990, 121.0520));
        borderPoints.add(new GeoPoint(14.7020, 121.0505));
        borderPoints.add(new GeoPoint(14.7060, 121.0500));
        borderPoints.add(new GeoPoint(14.7090, 121.0515));
        borderPoints.add(new GeoPoint(14.7115, 121.0530));

        Polygon polygon = new Polygon();
        polygon.setPoints(borderPoints);
        polygon.setFillColor(Color.argb(30, 255, 140, 0)); // Transparent Dark Orange
        polygon.setStrokeColor(Color.RED);
        polygon.setStrokeWidth(4.0f); // Thicker border
        polygon.setTitle("Brgy. Sta. Lucia Boundary");

        map.getOverlays().add(polygon);
        map.invalidate();
    }

    private void addHazardZones() {
        // --- 1. FIRE ALERT (Red Box) ---
        GeoPoint firePoint = new GeoPoint(STA_LUCIA_LAT + 0.0015, STA_LUCIA_LON - 0.0010);
        Marker fireMarker = new Marker(map);
        fireMarker.setPosition(firePoint);
        fireMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        fireMarker.setTitle("🔥 FIRE ALERT");
        fireMarker.setSnippet("Active Fire Reported: Residential Area");

        GradientDrawable fireBox = new GradientDrawable();
        fireBox.setShape(GradientDrawable.RECTANGLE);
        fireBox.setSize(30, 30);
        fireBox.setColor(Color.RED);
        fireBox.setStroke(3, Color.WHITE);
        fireMarker.setIcon(fireBox);

        map.getOverlays().add(fireMarker);

        // --- 2. FLOOD RISK (Blue Polygon + Blue Box) ---
        List<GeoPoint> floodPoints = new ArrayList<>();
        floodPoints.add(new GeoPoint(STA_LUCIA_LAT - 0.0020, STA_LUCIA_LON + 0.0010));
        floodPoints.add(new GeoPoint(STA_LUCIA_LAT - 0.0025, STA_LUCIA_LON + 0.0015));
        floodPoints.add(new GeoPoint(STA_LUCIA_LAT - 0.0015, STA_LUCIA_LON + 0.0025));
        floodPoints.add(new GeoPoint(STA_LUCIA_LAT - 0.0010, STA_LUCIA_LON + 0.0020));

        Polygon floodZone = new Polygon();
        floodZone.setPoints(floodPoints);
        floodZone.setFillColor(Color.argb(70, 0, 0, 255));
        floodZone.setStrokeColor(Color.BLUE);
        floodZone.setStrokeWidth(2.0f);
        floodZone.setTitle("🌊 FLOOD RISK ZONE");

        Marker floodMarker = new Marker(map);
        floodMarker.setPosition(new GeoPoint(STA_LUCIA_LAT - 0.0017, STA_LUCIA_LON + 0.0017));
        floodMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        floodMarker.setTitle("Flood Prone Area");
        floodMarker.setSnippet("High Water Level: Creek Area");

        GradientDrawable floodBox = new GradientDrawable();
        floodBox.setShape(GradientDrawable.RECTANGLE);
        floodBox.setSize(30, 30);
        floodBox.setColor(Color.BLUE);
        floodBox.setStroke(3, Color.WHITE);
        floodMarker.setIcon(floodBox);

        map.getOverlays().add(floodZone);
        map.getOverlays().add(floodMarker);

        map.invalidate();
    }

    private void addMapTapListener() {
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                addSelectedLocationMarker(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(receiver));
    }

    private void addSelectedLocationMarker(GeoPoint p) {
        if (selectedLocationMarker != null) {
            map.getOverlays().remove(selectedLocationMarker);
        }

        selectedLocationMarker = new Marker(map);
        selectedLocationMarker.setPosition(p);
        selectedLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedLocationMarker.setTitle("Selected Location");

        DecimalFormat df = new DecimalFormat("#.#####");
        String coords = df.format(p.getLatitude()) + ", " + df.format(p.getLongitude());
        selectedLocationMarker.setSnippet("Coords: " + coords + "\nTap Street View to see this area.");

        map.getOverlays().add(selectedLocationMarker);
        map.invalidate();
        selectedLocationMarker.showInfoWindow();
    }

    private void openGoogleMapsStreetView() {
        double lat, lon;
        if (selectedLocationMarker != null) {
            lat = selectedLocationMarker.getPosition().getLatitude();
            lon = selectedLocationMarker.getPosition().getLongitude();
        } else if (map.getMapCenter() != null) {
            lat = map.getMapCenter().getLatitude();
            lon = map.getMapCenter().getLongitude();
            Toast.makeText(getContext(), "Using Map Center", Toast.LENGTH_SHORT).show();
        } else {
            return;
        }

        Uri gmmIntentUri = Uri.parse("google.streetview:cbll=" + lat + "," + lon);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri browserUri = Uri.parse("https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=" + lat + "," + lon);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            startActivity(browserIntent);
        }
    }

    private void checkAndRequestLocation() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void setupWindTiles() {
        if (getContext() == null) return;
        final MapTileProviderBasic provider = new MapTileProviderBasic(requireContext());
        final OnlineTileSourceBase windSource = new OnlineTileSourceBase(
                "OpenWeatherMap Wind",
                0, 18, 256, ".png",
                new String[] { "https://tile.openweathermap.org/map/wind_new/" }
        ) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl()
                        + MapTileIndex.getZoom(pMapTileIndex) + "/"
                        + MapTileIndex.getX(pMapTileIndex) + "/"
                        + MapTileIndex.getY(pMapTileIndex)
                        + ".png?appid=" + OPENWEATHER_API_KEY;
            }
        };

        provider.setTileSource(windSource);
        TilesOverlay windOverlay = new TilesOverlay(provider, requireContext());
        windOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        map.getOverlays().add(0, windOverlay);
        map.invalidate();
    }

    private void fetchAdvancedWeather(double lat, double lon, String title) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&hourly=temperature_2m,weathercode&forecast_days=1&timezone=Asia%2FManila";
        Request request = new Request.Builder().url(url).build();
        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            JSONObject root = new JSONObject(jsonData);
                            JSONObject hourly = root.getJSONObject("hourly");
                            JSONArray temps = hourly.getJSONArray("temperature_2m");
                            JSONArray codes = hourly.getJSONArray("weathercode");

                            StringBuilder forecastSummary = new StringBuilder();
                            if (temps.length() > 0) {
                                double tempNow = temps.getDouble(0);
                                String condNow = decodeWmoCode(codes.getInt(0));
                                forecastSummary.append("Now: ").append(condNow).append(" (").append(tempNow).append("°C)\n");
                            }
                            if (temps.length() > 6) {
                                String condLater = decodeWmoCode(codes.getInt(6));
                                forecastSummary.append("Later (+6h): ").append(condLater);
                            }
                            if (homeMarker != null) {
                                String info = forecastSummary.toString();
                                homeMarker.setSnippet(title + "\n" + info);
                                if (homeMarker.isInfoWindowShown()) {
                                    homeMarker.closeInfoWindow();
                                    homeMarker.showInfoWindow();
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private String decodeWmoCode(int code) {
        if (code == 0) return "☀️ Clear";
        if (code >= 1 && code <= 3) return "🌥️ Cloudy";
        if (code >= 51 && code <= 67) return "🌧️ Rain";
        if (code >= 80 && code <= 99) return "⛈️ Storm";
        return "Normal";
    }

    private void fetchTyphoonTextAlert() {
        new Thread(() -> {
            try {
                String url = "https://bagong.pagasa.dost.gov.ph/tropical-cyclone/severe-weather-bulletin";
                Document doc = Jsoup.connect(url)
                        .sslSocketFactory(socketFactory())
                        .userAgent("Mozilla/5.0")
                        .timeout(10000).get();

                Elements headers = doc.select("h3, h4, .panel-heading");
                boolean typhoonFound = false;
                String typhoonName = "";

                for (Element header : headers) {
                    String text = header.text().toUpperCase();
                    if (text.contains("TROPICAL CYCLONE") && !text.contains("NO ACTIVE")) {
                        typhoonFound = true;
                        if (text.contains("“")) {
                            typhoonName = text.substring(text.indexOf("“"));
                        } else {
                            typhoonName = "Active Cyclone";
                        }
                        break;
                    }
                }

                if (typhoonFound) {
                    String finalName = typhoonName;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getContext(), "⚠️ ALERT: " + finalName + " Detected!", Toast.LENGTH_LONG).show();
                        addQuakeMarker(14.5995, 120.9842, "⚠️ " + finalName, "LPA/Typhoon Visualized on Map", Color.RED);
                    });
                }

            } catch (Exception e) {}
        }).start();
    }

    private void fetchPhivolcsData() {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect("https://earthquake.phivolcs.dost.gov.ph/").sslSocketFactory(socketFactory()).timeout(60000).get();
                Elements rows = doc.select("table tr");

                long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy - hh:mm a", Locale.ENGLISH);

                new Handler(Looper.getMainLooper()).post(() -> {
                    for (Element row : rows) {
                        try {
                            Elements cols = row.select("td");
                            if(cols.size() < 6) continue;
                            String dateStr = cols.get(0).text().replace("\u00A0", " ").trim();
                            try {
                                Date quakeDate = sdf.parse(dateStr);
                                if (quakeDate != null && quakeDate.getTime() < twentyFourHoursAgo) continue;
                            } catch (Exception e) { continue; }

                            double lat = Double.parseDouble(cols.get(1).text().trim());
                            double lon = Double.parseDouble(cols.get(2).text().trim());
                            double mag = Double.parseDouble(cols.get(4).text().trim());

                            addQuakeMarker(lat, lon, "PHIVOLCS: Mag " + mag, dateStr, 0xFFFF00FF);

                        } catch (Exception e) {}
                    }
                });
            } catch (Exception e) {}
        }).start();
    }

    private void fetchUSGSData() {
        Request req = new Request.Builder().url("https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson").build();
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

        new Thread(() -> {
            try (Response res = client.newCall(req).execute()) {
                if (res.body() != null) {
                    String json = res.body().string();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            JSONArray features = new JSONObject(json).getJSONArray("features");
                            for (int i=0; i<features.length(); i++) {
                                JSONObject p = features.getJSONObject(i).getJSONObject("properties");
                                long time = p.getLong("time");
                                if (time < twentyFourHoursAgo) continue;

                                JSONArray c = features.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates");
                                double lat = c.getDouble(1);
                                double lon = c.getDouble(0);
                                double mag = p.getDouble("mag");

                                if(lat > 4 && lat < 22 && lon > 116 && lon < 127) {
                                    addQuakeMarker(lat, lon, "USGS: Mag " + mag, p.getString("place"), 0xFFFF00FF);
                                }
                            }
                        } catch(Exception e) {}
                    });
                }
            } catch (Exception e) {}
        }).start();
    }

    private void setupCurrentLocation() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
            locationOverlay.enableMyLocation();
            map.getOverlays().add(locationOverlay);
        }
    }

    private void fetchRegisteredAddress(GeoPoint defaultPoint) {
        map.getController().setCenter(new GeoPoint(STA_LUCIA_LAT, STA_LUCIA_LON));
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null) {
                String address = (profile.getHouse_number() + " " + profile.getStreet() + ", " + profile.getBarangay() + ", " + profile.getCity() + ", Philippines").replace("null", "").trim();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (address.length() > 5) locateAddressOnMap(address, defaultPoint);
                    else {
                        addHomeMarker(STA_LUCIA_LAT, STA_LUCIA_LON, "My Location", "Sta. Lucia (Default)");
                        fetchAdvancedWeather(STA_LUCIA_LAT, STA_LUCIA_LON, "My Location");
                    }
                });
            }
            return null;
        });
    }

    private void locateAddressOnMap(String addressStr, GeoPoint defaultPoint) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                GeoPoint foundPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                addHomeMarker(foundPoint.getLatitude(), foundPoint.getLongitude(), "My Household", addressStr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addHomeMarker(double lat, double lon, String title, String snippet) {
        if (map == null) return;
        if(homeMarker != null) map.getOverlays().remove(homeMarker);

        homeMarker = new Marker(map);
        homeMarker.setPosition(new GeoPoint(lat, lon));
        homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        homeMarker.setTitle(title);
        homeMarker.setSnippet(snippet);
        try {
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_house);
            if (icon != null) homeMarker.setIcon(icon);
        } catch (Exception e) {}
        map.getOverlays().add(homeMarker);
        map.invalidate();
    }

    private void addQuakeMarker(double lat, double lon, String title, String snippet, int color) {
        if (map == null) return;
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setTitle(title);
        marker.setSnippet(snippet);
        GradientDrawable box = new GradientDrawable();
        box.setShape(GradientDrawable.RECTANGLE);
        box.setSize(24, 24);
        box.setColor(color);
        box.setStroke(2, Color.WHITE);
        marker.setIcon(box);
        map.getOverlays().add(marker);
        map.invalidate();
    }

    private SSLSocketFactory socketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}