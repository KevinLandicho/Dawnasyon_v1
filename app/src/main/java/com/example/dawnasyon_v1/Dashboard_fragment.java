package com.example.dawnasyon_v1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import java.util.List;

public class Dashboard_fragment extends BaseFragment {

    private PieChart chartRelief;
    private PieChart chartAffected;
    private LineChart chartFamilies;
    private LineChart chartDonations;

    // Define your Brand Colors
    private final int COLOR_DARK_ORANGE = Color.parseColor("#F5901A");
    private final int COLOR_MED_ORANGE = Color.parseColor("#FFCC80");
    private final int COLOR_LIGHT_ORANGE = Color.parseColor("#FFE0B2");
    private final int COLOR_TEAL = Color.parseColor("#27869B");

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

        chartRelief = view.findViewById(R.id.chart_relief_status);
        chartAffected = view.findViewById(R.id.chart_affected_areas);
        chartFamilies = view.findViewById(R.id.chart_registered_families);
        chartDonations = view.findViewById(R.id.chart_donation_trends);

        // 1. Setup Charts
        setupReliefPieChart();
        setupAffectedPieChart();
        setupFamiliesLineChart();
        setupDonationTrendsChart();

        // 2. Setup AI Analytics (The New Feature: Predictions + Risk Index)
        setupAnalyticsInsights(view);

        // 3. Connect Map Click Listener
        // Note: chartAffected.setTouchEnabled(false) is set in setupAffectedPieChart
        // This ensures the click passes through to this listener
        chartAffected.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new LiveMap_fragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    // ⭐ NEW METHOD: AI ANALYTICS LOGIC ⭐
    private void setupAnalyticsInsights(View view) {
        // --- 1. DATA INPUTS (Simulation) ---
        // In the future, these numbers will come from your SQL Database
        int totalFamilies = 500;
        int reliefPacksAvailable = 130;
        int avgDistributionPerDay = 25;

        // Data for Risk Index (Matches your Affected Areas Chart)
        int familiesInFloodZone = 150;
        int familiesInFireZone = 100;
        int totalAffected = familiesInFloodZone + familiesInFireZone;

        // --- 2. LOGIC: Supply Coverage Analysis ---
        int coveragePercent = (reliefPacksAvailable * 100) / totalFamilies;
        int deficit = totalFamilies - reliefPacksAvailable;

        ProgressBar progCoverage = view.findViewById(R.id.progress_coverage);
        TextView txtPercent = view.findViewById(R.id.txt_coverage_percent);
        TextView txtInsight = view.findViewById(R.id.txt_coverage_insight);

        if (progCoverage != null) {
            progCoverage.setProgress(coveragePercent);
            txtPercent.setText(coveragePercent + "%");

            if (coveragePercent < 50) {
                // Critical (Red)
                progCoverage.setProgressTintList(ColorStateList.valueOf(Color.RED));
                txtInsight.setText("CRITICAL: " + deficit + " families have no allocated packs.");
                txtInsight.setTextColor(Color.RED);
            } else if (coveragePercent < 100) {
                // Warning (Orange)
                progCoverage.setProgressTintList(ColorStateList.valueOf(COLOR_DARK_ORANGE));
                txtInsight.setText("⚠️ Gap: " + deficit + " more packs needed for 100% coverage.");
                txtInsight.setTextColor(Color.parseColor("#E65100"));
            } else {
                // Good (Green)
                progCoverage.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                txtInsight.setText("✅ Sufficient Stock. All families covered.");
                txtInsight.setTextColor(Color.parseColor("#2E7D32"));
            }
        }

        // --- 3. LOGIC: Predictive Modeling (Days Remaining) ---
        TextView txtPrediction = view.findViewById(R.id.txt_prediction);
        if (txtPrediction != null) {
            int daysLeft = reliefPacksAvailable / avgDistributionPerDay;
            if (daysLeft < 3) {
                txtPrediction.setText("URGENT: Stock will run out in " + daysLeft + " days at current rate.");
            } else {
                txtPrediction.setText("Stable: Supplies good for " + daysLeft + " days.");
            }
        }

        // --- 4. LOGIC: Real-Time Risk Index ---
        TextView badge = view.findViewById(R.id.txt_risk_badge);
        TextView txtAffected = view.findViewById(R.id.txt_affected_families);
        TextView txtDesc = view.findViewById(R.id.txt_risk_desc);

        if (badge != null) {
            int percentAffected = (totalAffected * 100) / totalFamilies;
            txtAffected.setText(totalAffected + " / " + totalFamilies + " Families Affected");

            if (percentAffected >= 50) {
                // HIGH RISK
                badge.setText("CRITICAL LEVEL");
                badge.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red BG
                badge.setTextColor(Color.RED);
                txtDesc.setText("⚠️ " + percentAffected + "% of population requires immediate evacuation.");
            } else if (percentAffected >= 20) {
                // MODERATE
                badge.setText("MODERATE RISK");
                badge.setBackgroundColor(Color.parseColor("#FFF3E0")); // Light Orange BG
                badge.setTextColor(Color.parseColor("#E65100"));
                txtDesc.setText("⚠️ " + percentAffected + "% of population affected. Monitor closely.");
            } else {
                // LOW
                badge.setText("LOW RISK");
                badge.setBackgroundColor(Color.parseColor("#E8F5E9")); // Light Green BG
                badge.setTextColor(Color.parseColor("#2E7D32"));
                txtDesc.setText("Situation stable. Minimal impact.");
            }
        }
    }

