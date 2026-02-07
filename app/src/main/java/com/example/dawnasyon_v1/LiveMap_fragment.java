package com.example.dawnasyon_v1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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

// Networking
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// Map & Tiles
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

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

    private Marker homeMarker;
    private MyLocationNewOverlay locationOverlay;

    // API KEYS
    private static final String OPENWEATHER_API_KEY = "00572d4c95d6813ee92167727a796fab";

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
        return inflater.inflate(R.layout.fragment_live_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        map = view.findViewById(R.id.osmmap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        GeoPoint startPoint = new GeoPoint(14.7036, 121.0543);
        map.getController().setZoom(10.0);

        // =========================================================
        // ⭐ VISUAL LAYERS
        // =========================================================

        // 1. WIND ONLY (Typhoon Swirls) - Rain layer removed to clear the "green field"
        setupWindTiles();

        // 2. SETUP LOCATION
        setupCurrentLocation();

        if (targetAddress != null && !targetAddress.isEmpty()) {
            locateAddressOnMap(targetAddress, startPoint);
        } else {
            fetchRegisteredAddress(startPoint);
        }

        // 3. SCANNERS (Green < 4.0, Purple >= 4.0)
        fetchUSGSData();
        fetchPhivolcsData();
        fetchTyphoonTextAlert();

        Button btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    // =========================================================
    // ⭐ WIND LAYER (The Typhoon Visual)
    // =========================================================
    private void setupWindTiles() {
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

    // =========================================================
    // 24-HOUR FORECAST (Open-Meteo)
    // =========================================================
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

    // =========================================================
    // TYPHOON ALERT
    // =========================================================
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

    // =========================================================
    // EARTHQUAKE SCANNERS (Updated: GREEN for < 4.0)
    // =========================================================
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

                            // ⭐ RESTORED: Shows ALL quakes.
                            // Green = Weak (< 4.0), Purple/Magenta = Strong (>= 4.0)
                            addQuakeMarker(lat, lon, "PHIVOLCS: Mag " + mag, dateStr, mag >= 4.0 ? 0xFFFF00FF : 0xFF2E7D32);

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
                                    // ⭐ RESTORED: Color logic for USGS
                                    addQuakeMarker(lat, lon, "USGS: Mag " + mag, p.getString("place"), mag >= 4.0 ? 0xFFFF00FF : 0xFF2E7D32);
                                }
                            }
                        } catch(Exception e) {}
                    });
                }
            } catch (Exception e) {}
        }).start();
    }

    // =========================================================
    // MAP & LOCATION HELPERS
    // =========================================================
    private void setupCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
            locationOverlay.enableMyLocation();
            locationOverlay.runOnFirstFix(() -> {
                GeoPoint myLoc = locationOverlay.getMyLocation();
                if (myLoc != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if(targetAddress == null) map.getController().animateTo(myLoc);
                        fetchAdvancedWeather(myLoc.getLatitude(), myLoc.getLongitude(), "Current Location");
                    });
                }
            });
            map.getOverlays().add(locationOverlay);
        }
    }

    private void fetchRegisteredAddress(GeoPoint defaultPoint) {
        map.getController().setCenter(defaultPoint);
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null) {
                String address = (profile.getHouse_number() + " " + profile.getStreet() + ", " + profile.getCity()).replace("null", "").trim();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (address.length() > 5) locateAddressOnMap(address, defaultPoint);
                    else {
                        addHomeMarker(defaultPoint.getLatitude(), defaultPoint.getLongitude(), "Location", "Default");
                        fetchAdvancedWeather(defaultPoint.getLatitude(), defaultPoint.getLongitude(), "My Location");
                    }
                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    addHomeMarker(defaultPoint.getLatitude(), defaultPoint.getLongitude(), "Location", "Default");
                    fetchAdvancedWeather(defaultPoint.getLatitude(), defaultPoint.getLongitude(), "My Location");
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
                map.getController().setCenter(foundPoint);
                addHomeMarker(foundPoint.getLatitude(), foundPoint.getLongitude(), "My Household", addressStr);
                fetchAdvancedWeather(foundPoint.getLatitude(), foundPoint.getLongitude(), "My Household");
            } else {
                map.getController().setCenter(defaultPoint);
                addHomeMarker(defaultPoint.getLatitude(), defaultPoint.getLongitude(), "Location", "Default");
                fetchAdvancedWeather(defaultPoint.getLatitude(), defaultPoint.getLongitude(), "Default");
            }
        } catch (IOException e) {
            map.getController().setCenter(defaultPoint);
        }
    }

    private void addHomeMarker(double lat, double lon, String title, String snippet) {
        if (map == null) return;
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