package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.dawnasyon_v1.R;
import java.util.ArrayList;
import java.util.List;

public class Home_fragment extends Fragment {

    // --- Carousel Fields ---
    private ViewPager2 imageCarouselViewPager;
    private ImageCarouselAdapter carouselAdapter;
    private Handler sliderHandler = new Handler();
    private final int SLIDE_INTERVAL_MS = 5000; // 5 seconds

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

        // Inflate the layout (R.layout.fragment_home is your main layout)
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
        // Prepare Data: List of your 5 images (must be in res/drawable)
        List<Integer> imageList = new ArrayList<>();
        imageList.add(R.drawable.una);
        imageList.add(R.drawable.pangalawa);
        imageList.add(R.drawable.pangatlo);
        imageList.add(R.drawable.pangapat);
        imageList.add(R.drawable.panglima);

        // Setup the Carousel Adapter
        carouselAdapter = new ImageCarouselAdapter(imageList);
        imageCarouselViewPager.setAdapter(carouselAdapter);
    }

    // -------------------------------------------------------------------
    // --- ANNOUNCEMENT LIST LOGIC ---
    // -------------------------------------------------------------------

    private void setupAnnouncementsList() {
        // Set a Layout Manager (Vertical scroll is standard)
        // Ensure you have R.id.announcement_recycler_view in your fragment_home.xml
        announcementRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create Sample Announcement Data
        List<Announcement> sampleAnnouncements = createSampleAnnouncements();

        // Setup the Announcement Adapter and attach it
        announcementAdapter = new AnnouncementAdapter(sampleAnnouncements);
        announcementRecyclerView.setAdapter(announcementAdapter);
    }

    /**
     * Helper method to generate dummy data for testing the RecyclerView.
     */
    private List<Announcement> createSampleAnnouncements() {
        List<Announcement> announcements = new ArrayList<>();

        // Use R.drawable.ic_image_placeholder (must exist)
        int placeholderImage = R.drawable.ic_image_placeholder;

        announcements.add(new Announcement(
                "Urgent Food Drive in Brgy. 143",
                "Thursday, 11:30AM",
                "We are calling for volunteers to help sort and pack relief goods for the communities affected by the recent flooding...",
                placeholderImage
        ));

        announcements.add(new Announcement(
                "Medicine Donations Needed",
                "Friday, 2:00PM",
                "The health center requires immediate donations of pain relievers, vitamins, and basic first aid supplies.",
                placeholderImage
        ));

        announcements.add(new Announcement(
                "Call for Hygiene Kit Volunteers",
                "Saturday, 9:00AM",
                "Assist in distributing hygiene kits door-to-door in vulnerable areas this weekend.",
                placeholderImage
        ));

        return announcements;
    }

    // -------------------------------------------------------------------
    // --- LIFECYCLE MANAGEMENT FOR AUTO-SWIPE ---
    // -------------------------------------------------------------------

    // Start the auto-slide mechanism when the fragment comes into view
    @Override
    public void onResume() {
        super.onResume();
        if (carouselAdapter != null) {
            sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);
        }
    }

    // Stop the auto-slide when the user leaves the fragment
    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    // Clean up to prevent memory leaks
    @Override
    public void onDestroy() {
        super.onDestroy();
        sliderHandler.removeCallbacksAndMessages(null);
    }
}