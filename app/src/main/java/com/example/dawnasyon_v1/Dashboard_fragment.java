package com.example.dawnasyon_v1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
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

// MPAndroidChart Imports
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.RadarChart;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dashboard_fragment extends BaseFragment {

    private PieChart chartRelief;
    private PieChart chartAffected;
    private LineChart chartFamilies;
    private LineChart chartDonations;
    private RadarChart chartImpact;

    private LinearLayout llReliefList;
    private LinearLayout llAffectedList;

    private TextView tvImpactCount;
    private ImageView iconFilter; // Filter Icon

    // Default Filter
    private String currentFilter = "all"; // "all", "monthly", "yearly"

    // --- COLOR PALETTE ---
    private final int COLOR_DEEP_ORANGE = Color.parseColor("#E65100");
    private final int COLOR_VIBRANT_ORANGE = Color.parseColor("#F5901A");
    private final int COLOR_MED_ORANGE = Color.parseColor("#FFB74D");
    private final int COLOR_SOFT_ORANGE = Color.parseColor("#FFCC80");
    private final int COLOR_LIGHT_ORANGE = Color.parseColor("#FFE0B2");
    private final int COLOR_PALE_ORANGE = Color.parseColor("#FFF3E0");
    private final int COLOR_TEAL = Color.parseColor("#27869B");

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
        chartImpact = view.findViewById(R.id.chart_donation_impact);

        tvImpactCount = view.findViewById(R.id.tv_impact_count);
        iconFilter = view.findViewById(R.id.icon_filter);

        Button btnLiveMap = view.findViewById(R.id.btn_live_map);

        llReliefList = view.findViewById(R.id.ll_relief_list);
        llAffectedList = view.findViewById(R.id.ll_affected_list);

        // Initial Chart Setup
        setupReliefPieChart();
        setupAffectedPieChart();
        setupFamiliesLineChart();
        setupDonationTrendsChart();
        setupImpactRadarChart();

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

        // Filter Click Listener
        if (iconFilter != null) {
            iconFilter.setOnClickListener(this::showFilterMenu);
        }

        // Load Initial Data
        loadRealData(view, currentFilter);
    }

    // ⭐ POPUP MENU LOGIC
    private void showFilterMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenu().add(0, 0, 0, "All Time");
        popup.getMenu().add(0, 1, 1, "Monthly (This Month)");
        popup.getMenu().add(0, 2, 2, "Yearly (This Year)");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 0:
                    currentFilter = "all";
                    Toast.makeText(getContext(), "Filter: All Time", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    currentFilter = "monthly";
                    Toast.makeText(getContext(), "Filter: Monthly", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    currentFilter = "yearly";
                    Toast.makeText(getContext(), "Filter: Yearly", Toast.LENGTH_SHORT).show();
                    break;
            }
            // Reload with new filter
            if (getView() != null) {
                loadRealData(getView(), currentFilter);
            }
            return true;
        });
        popup.show();
    }

    private void loadRealData(View view, String filterType) {

        // 1. SHOW LOADING
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).showLoading();
        }

        SupabaseJavaHelper.fetchDashboardData(filterType, new DashboardCallback() {
            @Override
            public void onDataLoaded(Map<String, Integer> inventory,
                                     Map<String, Integer> areas,
                                     Map<String, Float> donations,
                                     Map<String, Integer> families,
                                     DashboardMetrics metrics) {

                // 2. HIDE LOADING ON SUCCESS
                if (isAdded() && getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }

                if (!isAdded()) return;

                // A. Update Standard Charts
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

                // C. Update Analytics
                updateAnalyticsUI(view, metrics);

                // D. Update Impact Radar Chart
                updateRadarChart(areas);
            }

            @Override
            public void onError(String message) {

                // 3. HIDE LOADING ON ERROR
                if (isAdded() && getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }

                if (isAdded()) {
                    Toast.makeText(getContext(), "Error loading dashboard: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // --- CHART SETUP & UPDATE METHODS ---

    private void setupImpactRadarChart() {
        chartImpact.getDescription().setEnabled(false);
        chartImpact.setWebLineWidth(1f);
        chartImpact.setWebColor(Color.LTGRAY);
        chartImpact.setWebLineWidthInner(1f);
        chartImpact.setWebColorInner(Color.LTGRAY);
        chartImpact.setWebAlpha(100);
        chartImpact.getLegend().setEnabled(false);
        chartImpact.getYAxis().setEnabled(false);

        XAxis xAxis = chartImpact.getXAxis();
        xAxis.setTextSize(9f);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{}));
        xAxis.setTextColor(Color.DKGRAY);
    }

    private void updateRadarChart(Map<String, Integer> data) {
        if (data == null || data.isEmpty()) return;

        List<RadarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int totalHelped = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            entries.add(new RadarEntry(entry.getValue()));
            labels.add(entry.getKey());
            totalHelped += entry.getValue();
        }

        if (tvImpactCount != null) {
            tvImpactCount.setText(String.valueOf(totalHelped));
        }

        RadarDataSet set = new RadarDataSet(entries, "Impact");
        set.setColor(COLOR_VIBRANT_ORANGE);
        set.setFillColor(COLOR_SOFT_ORANGE);
        set.setDrawFilled(true);
        set.setFillAlpha(100);
        set.setLineWidth(2f);
        set.setDrawHighlightCircleEnabled(true);
        set.setDrawHighlightIndicators(false);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.BLACK);
        set.setDrawValues(true);

        RadarData radarData = new RadarData(set);
        XAxis xAxis = chartImpact.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        chartImpact.setData(radarData);
        chartImpact.invalidate();
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

        int colorIndex = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            TextView itemRow = new TextView(getContext());
            itemRow.setText("● " + entry.getKey() + "   " + entry.getValue());
            itemRow.setTextSize(12);
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
                badge.setBackgroundColor(COLOR_PALE_ORANGE);
                badge.setTextColor(COLOR_DEEP_ORANGE);
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
        entries.add(new PieEntry(1f, ""));
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ORANGE_SCALE_COLORS);
        dataSet.setDrawValues(false);
        PieData data = new PieData(dataSet);
        chartRelief.setData(data);
        chartRelief.setDrawEntryLabels(false);
        chartRelief.getDescription().setEnabled(false);
        chartRelief.getLegend().setEnabled(false);
        chartRelief.setTouchEnabled(false);
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
        dataSet.setDrawValues(false);
        PieData data = new PieData(dataSet);
        chartAffected.setData(data);
        chartAffected.setDrawEntryLabels(false);
        chartAffected.getDescription().setEnabled(false);
        chartAffected.getLegend().setEnabled(false);
        chartAffected.setTouchEnabled(false);
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