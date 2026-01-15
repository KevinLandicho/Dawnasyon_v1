package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class Home_fragment extends BaseFragment {

    private TextView welcomeText;
    private SearchView searchView;

    // Carousel
    private ViewPager2 imageCarouselViewPager;
    private ImageCarouselAdapter carouselAdapter;
    private Handler sliderHandler = new Handler();
    private final int SLIDE_INTERVAL_MS = 3000;

    // Announcement Lists
    private RecyclerView announcementRecyclerView;
    private AnnouncementAdapter announcementAdapter;

    private List<Announcement> announcementList = new ArrayList<>();
    private List<Announcement> fullAnnouncementList = new ArrayList<>();

    // ⭐ NEW: Store verification status
    private boolean isUserVerified = false;

    // Carousel Auto-scroll Runnable
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (imageCarouselViewPager != null && carouselAdapter != null) {
                int currentItem = imageCarouselViewPager.getCurrentItem();
                int totalItems = carouselAdapter.getItemCount();
                if (totalItems > 0) {
                    int nextItem = (currentItem + 1) % totalItems;
                    imageCarouselViewPager.setCurrentItem(nextItem, true);
                }
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

        // Bind Views
        welcomeText = view.findViewById(R.id.welcome_text);
        searchView = view.findViewById(R.id.search_view);
        imageCarouselViewPager = view.findViewById(R.id.image_carousel_view_pager);
        announcementRecyclerView = view.findViewById(R.id.announcement_recycler_view);

        // Setup Logic
        loadUserProfile();
        setupCarousel();
        setupAnnouncementsList();
        setupSearch();

        // Fetch Data
        fetchAnnouncementsFromSupabase();

        return view;
    }

    private void loadUserProfile() {
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                // 1. Update Welcome Text
                welcomeText.setText("Welcome, " + profile.getFull_name() + "!");

                // 2. ⭐ CAPTURE VERIFICATION STATUS
                // Boolean.TRUE.equals handles null safety (if verified is null, it returns false)
                isUserVerified = Boolean.TRUE.equals(profile.getVerified());
            }
            return null;
        });
    }

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

    private void setupAnnouncementsList() {
        announcementRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        announcementAdapter = new AnnouncementAdapter(announcementList, this::showApplyDialog);
        announcementRecyclerView.setAdapter(announcementAdapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterAnnouncements(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAnnouncements(newText);
                return true;
            }
        });
    }

    private void filterAnnouncements(String query) {
        if (query == null || query.isEmpty()) {
            announcementAdapter.updateData(new ArrayList<>(fullAnnouncementList));
            return;
        }

        List<Announcement> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();

        for (Announcement item : fullAnnouncementList) {
            boolean matchesTitle = item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery);
            boolean matchesBody = item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseQuery);

            if (matchesTitle || matchesBody) {
                filteredList.add(item);
            }
        }
        announcementAdapter.updateData(filteredList);
    }

    private void fetchAnnouncementsFromSupabase() {
        SupabaseJavaHelper.fetchAnnouncements(new AnnouncementCallback() {
            @Override
            public void onSuccess(List<? extends Announcement> data) {
                if (isAdded()) {
                    announcementList.clear();
                    announcementList.addAll(data);

                    fullAnnouncementList.clear();
                    fullAnnouncementList.addAll(data);

                    announcementAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded()) {
                    Log.e("HomeFragment", "Fetch Error: " + message);
                    Toast.makeText(getContext(), "Failed to load announcements", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showApplyDialog(Announcement announcement) {
        // ⭐ FIX: BLOCK ACTION IF NOT VERIFIED
        if (!isUserVerified) {
            Toast.makeText(getContext(), "🔒 You must verify your account to apply.", Toast.LENGTH_LONG).show();
            return; // Stop here, do not show dialog
        }

        // 1. Create the dialog
        ApplyConfirmationDialogFragment dialog = new ApplyConfirmationDialogFragment();

        // 2. Define what happens when user clicks "Confirm"
        dialog.setOnConfirmListener(() -> {

            if (announcement.getLinkedDriveId() == null) {
                Toast.makeText(getContext(), "Error: This drive is not linked properly.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Call the Helper to save to Database
            SupabaseJavaHelper.applyToDrive(announcement.getLinkedDriveId(), new ApplicationCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        dialog.dismiss();

                        ApplicationSuccessDialogFragment successDialog = new ApplicationSuccessDialogFragment();
                        successDialog.show(getParentFragmentManager(), "SuccessDialog");
                    }
                }

                @Override
                public void onError(@NonNull String message) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed: " + message, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });
        });

        // 4. Show the dialog
        dialog.show(getParentFragmentManager(), "ApplyDialog");
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);
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