    // --- CHART SETUP METHODS ---

    private void setupReliefPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(170f, ""));
        entries.add(new PieEntry(58f, ""));
        entries.add(new PieEntry(11f, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{COLOR_DARK_ORANGE, COLOR_MED_ORANGE, COLOR_LIGHT_ORANGE});
        dataSet.setSliceSpace(3f);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        chartRelief.setData(data);

        chartRelief.setHoleRadius(65f);
        chartRelief.setTransparentCircleRadius(0f);
        chartRelief.setHoleColor(Color.TRANSPARENT);
        chartRelief.getDescription().setEnabled(false);
        chartRelief.getLegend().setEnabled(false);
        chartRelief.setTouchEnabled(false);
        chartRelief.animateY(1000);
        chartRelief.invalidate();
    }

    private void setupAffectedPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(150f, ""));
        entries.add(new PieEntry(100f, ""));
        entries.add(new PieEntry(25f, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{COLOR_DARK_ORANGE, COLOR_MED_ORANGE, COLOR_LIGHT_ORANGE});
        dataSet.setSliceSpace(3f);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        chartAffected.setData(data);

        chartAffected.setHoleRadius(65f);
        chartAffected.setHoleColor(Color.TRANSPARENT);
        chartAffected.getDescription().setEnabled(false);
        chartAffected.getLegend().setEnabled(false);

        // ⭐ IMPORTANT: Disable touch so click passes to the Listener
        chartAffected.setTouchEnabled(false);

        chartAffected.animateY(1000);
        chartAffected.invalidate();
    }

    private void setupFamiliesLineChart() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0f, 20f));
        entries.add(new Entry(1f, 32f));
        entries.add(new Entry(2f, 45f));
        entries.add(new Entry(3f, 88f));

        LineDataSet dataSet = new LineDataSet(entries, "Families");
        dataSet.setColor(COLOR_DARK_ORANGE);
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(COLOR_DARK_ORANGE);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(false);

        LineData data = new LineData(dataSet);
        chartFamilies.setData(data);

        XAxis xAxis = chartFamilies.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);

        chartFamilies.getAxisRight().setEnabled(false);
        chartFamilies.getAxisLeft().setEnabled(true);
        chartFamilies.getAxisLeft().setDrawGridLines(true);
        chartFamilies.getAxisLeft().setGridColor(Color.parseColor("#EEEEEE"));
        chartFamilies.getDescription().setEnabled(false);
        chartFamilies.getLegend().setEnabled(false);
        chartFamilies.setTouchEnabled(false);
        chartFamilies.animateX(1500);
        chartFamilies.invalidate();
    }

    private void setupDonationTrendsChart() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0f, 95f)); // Jan
        entries.add(new Entry(1f, 65f)); // Feb
        entries.add(new Entry(2f, 80f)); // Mar
        entries.add(new Entry(3f, 35f)); // Apr
        entries.add(new Entry(4f, 55f)); // Jun

        LineDataSet dataSet = new LineDataSet(entries, "Donations");
        dataSet.setColor(COLOR_TEAL);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(COLOR_TEAL);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(COLOR_TEAL);
        dataSet.setFillAlpha(30);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        chartDonations.setData(data);

        XAxis xAxis = chartDonations.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#EEEEEE"));

        final String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "Jun"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setGranularity(1f);

        chartDonations.getAxisRight().setEnabled(false);
        chartDonations.getAxisLeft().setDrawGridLines(true);
        chartDonations.getAxisLeft().setGridColor(Color.parseColor("#EEEEEE"));
        chartDonations.getDescription().setEnabled(false);
        chartDonations.getLegend().setEnabled(false);
        chartDonations.setTouchEnabled(false);
        chartDonations.animateX(1500);
        chartDonations.invalidate();
    }
}