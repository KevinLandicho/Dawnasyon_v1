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
    private ImageView iconFilter;

    private String currentFilter = "all";

    // --- COLOR PALETTE ---
    private final int COLOR_DEEP_ORANGE = Color.parseColor("#E65100");
    private final int COLOR_VIBRANT_ORANGE = Color.parseColor("#F5901A");
    private final int COLOR_MED_ORANGE = Color.parseColor("#FFB74D");
    private final int COLOR_SOFT_ORANGE = Color.parseColor("#FFCC80");
    private final int COLOR_LIGHT_ORANGE = Color.parseColor("#FFE0B2");
    private final int COLOR_PALE_ORANGE = Color.parseColor("#FFF3E0");
    private final int COLOR_TEAL = Color.parseColor("#27869B");

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

        // Setup Charts
        setupReliefPieChart();
        setupAffectedPieChart();
        setupFamiliesLineChart();
        setupDonationTrendsChart();
        setupImpactRadarChart();

        View.OnClickListener mapClickListener = v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LiveMap_fragment())
                    .addToBackStack(null)
                    .commit();
        };

        if (btnLiveMap != null) btnLiveMap.setOnClickListener(mapClickListener);
        if (chartAffected != null) chartAffected.setOnClickListener(mapClickListener);

        if (iconFilter != null) {
            iconFilter.setOnClickListener(this::showFilterMenu);
        }

        loadRealData(view, currentFilter);
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

    private void loadRealData(View view, String filterType) {
        if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).showLoading();

        SupabaseJavaHelper.fetchDashboardData(filterType, new SupabaseJavaHelper.DashboardCallback() {
            @Override
            public void onDataLoaded(Map<String, Integer> inventory, Map<String, Integer> areas, Map<String, Float> donations, Map<String, Integer> families, DashboardMetrics metrics) {
                if (isAdded() && getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                if (!isAdded()) return;

                updatePieChart(chartRelief, inventory);
                updatePieChart(chartAffected, areas);
                updateLineChart(chartDonations, donations);

                Map<String, Float> familiesFloat = new HashMap<>();
                if (families != null) {
                    for (Map.Entry<String, Integer> entry : families.entrySet()) {
                        familiesFloat.put(entry.getKey(), entry.getValue().floatValue());
                    }
                }
                updateLineChart(chartFamilies, familiesFloat);

                updateListUI(llReliefList, inventory, "Item");
                updateListUI(llAffectedList, areas, "Street");
                updateAnalyticsUI(view, metrics);
                updateRadarChart(areas);
            }

            @Override
            public void onError(String message) {
                if (isAdded() && getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                if (isAdded()) Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- CHART METHODS ---

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
        if (data == null || data.isEmpty()) return;
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

    private void updateListUI(LinearLayout container, Map<String, Integer> data, String labelTitle) {
        if (container == null) return;
        container.removeAllViews();
        TextView header = new TextView(getContext());
        header.setText(labelTitle + "          Count");
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(header);

        if (data == null || data.isEmpty()) return;
        int colorIndex = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            TextView itemRow = new TextView(getContext());
            itemRow.setText("● " + entry.getKey() + "   " + entry.getValue());
            itemRow.setTextColor(ORANGE_SCALE_COLORS[colorIndex % ORANGE_SCALE_COLORS.length]);
            container.addView(itemRow);
            colorIndex++;
        }
    }

    private void updateAnalyticsUI(View view, DashboardMetrics metrics) {
        int totalFamilies = metrics.getTotal_families();
        int reliefPacks = metrics.getTotal_packs();
        int totalAffected = metrics.getTotal_affected();

        TextView tvPercentage = view.findViewById(R.id.tv_percentage);
        if (tvPercentage != null) tvPercentage.setText(String.valueOf(totalFamilies));

        if (totalFamilies == 0) totalFamilies = 1;
        int coveragePercent = (reliefPacks * 100) / totalFamilies;
        int deficit = Math.max(0, totalFamilies - reliefPacks);

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
                txtInsight.setText("✅ Sufficient Stock.");
                txtInsight.setTextColor(Color.parseColor("#2E7D32"));
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

            // ⭐ FIX: Force text off
            set.setDrawValues(false);

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
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        // ⭐ FIX: Ensure X-Axis Granularity so labels don't repeat (Jan Jan Jan)
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

    private void setupReliefPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(1f, ""));
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ORANGE_SCALE_COLORS);
        PieData data = new PieData(dataSet);
        chartRelief.setData(data);

        // ⭐ FIX: Hide labels
        chartRelief.setDrawEntryLabels(false);
        dataSet.setDrawValues(false);

        chartRelief.getDescription().setEnabled(false);
        chartRelief.getLegend().setEnabled(false);
    }

    private void setupAffectedPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(1f, ""));
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ORANGE_SCALE_COLORS);
        PieData data = new PieData(dataSet);
        chartAffected.setData(data);

        // ⭐ FIX: Hide labels
        chartAffected.setDrawEntryLabels(false);
        dataSet.setDrawValues(false);

        chartAffected.getDescription().setEnabled(false);
        chartAffected.getLegend().setEnabled(false);
    }

    private void setupFamiliesLineChart() {
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Families");
        dataSet.setColor(COLOR_VIBRANT_ORANGE);
        dataSet.setLineWidth(3f);

        // ⭐ FIX: Hide Grid Lines (Removes multiple lines)
        XAxis xAxis = chartFamilies.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        // ⭐ FIX: Prevent repeating labels
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

        // ⭐ FIX: Hide Grid Lines (Removes multiple lines)
        XAxis xAxis = chartDonations.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        // ⭐ FIX: Prevent repeating labels
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        chartDonations.getAxisLeft().setDrawGridLines(false);
        chartDonations.getAxisRight().setDrawGridLines(false);

        LineData data = new LineData(dataSet);
        chartDonations.setData(data);
        chartDonations.getDescription().setEnabled(false);
    }
}