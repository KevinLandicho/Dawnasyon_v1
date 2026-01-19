package com.example.dawnasyon_v1;

import android.content.Context;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PhLocationHelper {

    // Cache the data so we don't read the file every time
    private static JSONObject fullData;

    // Map: Province -> List of Cities
    private static final Map<String, List<String>> cityMap = new HashMap<>();
    // Map: City -> List of Barangays
    private static final Map<String, List<String>> brgyMap = new HashMap<>();

    public static void loadData(Context context) {
        if (fullData != null) return; // Already loaded

        try {
            // Read JSON from Assets
            InputStream is = context.getAssets().open("ph_locations.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            fullData = new JSONObject(jsonString);

            // Structure of typical PSGC JSON:
            // { "NCR": { "province_list": { "Metro Manila": { "municipality_list": { "Manila": { "barangay_list": [...] } } } } } }

            // NOTE: The parsing logic depends heavily on the JSON structure you downloaded.
            // This logic below assumes the standard format from the link provided above.

            Iterator<String> regionKeys = fullData.keys();
            while (regionKeys.hasNext()) {
                String regionName = regionKeys.next();
                JSONObject regionObj = fullData.getJSONObject(regionName);

                if (regionObj.has("province_list")) {
                    JSONObject provinceList = regionObj.getJSONObject("province_list");
                    Iterator<String> provKeys = provinceList.keys();

                    while (provKeys.hasNext()) {
                        String provName = provKeys.next();
                        JSONObject provObj = provinceList.getJSONObject(provName);

                        // Process Cities
                        if (provObj.has("municipality_list")) {
                            JSONObject cityList = provObj.getJSONObject("municipality_list");
                            Iterator<String> cityKeys = cityList.keys();
                            List<String> cities = new ArrayList<>();

                            while (cityKeys.hasNext()) {
                                String cityName = cityKeys.next();
                                cities.add(cityName);

                                // Process Barangays
                                JSONObject cityObj = cityList.getJSONObject(cityName);
                                if (cityObj.has("barangay_list")) {
                                    List<String> brgys = new ArrayList<>();
                                    org.json.JSONArray brgyArray = cityObj.getJSONArray("barangay_list");
                                    for(int i=0; i<brgyArray.length(); i++) {
                                        brgys.add(brgyArray.getString(i));
                                    }
                                    Collections.sort(brgys);
                                    brgyMap.put(cityName, brgys);
                                }
                            }
                            Collections.sort(cities);
                            cityMap.put(provName, cities);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getProvinces() {
        if (cityMap.isEmpty()) return new ArrayList<>();
        List<String> provinces = new ArrayList<>(cityMap.keySet());
        Collections.sort(provinces);
        return provinces;
    }

    public static List<String> getCities(String province) {
        if (cityMap.containsKey(province)) {
            return cityMap.get(province);
        }
        return new ArrayList<>();
    }

    public static List<String> getBarangays(String city) {
        if (brgyMap.containsKey(city)) {
            return brgyMap.get(city);
        }
        return new ArrayList<>();
    }
}