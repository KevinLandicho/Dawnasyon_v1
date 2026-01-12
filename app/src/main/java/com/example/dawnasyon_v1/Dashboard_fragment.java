package com.example.dawnasyon_v1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// MPAndroidChart Imports
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dashboard_fragment extends BaseFragment {

    private PieChart chartRelief;
    private PieChart chartAffected;
    private LineChart chartFamilies;
    private LineChart chartDonations;

    private LinearLayout llReliefList;
    private LinearLayout llAffectedList;

    // --- EXPANDED COLOR PALETTE ---
    // 1. Deep/Dark Orange (Primary/Warning)
    private final int COLOR_DEEP_ORANGE = Color.parseColor("#E65100");
    // 2. Vibrant Orange (Brand)
    private final int COLOR_VIBRANT_ORANGE = Color.parseColor("#F5901A");
    // 3. Medium Orange
    private final int COLOR_MED_ORANGE = Color.parseColor("#FFB74D");
    // 4. Soft Orange
    private final int COLOR_SOFT_ORANGE = Color.parseColor("#FFCC80");
    // 5. Light Orange
    private final int COLOR_LIGHT_ORANGE = Color.parseColor("#FFE0B2");
    // 6. Pale Orange (Backgrounds)
    private final int COLOR_PALE_ORANGE = Color.parseColor("#FFF3E0");

    // Teal for contrast
    private final int COLOR_TEAL = Color.parseColor("#27869B");

    // Array for cycling colors in charts/lists
    private final int[] ORANGE_SCALE_COLORS = {
            COLOR_DEEP_ORANGE,
            COLOR_VIBRANT_ORANGE,
            COLOR_MED_ORANGE,
            COLOR_SOFT_ORANGE,
            COLOR_LIGHT_ORANGE
    };

    public Dashboard_fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        chartRelief = view.findViewById(R.id.chart_relief_status);
        chartAffected = view.findViewById(R.id.chart_affected_areas);
        chartFamilies = view.findViewById(R.id.chart_registered_families);
        chartDonations = view.findViewById(R.id.chart_donation_trends);
        Button btnLiveMap = view.findViewById(R.id.btn_live_map);

        llReliefList = view.findViewById(R.id.ll_relief_list);
        llAffectedList = view.findViewById(R.id.ll_affected_list);

        // Initial Setup
        setupReliefPieChart();
        setupAffectedPieChart();
        setupFamiliesLineChart();
        setupDonationTrendsChart();

        // Listeners
        View.OnClickListener mapClickListener = v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new LiveMap_fragment())
                    .addToBackStack(null)
                    .commit();
        };

        if (btnLiveMap != null) btnLiveMap.setOnClickListener(mapClickListener);
        if (chartAffected != null) chartAffected.setOnClickListener(mapClickListener);

        loadRealData(view);
    }

    private void loadRealData(View view) {
        SupabaseJavaHelper.fetchDashboardData(new DashboardCallback() {
            @Override
            public void onDataLoaded(Map<String, Integer> inventory,
                                     Map<String, Integer> areas,
                                     Map<String, Float> donations,
                                     Map<String, Integer> families,
                                     DashboardMetrics metrics) {
                if (!isAdded()) return;

                // A. Update Charts
                updatePieChart(chartRelief, inventory);
                updatePieChart(chartAffected, areas);
                updateLineChart(chartDonations, donations);

                // Update Families Chart
                Map<String, Float> familiesFloat = new HashMap<>();
                if (families != null) {
                    for (Map.Entry<String, Integer> entry : families.entrySet()) {
                        familiesFloat.put(entry.getKey(), entry.getValue().floatValue());
                    }
                }
                updateLineChart(chartFamilies, familiesFloat);

                // B. Update Lists
                updateListUI(llReliefList, inventory, "Item");
                updateListUI(llAffectedList, areas, "Street");

                // C. Update Analytics (Top Card & Registered Families Text)
                updateAnalyticsUI(view, metrics);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error loading dashboard: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateListUI(LinearLayout container, Map<String, Integer> data, String labelTitle) {
        if (container == null) return;
        container.removeAllViews();

        TextView header = new TextView(getContext());
        header.setText(labelTitle + "          Count");
        header.setTextSize(12);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setTextColor(Color.BLACK);
        header.setPadding(0, 0, 0, 8);
        container.addView(header);

        if (data == null || data.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No data available");
            empty.setTextSize(12);
            container.addView(empty);
            return;
        }

        // Cycle through the expanded orange palette
        int colorIndex = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            TextView itemRow = new TextView(getContext());
            itemRow.setText("● " + entry.getKey() + "   " + entry.getValue());
            itemRow.setTextSize(12);

            // Use the new ORANGE_SCALE_COLORS array to cycle bullet colors
            itemRow.setTextColor(ORANGE_SCALE_COLORS[colorIndex % ORANGE_SCALE_COLORS.length]);

            itemRow.setPadding(0, 4, 0, 4);
            container.addView(itemRow);
            colorIndex++;
        }
    }

    private void updateAnalyticsUI(View view, DashboardMetrics metrics) {
        int totalFamilies = metrics.getTotal_families();
        int reliefPacks = metrics.getTotal_packs();
        int totalAffected = metrics.getTotal_affected();

        // Update the "Registered Families" Text View
        TextView tvPercentage = view.findViewById(R.id.tv_percentage);
        if (tvPercentage != null) {
            tvPercentage.setText(String.valueOf(totalFamilies));
        }

        if (totalFamilies == 0) totalFamilies = 1;

        int coveragePercent = (reliefPacks * 100) / totalFamilies;
        int deficit = totalFamilies - reliefPacks;
        if (deficit < 0) deficit = 0;

        ProgressBar progCoverage = view.findViewById(R.id.progress_coverage);
        TextView txtPercent = view.findViewById(R.id.txt_coverage_percent);
        TextView txtInsight = view.findViewById(R.id.txt_coverage_insight);

        if (progCoverage != null) {
            progCoverage.setProgress(coveragePercent);
            txtPercent.setText(coveragePercent + "%");

            if (coveragePercent < 50) {
                progCoverage.setProgressTintList(ColorStateList.valueOf(Color.RED));
                txtInsight.setText("CRITICAL: " + deficit + " families have no allocated packs.");
                txtInsight.setTextColor(Color.RED);
            } else if (coveragePercent < 100) {
                // Use Deep Orange for warning
                progCoverage.setProgressTintList(ColorStateList.valueOf(COLOR_DEEP_ORANGE));
                txtInsight.setText("⚠️ Gap: " + deficit + " more packs needed.");
                txtInsight.setTextColor(COLOR_DEEP_ORANGE);
            } else {
                progCoverage.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                txtInsight.setText("✅ Sufficient Stock. All families covered.");
                txtInsight.setTextColor(Color.parseColor("#2E7D32"));
            }
        }

        int avgDistributionPerDay = 25;
        TextView txtPrediction = view.findViewById(R.id.txt_prediction);
        if (txtPrediction != null) {
            if (reliefPacks == 0) {
                txtPrediction.setText("URGENT: No stock available.");
            } else {
                int daysLeft = reliefPacks / avgDistributionPerDay;
                txtPrediction.setText("Based on avg usage, stocks last " + daysLeft + " days.");
            }
        }

        TextView badge = view.findViewById(R.id.txt_risk_badge);
        TextView txtAffectedView = view.findViewById(R.id.txt_affected_families);
        TextView txtDesc = view.findViewById(R.id.txt_risk_desc);

        if (badge != null) {
            txtAffectedView.setText(totalAffected + " / " + totalFamilies + " Families Applied");

            int riskPercent = (totalAffected * 100) / totalFamilies;

            if (riskPercent >= 50) {
                badge.setText("HIGH RISK");
                badge.setBackgroundColor(Color.parseColor("#FFEBEE"));
                badge.setTextColor(Color.RED);
                txtDesc.setText("⚠️ Major crisis. Immediate action required.");
            } else if (riskPercent >= 20) {
                badge.setText("MODERATE");
                badge.setBackgroundColor(COLOR_PALE_ORANGE); // Use pale orange bg
                badge.setTextColor(COLOR_DEEP_ORANGE);       // Use deep orange text
                txtDesc.setText("⚠️ Significant impact. Monitor closely.");
            } else {
                badge.setText("LOW RISK");
                badge.setBackgroundColor(Color.parseColor("#E8F5E9"));
                badge.setTextColor(Color.parseColor("#2E7D32"));
                txtDesc.setText("Situation stable. Minimal impact.");
            }
        }
    }

    private void updatePieChart(PieChart chart, Map<String, Integer> data) {
        if (data == null || data.isEmpty()) return;
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }
        if (chart.getData() != null && chart.getData().getDataSet() != null) {
            PieDataSet set = (PieDataSet) chart.getData().getDataSet();
            set.setValues(entries);

            // Apply the new expanded colors to the Pie Chart
            set.setColors(ORANGE_SCALE_COLORS);

            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
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
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());
        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            LineDataSet set = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set.setValues(entries);
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }

    private void setupReliefPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        // Empty string label to hide text
        entries.add(new PieEntry(1f, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ORANGE_SCALE_COLORS);

        // ⭐ HIDE TEXT ON CHART (Clean Look)
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        chartRelief.setData(data);

        // ⭐ HIDE LABELS
        chartRelief.setDrawEntryLabels(false);

        chartRelief.getDescription().setEnabled(false);
        chartRelief.getLegend().setEnabled(false);
        chartRelief.setTouchEnabled(false);

        // Donut Style
        chartRelief.setDrawHoleEnabled(true);
        chartRelief.setHoleColor(Color.TRANSPARENT);
        chartRelief.setHoleRadius(50f);
        chartRelief.setTransparentCircleRadius(0f);
    }

    private void setupAffectedPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(1f, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ORANGE_SCALE_COLORS);

        // ⭐ HIDE TEXT ON CHART
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        chartAffected.setData(data);

        // ⭐ HIDE LABELS
        chartAffected.setDrawEntryLabels(false);

        chartAffected.getDescription().setEnabled(false);
        chartAffected.getLegend().setEnabled(false);
        chartAffected.setTouchEnabled(false);

        // Donut Style
        chartAffected.setDrawHoleEnabled(true);
        chartAffected.setHoleColor(Color.TRANSPARENT);
        chartAffected.setHoleRadius(50f);
        chartAffected.setTransparentCircleRadius(0f);
    }

    private void setupFamiliesLineChart() {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Families");
        dataSet.setColor(COLOR_VIBRANT_ORANGE);
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(COLOR_DEEP_ORANGE);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        chartFamilies.setData(data);

        chartFamilies.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartFamilies.getXAxis().setDrawGridLines(false);
        chartFamilies.getAxisRight().setEnabled(false);
        chartFamilies.getDescription().setEnabled(false);
        chartFamilies.getLegend().setEnabled(false);
        chartFamilies.setTouchEnabled(false);
    }

    private void setupDonationTrendsChart() {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Donations");
        dataSet.setColor(COLOR_TEAL);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(COLOR_TEAL);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(COLOR_TEAL);
        dataSet.setFillAlpha(30);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        chartDonations.setData(data);

        chartDonations.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartDonations.getXAxis().setDrawGridLines(true);
        chartDonations.getXAxis().setGridColor(Color.parseColor("#EEEEEE"));
        chartDonations.getAxisRight().setEnabled(false);
        chartDonations.getDescription().setEnabled(false);
        chartDonations.getLegend().setEnabled(false);
        chartDonations.setTouchEnabled(false);
    }
}