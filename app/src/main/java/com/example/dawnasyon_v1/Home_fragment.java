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
    private ViewPager2 imageCarouselViewPager;
    private ImageCarouselAdapter carouselAdapter;
    private Handler sliderHandler = new Handler();
    private final int SLIDE_INTERVAL_MS = 3000;

    private RecyclerView announcementRecyclerView;
    private AnnouncementAdapter announcementAdapter;
    private TextView tvEmptyPlaceholder;

    private List<Announcement> announcementList = new ArrayList<>();
    private List<Announcement> fullAnnouncementList = new ArrayList<>();

    private boolean isUserVerified = false;
    private String userType = "Resident";

    // ⭐ NEW: Flag to control loading indicator behavior
    private boolean isFirstLoad = true;

    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (imageCarouselViewPager != null && carouselAdapter != null) {
                int currentItem = imageCarouselViewPager.getCurrentItem();
                int totalItems = carouselAdapter.getItemCount();
                if (totalItems > 0) {
                    imageCarouselViewPager.setCurrentItem((currentItem + 1) % totalItems, true);
                }
            }
            sliderHandler.postDelayed(this, SLIDE_INTERVAL_MS);
        }
    };

    public Home_fragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        welcomeText = view.findViewById(R.id.welcome_text);
        searchView = view.findViewById(R.id.search_view);
        imageCarouselViewPager = view.findViewById(R.id.image_carousel_view_pager);
        announcementRecyclerView = view.findViewById(R.id.announcement_recycler_view);
        tvEmptyPlaceholder = view.findViewById(R.id.tv_empty_placeholder);

        // Setup UI (But DO NOT load data yet)
        setupCarousel();
        setupAnnouncementsList();
        setupSearch();

        return view;
    }

    // ⭐ MOVED DATA LOADING TO ONRESUME
    // This ensures that even if Auth takes a second to load,
    // the fragment refreshes the data as soon as it appears.
    @Override
    public void onResume() {
        super.onResume();

        // 1. Restart Slider
        sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);

        // 2. Load User Profile (Fixes "Welcome User" missing)
        loadUserProfile();

        // 3. Load Announcements (Fixes Likes/Bookmarks missing)
        fetchAnnouncementsFromSupabase();
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

    private void loadUserProfile() {
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                welcomeText.setText("Welcome, " + profile.getFull_name() + "!");
                isUserVerified = Boolean.TRUE.equals(profile.getVerified());
                if (profile.getType() != null) userType = profile.getType();
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
        announcementAdapter = new AnnouncementAdapter(announcementList, new AnnouncementAdapter.OnItemClickListener() {
            @Override
            public void onApplyClick(Announcement announcement) {
                showApplyDialog(announcement);
            }
            @Override
            public void onLikeClick(Announcement announcement, int position) {
                handleLike(announcement, position);
            }
            @Override
            public void onBookmarkClick(Announcement announcement, int position) {
                handleBookmark(announcement, position);
            }
        });
        announcementRecyclerView.setAdapter(announcementAdapter);
    }

    private void handleLike(Announcement item, int position) {
        boolean currentState = item.isLiked();
        boolean newState = !currentState;
        item.setLiked(newState);
        int currentCount = item.getLikeCount();
        item.setLikeCount(newState ? currentCount + 1 : Math.max(0, currentCount - 1));
        announcementAdapter.notifyItemChanged(position);

        SupabaseJavaHelper.toggleLike(item.getPostId(), newState, new SupabaseJavaHelper.SimpleCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(String msg) {
                if (isAdded()) {
                    item.setLiked(currentState);
                    item.setLikeCount(currentCount);
                    announcementAdapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Failed: " + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleBookmark(Announcement item, int position) {
        boolean currentState = item.isBookmarked();
        boolean newState = !currentState;
        item.setBookmarked(newState);
        announcementAdapter.notifyItemChanged(position);
        Toast.makeText(getContext(), newState ? "Saved" : "Removed", Toast.LENGTH_SHORT).show();

        SupabaseJavaHelper.toggleBookmark(item.getPostId(), newState, new SupabaseJavaHelper.SimpleCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(String msg) {
                if (isAdded()) {
                    item.setBookmarked(currentState);
                    announcementAdapter.notifyItemChanged(position);
                }
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { filterAnnouncements(query); return true; }
            @Override
            public boolean onQueryTextChange(String newText) { filterAnnouncements(newText); return true; }
        });
    }

    private void filterAnnouncements(String query) {
        if (query == null || query.isEmpty()) {
            announcementAdapter.updateData(new ArrayList<>(fullAnnouncementList));
            updatePlaceholder(fullAnnouncementList.isEmpty());
            return;
        }
        List<Announcement> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();
        for (Announcement item : fullAnnouncementList) {
            boolean matchesTitle = item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery);
            boolean matchesBody = item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseQuery);
            if (matchesTitle || matchesBody) filteredList.add(item);
        }
        announcementAdapter.updateData(filteredList);
        updatePlaceholder(filteredList.isEmpty());
    }

    private void fetchAnnouncementsFromSupabase() {
        // ⭐ OPTIMIZATION: Only show blocking loader on the very first load
        // Subsequent refreshes (tabs) happen silently in background
        if (isFirstLoad && getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).showLoading();
        }

        SupabaseJavaHelper.fetchAnnouncements(new SupabaseJavaHelper.AnnouncementCallback() {
            @Override
            public void onSuccess(List<Announcement> data) {
                // Hide loader regardless of first load or not
                if (isAdded() && getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }

                if (isAdded()) {
                    announcementList.clear();
                    announcementList.addAll(data);
                    fullAnnouncementList.clear();
                    fullAnnouncementList.addAll(data);
                    announcementAdapter.notifyDataSetChanged();
                    updatePlaceholder(announcementList.isEmpty());

                    isFirstLoad = false; // Mark first load complete
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (isAdded() && getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }
                if (isAdded()) {
                    Log.e("HomeFragment", "Fetch Error: " + message);
                    updatePlaceholder(announcementList.isEmpty());
                }
            }
        });
    }

    private void updatePlaceholder(boolean isEmpty) {
        if (tvEmptyPlaceholder == null) return;
        tvEmptyPlaceholder.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        announcementRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showApplyDialog(Announcement announcement) {
        if (announcement.isApplied()) {
            Toast.makeText(getContext(), "You have already applied to this drive!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userType != null && (userType.equalsIgnoreCase("Overseas") || userType.equalsIgnoreCase("Non-Resident"))) {
            Toast.makeText(getContext(), "🚫 Only Residents can apply for relief packs.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!isUserVerified) {
            Toast.makeText(getContext(), "🔒 You must be a VERIFIED Resident to apply.", Toast.LENGTH_LONG).show();
            return;
        }

        ApplyConfirmationDialogFragment dialog = new ApplyConfirmationDialogFragment();
        dialog.setOnConfirmListener(() -> {
            if (announcement.getLinkedDriveId() == null) {
                Toast.makeText(getContext(), "Error: Drive not linked.", Toast.LENGTH_SHORT).show();
                return;
            }

            SupabaseJavaHelper.applyToDrive(announcement.getLinkedDriveId(), new SupabaseJavaHelper.ApplicationCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        dialog.dismiss();
                        announcement.setApplied(true);
                        announcementAdapter.notifyDataSetChanged();
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
        dialog.show(getParentFragmentManager(), "ApplyDialog");
    }
}