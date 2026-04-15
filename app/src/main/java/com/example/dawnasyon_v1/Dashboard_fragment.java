package com.example.dawnasyon_v1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // ⭐ NEW IMPORT

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Dashboard_fragment extends BaseFragment {

    private PieChart chartRelief;
    private PieChart chartAffected;
    private PieChart chartUserActivity;
    private LineChart chartFamilies;
    private LineChart chartDonations;
    private RadarChart chartImpact;

    private LinearLayout llReliefList;
    private LinearLayout llAffectedList;

    private TextView tvImpactCount;
    private ImageView iconFilter;

    private TextView txtPrediction;
    private TextView txtRiskBadge;
    private TextView txtRiskDesc;
    private TextView txtAffectedFamilies;

    // ⭐ NEW: Chatbot variables
    private FloatingActionButton fabChat;
    private String latestDashboardContextString = "No data loaded yet.";

    private String currentFilter = "all";

    // ⭐ SUPABASE CONFIGURATION
    private static final String SUPABASE_URL = "https://ypkbnwbxmnnptypxiaoa.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_dqUvLA6v5ZQtuUg9vBJfeQ_wRDp_2hi";
    private final OkHttpClient client = new OkHttpClient();

    private final int COLOR_DEEP_ORANGE = Color.parseColor("#E65100");
    private final int COLOR_VIBRANT_ORANGE = Color.parseColor("#F5901A");
    private final int COLOR_MED_ORANGE = Color.parseColor("#FFB74D");
    private final int COLOR_SOFT_ORANGE = Color.parseColor("#FFCC80");
    private final int COLOR_LIGHT_ORANGE = Color.parseColor("#FFE0B2");
    private final int COLOR_TEAL = Color.parseColor("#27869B");
    private final int COLOR_ACTIVE = Color.parseColor("#4CAF50");
    private final int COLOR_INACTIVE = Color.parseColor("#9E9E9E");

    private final int[] ORANGE_SCALE_COLORS = {
            COLOR_DEEP_ORANGE, COLOR_VIBRANT_ORANGE, COLOR_MED_ORANGE, COLOR_SOFT_ORANGE, COLOR_LIGHT_ORANGE
    };

    public Dashboard_fragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chartRelief = view.findViewById(R.id.chart_relief_status);
        chartAffected = view.findViewById(R.id.chart_affected_areas);
        chartUserActivity = view.findViewById(R.id.chart_user_activity);
        chartFamilies = view.findViewById(R.id.chart_registered_families);
        chartDonations = view.findViewById(R.id.chart_donation_trends);
        chartImpact = view.findViewById(R.id.chart_donation_impact);

        tvImpactCount = view.findViewById(R.id.tv_impact_count);
        iconFilter = view.findViewById(R.id.icon_filter);

        txtPrediction = view.findViewById(R.id.txt_prediction);
        txtRiskBadge = view.findViewById(R.id.txt_risk_badge);
        txtRiskDesc = view.findViewById(R.id.txt_risk_desc);
        txtAffectedFamilies = view.findViewById(R.id.txt_affected_families);

        Button btnLiveMap = view.findViewById(R.id.btn_live_map);
        llReliefList = view.findViewById(R.id.ll_relief_list);
        llAffectedList = view.findViewById(R.id.ll_affected_list);

        fabChat = view.findViewById(R.id.fab_chat);

        // ⭐ NEW: DRAGGABLE FAB LOGIC
        if (fabChat != null) {
            fabChat.setOnTouchListener(new View.OnTouchListener() {
                private float dX, dY;
                private float startX, startY;
                private static final int CLICK_TOLERANCE = 10;

                @Override
                public boolean onTouch(View view, android.view.MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            dX = view.getX() - event.getRawX();
                            dY = view.getY() - event.getRawY();
                            startX = event.getRawX();
                            startY = event.getRawY();
                            break;
                        case android.view.MotionEvent.ACTION_MOVE:
                            view.setY(event.getRawY() + dY);
                            view.setX(event.getRawX() + dX);
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                            if (Math.abs(event.getRawX() - startX) < CLICK_TOLERANCE &&
                                    Math.abs(event.getRawY() - startY) < CLICK_TOLERANCE) {
                                view.performClick(); // It was a click, not a drag!
                            }
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            });

            fabChat.setOnClickListener(v -> {
                GeminiChatDialog chatDialog = GeminiChatDialog.newInstance(latestDashboardContextString);
                chatDialog.show(getParentFragmentManager(), "GeminiChat");
            });
        }

        setupReliefPieChart();
        setupAffectedPieChart();
        setupUserActivityChart();
        setupFamiliesLineChart();
        setupDonationTrendsChart();
        setupImpactRadarChart();

        if (btnLiveMap != null) btnLiveMap.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LiveMap_fragment())
                .addToBackStack(null).commit());

        if (iconFilter != null) iconFilter.setOnClickListener(this::showFilterMenu);

        loadRealData(view, currentFilter);
        applyTagalogTranslation(view);
    }
    private void showFilterMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenu().add(0, 0, 0, "All Time");
        popup.getMenu().add(0, 1, 1, "Monthly (This Month)");
        popup.getMenu().add(0, 2, 2, "Yearly (This Year)");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 0: currentFilter = "all"; break;
                case 1: currentFilter = "monthly"; break;
                case 2: currentFilter = "yearly"; break;
            }
            if (getView() != null) {
                loadRealData(getView(), currentFilter);
            }
            return true;
        });
        popup.show();
    }

    private Map<String, Integer> getTop5(Map<String, Integer> data) {
        if (data == null || data.isEmpty()) return new HashMap<>();
        List<Map.Entry<String, Integer>> list = new ArrayList<>(data.entrySet());
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Integer> entry : list) {
            if (count >= 5) break;
            sortedMap.put(entry.getKey(), entry.getValue());
            count++;
        }
        return sortedMap;
    }

    private Map<String, Integer> categorizeInventory(Map<String, Integer> rawInventory) {
        Map<String, Integer> categorized = new LinkedHashMap<>();
        int packs = 0, food = 0, medicine = 0, hygiene = 0, others = 0;

        if (rawInventory != null) {
            for (Map.Entry<String, Integer> entry : rawInventory.entrySet()) {
                String key = entry.getKey().toLowerCase();
                int qty = entry.getValue();

                if (key.contains("pack") || key.contains("relief pack") || key.contains("ayuda")) {
                    packs += qty;
                } else if (key.contains("rice") || key.contains("instant noodle") || key.contains("noodle") || key.contains("can good") || key.contains("canned") || key.contains("biscuit") || key.contains("water") || key.contains("food")) {
                    food += qty;
                } else if (key.contains("pain reliever") || key.contains("vitamin") || key.contains("cough syrup") || key.contains("first aid kit") || key.contains("medicine") || key.contains("paracetamol")) {
                    medicine += qty;
                } else if (key.contains("body care") || key.contains("sanitation") || key.contains("laundry") || key.contains("protection") || key.contains("hygiene") || key.contains("soap") || key.contains("kit")) {
                    hygiene += qty;
                } else {
                    others += qty;
                }
            }
        }

        if (packs > 0) categorized.put("Relief Packs", packs);
        if (food > 0) categorized.put("Food & Water", food);
        if (medicine > 0) categorized.put("Medicine", medicine);
        if (hygiene > 0) categorized.put("Hygiene Kits", hygiene);
        if (others > 0) categorized.put("Others", others);

        return categorized;
    }

    private void loadRealData(View view, String filterType) {
        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

        SupabaseJavaHelper.fetchDashboardData(getContext(), filterType, new SupabaseJavaHelper.DashboardCallback() {
            @Override
            public void onDataLoaded(Map<String, Integer> inventory, Map<String, Integer> areas, Map<String, Float> donations, Map<String, Integer> families, DashboardMetrics metrics, Map<String, Integer> impact) {
                if (isAdded() && getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                if (!isAdded()) return;

                // ⭐ AGGRESSIVE FIX: Remove anything that looks like "ALL STREET", "ALL AREAS", "ALL"
                if (areas != null) {
                    List<String> keysToRemove = new ArrayList<>();
                    for (String key : areas.keySet()) {
                        if (key != null) {
                            String normalizedKey = key.trim().toLowerCase();
                            if (normalizedKey.equals("all") ||
                                    normalizedKey.contains("all street") ||
                                    normalizedKey.contains("all area")) {
                                keysToRemove.add(key);
                            }
                        }
                    }
                    for (String key : keysToRemove) {
                        areas.remove(key); // Annihilate it from the map!
                    }
                }

                Map<String, Integer> categorizedInventory = categorizeInventory(inventory);
                updatePieChart(chartRelief, categorizedInventory, "Relief\nItems");
                updateListUI(llReliefList, categorizedInventory, "Category");

                Map<String, Integer> topAreas = getTop5(areas);
                updatePieChart(chartAffected, topAreas, "Affected\nAreas");
                updateListUI(llAffectedList, topAreas, "Street");

                updateUserActivityChart(metrics.getActive_users(), metrics.getTotal_users());
                updateLineChart(chartDonations, donations);

                Map<String, Float> familiesFloat = new HashMap<>();
                if (families != null) {
                    for (Map.Entry<String, Integer> entry : families.entrySet()) {
                        familiesFloat.put(entry.getKey(), entry.getValue().floatValue());
                    }
                }
                updateLineChart(chartFamilies, familiesFloat);
                updateRadarChart(impact);

                int reliefPacksCount = categorizedInventory.containsKey("Relief Packs") ? categorizedInventory.get("Relief Packs") : 0;
                fetchActiveAffectedFamiliesAndUpdateUI(view, metrics, reliefPacksCount, categorizedInventory, donations, familiesFloat, topAreas);
            }

            @Override
            public void onError(String message) {
                if (isAdded() && getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                if (isAdded()) Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchActiveAffectedFamiliesAndUpdateUI(View view, DashboardMetrics metrics, int reliefPacksCount, Map<String, Integer> inventory, Map<String, Float> donations, Map<String, Float> familiesFloat, Map<String, Integer> areas) {
        String url = SUPABASE_URL + "/rest/v1/announcements?select=families_affected,event_date&status=eq.Approved";
        Request request = new Request.Builder().url(url).addHeader("apikey", SUPABASE_KEY).addHeader("Authorization", "Bearer " + SUPABASE_KEY).build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                int activeAffected = 0;
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JSONArray array = new JSONArray(json);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -45);
                    Date activeThreshold = cal.getTime();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        int fam = obj.optInt("families_affected", 0);
                        String eDateStr = obj.optString("event_date", "");
                        if (fam > 0 && !eDateStr.isEmpty() && !eDateStr.equals("null")) {
                            try {
                                String targetDateStr = eDateStr.toLowerCase().contains("to") ? eDateStr.toLowerCase().split("to")[1].trim() : eDateStr.trim();
                                Date eDate = sdf.parse(targetDateStr);
                                if (eDate != null && !eDate.before(activeThreshold)) activeAffected += fam;
                            } catch (Exception e) {}
                        }
                    }
                }
                int finalAffected = activeAffected;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded()) updateAnalyticsUI(view, metrics, finalAffected, reliefPacksCount, inventory, donations, familiesFloat, areas);
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded()) updateAnalyticsUI(view, metrics, metrics.getTotal_affected(), reliefPacksCount, inventory, donations, familiesFloat, areas);
                });
            }
        }).start();
    }


    private void updateAnalyticsUI(View view, DashboardMetrics metrics, int totalAffected, int reliefPacks, Map<String, Integer> inventory, Map<String, Float> donations, Map<String, Float> families, Map<String, Integer> areas) {

        // ⭐ NEW: Save the live context to the string so Gemini can read it!
        latestDashboardContextString = "Total Registered Families: " + metrics.getTotal_families() + ". " +
                "Currently Affected Families (Recent Disaster): " + totalAffected + ". " +
                "Full Inventory Breakdown (Use this to suggest what is missing): " + inventory.toString() + ". " +
                "Top Affected Areas: " + areas.keySet().toString() + ".";

        int totalPopulation = metrics.getTotal_families();

        TextView tvPercentage = view.findViewById(R.id.tv_percentage);
        if (tvPercentage != null) tvPercentage.setText(String.valueOf(totalPopulation));

        int coveragePercent = totalAffected > 0 ? (int) (((float) reliefPacks / totalAffected) * 100) : 100;
        if (coveragePercent > 100) coveragePercent = 100;
        int deficit = Math.max(0, totalAffected - reliefPacks);

        ProgressBar progCoverage = view.findViewById(R.id.progress_coverage);
        TextView txtPercent = view.findViewById(R.id.txt_coverage_percent);
        TextView txtInsight = view.findViewById(R.id.txt_coverage_insight);

        // ⭐ RESIDENT-FRIENDLY WORDING FOR THE METRICS
        if (progCoverage != null) {
            progCoverage.setProgress(coveragePercent);
            txtPercent.setText(coveragePercent + "%");

            if (totalAffected == 0) {
                progCoverage.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                txtInsight.setText("Community is safe. No active disasters reported.");
                txtInsight.setTextColor(Color.parseColor("#2E7D32"));
                if(txtPrediction != null) txtPrediction.setText("Status: Normal & Peaceful");
            } else if (coveragePercent < 50) {
                progCoverage.setProgressTintList(ColorStateList.valueOf(Color.RED));
                txtInsight.setText("Pending Support: The barangay is working to secure packs for " + deficit + " more families.");
                txtInsight.setTextColor(Color.RED);
                if(txtPrediction != null) txtPrediction.setText("Update: Actively gathering more donations.");
            } else if (coveragePercent < 100) {
                progCoverage.setProgressTintList(ColorStateList.valueOf(COLOR_DEEP_ORANGE));
                txtInsight.setText("Relief operations ongoing. " + deficit + " more families need support.");
                txtInsight.setTextColor(COLOR_DEEP_ORANGE);
                if(txtPrediction != null) txtPrediction.setText("Update: Relief goods are still arriving.");
            } else {
                progCoverage.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                txtInsight.setText("The barangay has secured enough aid for recent victims.");
                txtInsight.setTextColor(Color.parseColor("#2E7D32"));
                if(txtPrediction != null) txtPrediction.setText("Status: Relief supply is stable.");
            }
        }

        if (txtRiskBadge != null) {
            if (totalAffected > 50) {
                txtRiskBadge.setText("STAY ALERT");
                txtRiskBadge.setTextColor(Color.WHITE);
                txtRiskBadge.setBackgroundColor(Color.RED);
            } else if (totalAffected > 20) {
                txtRiskBadge.setText("BE CAUTIOUS");
                txtRiskBadge.setTextColor(Color.WHITE);
                txtRiskBadge.setBackgroundColor(COLOR_DEEP_ORANGE);
            } else {
                txtRiskBadge.setText("SAFE");
                txtRiskBadge.setTextColor(Color.WHITE);
                txtRiskBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
            }
        }

        if(txtAffectedFamilies != null) txtAffectedFamilies.setText(totalAffected + " Families Affected Recently");
        if(txtRiskDesc != null) txtRiskDesc.setText("Stay safe and look out for each other.");

        if(txtRiskDesc != null) TranslationHelper.autoTranslate(getContext(), txtRiskDesc, txtRiskDesc.getText().toString());
        if(txtPrediction != null) TranslationHelper.autoTranslate(getContext(), txtPrediction, txtPrediction.getText().toString());
        if(txtInsight != null) TranslationHelper.autoTranslate(getContext(), txtInsight, txtInsight.getText().toString());

        // Trigger the Resident-Focused AI Engine
        generatePredictiveInsights(view, inventory, donations, areas, totalAffected);
    }

    // =========================================================
    // 💡 RESIDENT-FOCUSED COMMUNITY INSIGHTS ENGINE
    // =========================================================
    private void generatePredictiveInsights(View view, Map<String, Integer> inventory, Map<String, Float> donations, Map<String, Integer> areas, int activeAffected) {
        LinearLayout llInsights = view.findViewById(R.id.ll_predictive_insights);
        if (llInsights == null) return;

        llInsights.removeAllViews();

        TextView header = new TextView(getContext());
        header.setText("Community Insights & Alerts");
        header.setTextSize(18f);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setTextColor(Color.parseColor("#27869B"));
        header.setPadding(0, 0, 0, 16);
        llInsights.addView(header);

        // --- RESIDENT INSIGHT 1: RELIEF AVAILABILITY ---
        int reliefPacks = inventory != null && inventory.containsKey("Relief Packs") ? inventory.get("Relief Packs") : 0;
        if (activeAffected > 0) {
            float coverage = (float) reliefPacks / activeAffected;
            String availMsg;
            int availColor;

            if (coverage >= 1.0f) {
                availMsg = "Good Supply: The barangay currently has enough relief packs for recent victims. Please check announcements for claiming schedules.";
                availColor = Color.parseColor("#E8F5E9");
            } else if (coverage >= 0.5f) {
                availMsg = "Moderate Supply: Relief packs are actively being distributed, but stocks are currently limited. Priority may be given to highly affected areas.";
                availColor = Color.parseColor("#FFF3E0");
            } else {
                availMsg = "Limited Supply: Relief goods are currently running low. The barangay is awaiting more donations. Please stand by for updates.";
                availColor = Color.parseColor("#FFEBEE");
            }
            llInsights.addView(createInsightCard("Relief Availability", availMsg, availColor));
        }

        // --- RESIDENT INSIGHT 2: COMMUNITY SUPPORT TREND ---
        if (donations != null && donations.size() >= 2) {
            List<Float> donValues = new ArrayList<>(donations.values());
            float donTrend = donValues.get(donValues.size() - 1) - donValues.get(donValues.size() - 2);

            String trendMsg;
            int trendColor;

            if (donTrend > 0) {
                trendMsg = "Community Strong: We are seeing an increase in donations! Thank you to everyone helping our barangay recover.";
                trendColor = Color.parseColor("#E8F5E9");
            } else {
                trendMsg = "Bayanihan Needed: Donations have slowed down recently. If you have extra resources, consider helping your neighbors in need.";
                trendColor = Color.parseColor("#E3F2FD");
            }
            llInsights.addView(createInsightCard("Community Support", trendMsg, trendColor));
        }

        // --- RESIDENT INSIGHT 3: NEIGHBORHOOD ALERT ---
        if (areas != null && !areas.isEmpty() && activeAffected > 0) {
            Map.Entry<String, Integer> topArea = null;
            for (Map.Entry<String, Integer> entry : areas.entrySet()) {
                if (topArea == null || entry.getValue() > topArea.getValue()) {
                    topArea = entry;
                }
            }

            if (topArea != null) {
                String routeMsg = "Area Update: " + topArea.getKey() + " has reported the highest number of affected families recently. Please offer assistance to neighbors in this vicinity if you are able.";
                llInsights.addView(createInsightCard("Neighborhood Alert", routeMsg, Color.parseColor("#F3E5F5")));
            }
        }
    }

    private View createInsightCard(String title, String message, int bgColor) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(bgColor);
        card.setPadding(24, 24, 24, 24);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setPadding(0, 0, 0, 8);

        TextView tvMsg = new TextView(getContext());
        tvMsg.setText(message);
        tvMsg.setTextColor(Color.DKGRAY);
        tvMsg.setTextSize(14f);

        card.addView(tvTitle);
        card.addView(tvMsg);

        TranslationHelper.autoTranslate(getContext(), tvTitle, tvTitle.getText().toString());
        TranslationHelper.autoTranslate(getContext(), tvMsg, tvMsg.getText().toString());

        return card;
    }

    private void updateListUI(LinearLayout container, Map<String, Integer> data, String labelTitle) {
        if (container == null) return;
        container.removeAllViews();

        TextView header = new TextView(getContext());
        header.setText(labelTitle + "          Count");
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setTextColor(Color.BLACK);
        header.setPadding(0, 0, 0, 8);
        container.addView(header);

        if (data == null || data.isEmpty()) return;

        int colorIndex = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            TextView itemRow = new TextView(getContext());
            String label = entry.getKey();
            if (label != null && label.length() > 16) label = label.substring(0, 16) + "..";

            String fullText = "●  " + label + "   " + entry.getValue();
            SpannableString spannable = new SpannableString(fullText);

            int bulletColor = ORANGE_SCALE_COLORS[colorIndex % ORANGE_SCALE_COLORS.length];
            spannable.setSpan(new ForegroundColorSpan(bulletColor), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 1, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            itemRow.setText(spannable);
            itemRow.setSingleLine(true);
            itemRow.setEllipsize(TextUtils.TruncateAt.END);
            itemRow.setPadding(0, 4, 0, 4);

            container.addView(itemRow);
            colorIndex++;
        }
    }

    private void setupReliefPieChart() {
        chartRelief.setDrawHoleEnabled(true);
        chartRelief.setHoleColor(Color.WHITE);
        chartRelief.setHoleRadius(65f);
        chartRelief.setTransparentCircleRadius(70f);
        chartRelief.setDrawEntryLabels(false);
        chartRelief.getDescription().setEnabled(false);
        chartRelief.getLegend().setEnabled(false);
        chartRelief.setCenterTextSize(11f);
        chartRelief.setCenterTextColor(Color.DKGRAY);
        chartRelief.setCenterText("Relief\nItems");
    }

    private void setupAffectedPieChart() {
        chartAffected.setDrawHoleEnabled(true);
        chartAffected.setHoleColor(Color.WHITE);
        chartAffected.setHoleRadius(65f);
        chartAffected.setTransparentCircleRadius(70f);
        chartAffected.setDrawEntryLabels(false);
        chartAffected.getDescription().setEnabled(false);
        chartAffected.getLegend().setEnabled(false);
        chartAffected.setCenterTextSize(11f);
        chartAffected.setCenterTextColor(Color.DKGRAY);
        chartAffected.setCenterText("Affected\nAreas");
    }

    private void setupUserActivityChart() {
        if (chartUserActivity == null) return;
        chartUserActivity.setDrawHoleEnabled(true);
        chartUserActivity.setHoleColor(Color.WHITE);
        chartUserActivity.setHoleRadius(65f);
        chartUserActivity.setTransparentCircleRadius(70f);
        chartUserActivity.setDrawEntryLabels(false);
        chartUserActivity.getDescription().setEnabled(false);
        chartUserActivity.getLegend().setEnabled(false);
        chartUserActivity.setCenterTextSize(11f);
        chartUserActivity.setCenterTextColor(Color.DKGRAY);
        chartUserActivity.setCenterText("User\nActivity");
    }

    private void updateUserActivityChart(int activeCount, int totalCount) {
        if (chartUserActivity == null) return;
        int inactiveCount = Math.max(0, totalCount - activeCount);
        List<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        if (activeCount > 0) { entries.add(new PieEntry((float) activeCount, "Active")); colors.add(COLOR_ACTIVE); }
        if (inactiveCount > 0) { entries.add(new PieEntry((float) inactiveCount, "Inactive")); colors.add(COLOR_INACTIVE); }
        if (entries.isEmpty()) { entries.add(new PieEntry(1f, "No Data")); colors.add(Color.LTGRAY); }
        PieDataSet dataSet = new PieDataSet(entries, "Activity");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setDrawValues(false);
        PieData data = new PieData(dataSet);
        chartUserActivity.setData(data);

        chartUserActivity.invalidate();
    }

    private void updatePieChart(PieChart chart, Map<String, Integer> data, String defaultCenterText) {
        if (data == null || data.isEmpty()) return;
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }
        PieDataSet set;
        if (chart.getData() != null && chart.getData().getDataSet() != null) {
            set = (PieDataSet) chart.getData().getDataSet();
            set.setValues(entries);
        } else {
            set = new PieDataSet(entries, "");
        }
        set.setColors(ORANGE_SCALE_COLORS);
        set.setDrawValues(false);
        PieData pieData = new PieData(set);
        chart.setData(pieData);
        chart.setCenterText(defaultCenterText);
        chart.invalidate();
    }

    private void setupImpactRadarChart() {
        chartImpact.getDescription().setEnabled(false);
        chartImpact.setWebLineWidth(1f);
        chartImpact.setWebColor(Color.LTGRAY);
        chartImpact.setWebLineWidthInner(1f);
        chartImpact.setWebColorInner(Color.LTGRAY);
        chartImpact.setWebAlpha(100);
        chartImpact.getLegend().setEnabled(false);
        chartImpact.getYAxis().setEnabled(false);
        chartImpact.getXAxis().setTextSize(9f);
        chartImpact.getXAxis().setValueFormatter(new IndexAxisValueFormatter(new String[]{}));
        chartImpact.getXAxis().setTextColor(Color.DKGRAY);
    }

    private void updateRadarChart(Map<String, Integer> data) {
        if (data == null || data.isEmpty()) {
            chartImpact.clear();
            if (tvImpactCount != null) tvImpactCount.setText("0");
            return;
        }
        List<RadarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int totalHelped = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            entries.add(new RadarEntry(entry.getValue()));
            labels.add(entry.getKey());
            totalHelped += entry.getValue();
        }
        if (tvImpactCount != null) tvImpactCount.setText(String.valueOf(totalHelped));

        RadarDataSet set = new RadarDataSet(entries, "Impact");
        set.setColor(COLOR_VIBRANT_ORANGE);
        set.setFillColor(COLOR_SOFT_ORANGE);
        set.setDrawFilled(true);
        set.setFillAlpha(100);
        set.setDrawValues(true);

        RadarData radarData = new RadarData(set);
        chartImpact.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartImpact.setData(radarData);
        chartImpact.invalidate();
    }

    private void updateLineChart(LineChart chart, Map<String, Float> data) {
        if (data == null || data.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Float> entry : data.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setGranularityEnabled(true);

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            LineDataSet set = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set.setValues(entries);
            set.setDrawValues(false);
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }

    private void setupFamiliesLineChart() {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Families");
        dataSet.setColor(COLOR_VIBRANT_ORANGE);
        dataSet.setLineWidth(3f);
        XAxis xAxis = chartFamilies.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        chartFamilies.getAxisLeft().setDrawGridLines(false);
        chartFamilies.getAxisRight().setDrawGridLines(false);
        LineData data = new LineData(dataSet);
        chartFamilies.setData(data);
        chartFamilies.getDescription().setEnabled(false);
    }

    private void setupDonationTrendsChart() {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Donations");
        dataSet.setColor(COLOR_TEAL);
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(true);
        XAxis xAxis = chartDonations.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        chartDonations.getAxisLeft().setDrawGridLines(false);
        chartDonations.getAxisRight().setDrawGridLines(false);
        LineData data = new LineData(dataSet);
        chartDonations.setData(data);
        chartDonations.getDescription().setEnabled(false);
    }
}