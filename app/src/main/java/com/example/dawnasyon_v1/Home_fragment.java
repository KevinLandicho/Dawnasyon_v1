package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class Home_fragment extends BaseFragment {

    // --- Carousel Fields ---
    private ViewPager2 imageCarouselViewPager;
    private ImageCarouselAdapter carouselAdapter;
    private Handler sliderHandler = new Handler();
    private final int SLIDE_INTERVAL_MS = 3000; // 3 seconds

    // --- Announcement Fields ---
    private RecyclerView announcementRecyclerView;
    private AnnouncementAdapter announcementAdapter;

    // Runnable for the auto-sliding mechanism
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (imageCarouselViewPager != null && carouselAdapter != null) {
                int currentItem = imageCarouselViewPager.getCurrentItem();
                int totalItems = carouselAdapter.getItemCount();
                int nextItem = (currentItem + 1) % totalItems;
                imageCarouselViewPager.setCurrentItem(nextItem, true);
            }
            sliderHandler.postDelayed(this, SLIDE_INTERVAL_MS);
        }
    };

    public Home_fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Initialize Views
        imageCarouselViewPager = view.findViewById(R.id.image_carousel_view_pager);
        announcementRecyclerView = view.findViewById(R.id.announcement_recycler_view);

        // 2. Setup Carousel
        setupCarousel();

        // 3. Setup Announcements List
        setupAnnouncementsList();

        return view;
    }

    // -------------------------------------------------------------------
    // --- CAROUSEL LOGIC ---
    // -------------------------------------------------------------------

    private void setupCarousel() {
        List<Integer> imageList = new ArrayList<>();
        imageList.add(R.drawable.una);
        imageList.add(R.drawable.pangalawa);
        imageList.add(R.drawable.pangatlo);
        imageList.add(R.drawable.pangapat);
        imageList.add(R.drawable.panglima);

        carouselAdapter = new ImageCarouselAdapter(imageList);
        imageCarouselViewPager.setAdapter(carouselAdapter);
    }

    // -------------------------------------------------------------------
    // --- ANNOUNCEMENT LIST LOGIC (UPDATED) ---
    // -------------------------------------------------------------------

    private void setupAnnouncementsList() {
        announcementRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Announcement> sampleAnnouncements = createSampleAnnouncements();

        // [UPDATED] Initialize Adapter with the Click Listener
        announcementAdapter = new AnnouncementAdapter(sampleAnnouncements, new AnnouncementAdapter.OnApplyClickListener() {
            @Override
            public void onApplyClick(Announcement announcement) {
                // When the button inside the adapter is clicked, this runs:
                showApplyDialog(announcement);
            }
        });

        announcementRecyclerView.setAdapter(announcementAdapter);
    }

    /**
     * [NEW] Helper method to show the ApplyConfirmationDialogFragment
     */
    private void showApplyDialog(Announcement announcement) {
        // Create the dialog instance
        ApplyConfirmationDialogFragment dialog = new ApplyConfirmationDialogFragment();

        // Show the dialog using the parent fragment manager
        dialog.show(getParentFragmentManager(), "ApplyDialog");
    }

    /**
     * Helper method to generate dummy data for testing the RecyclerView.
     */
    private List<Announcement> createSampleAnnouncements() {
        List<Announcement> announcements = new ArrayList<>();
        int placeholderImage = R.drawable.ic_image_placeholder; // Ensure this exists

        announcements.add(new Announcement(
                "Urgent Food Drive in Brgy. 143",
                "Thursday, 11:30AM",
                "We are calling for volunteers to help sort and pack relief goods...",
                placeholderImage
        ));

        announcements.add(new Announcement(
                "Medicine Donations Needed",
                "Friday, 2:00PM",
                "The health center requires immediate donations of pain relievers...",
                placeholderImage
        ));

        announcements.add(new Announcement(
                "Call for Hygiene Kit Volunteers",
                "Saturday, 9:00AM",
                "Assist in distributing hygiene kits door-to-door in vulnerable areas...",
                placeholderImage
        ));

        return announcements;
    }

    // -------------------------------------------------------------------
    // --- LIFECYCLE MANAGEMENT ---
    // -------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
        if (carouselAdapter != null) {
            sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sliderHandler.removeCallbacksAndMessages(null);
    }
}