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

    private List<Announcement> announcementList = new ArrayList<>();
    private List<Announcement> fullAnnouncementList = new ArrayList<>();
    private boolean isUserVerified = false;

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

        loadUserProfile();
        setupCarousel();
        setupAnnouncementsList();
        setupSearch();
        fetchAnnouncementsFromSupabase();

        return view;
    }

    private void loadUserProfile() {
        AuthHelper.fetchUserProfile(profile -> {
            if (profile != null && isAdded()) {
                welcomeText.setText("Welcome, " + profile.getFull_name() + "!");
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

        // ⭐ INITIALIZE ADAPTER WITH CLICK LISTENERS
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

    // ⭐ LOGIC: Handle Like Click
    private void handleLike(Announcement item, int position) {
        // 1. Optimistic update: Change UI instantly
        boolean currentState = item.isLiked();
        boolean newState = !currentState;

        item.setLiked(newState);

        // Update count visually
        int currentCount = item.getLikeCount();
        int newCount = newState ? currentCount + 1 : Math.max(0, currentCount - 1);
        item.setLikeCount(newCount);

        // Notify adapter to redraw row immediately
        announcementAdapter.notifyItemChanged(position);

        // 2. Send to Database
        SupabaseJavaHelper.toggleLike(item.getPostId(), newState, new SimpleCallback() {
            @Override
            public void onSuccess() {
                // Success - UI is already updated, nothing to do
                Log.d("HomeFragment", "Like updated successfully");
            }

            @Override
            public void onError(String msg) {
                // 3. If DB fails, Revert UI
                if (isAdded()) {
                    item.setLiked(currentState); // Revert state
                    item.setLikeCount(currentCount); // Revert count
                    announcementAdapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Failed to like: " + msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ⭐ LOGIC: Handle Bookmark Click
    private void handleBookmark(Announcement item, int position) {
        // 1. Optimistic update
        boolean currentState = item.isBookmarked();
        boolean newState = !currentState;

        item.setBookmarked(newState);
        announcementAdapter.notifyItemChanged(position);

        String msg = newState ? "Saved to bookmarks" : "Removed from bookmarks";
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();

        // 2. Send to Database
        SupabaseJavaHelper.toggleBookmark(item.getPostId(), newState, new SimpleCallback() {
            @Override
            public void onSuccess() {
                // Success - nothing to do
                Log.d("HomeFragment", "Bookmark updated successfully");
            }

            @Override
            public void onError(String msg) {
                // 3. Revert if failed
                if (isAdded()) {
                    item.setBookmarked(currentState);
                    announcementAdapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Failed to bookmark", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

            if (matchesTitle || matchesBody) filteredList.add(item);
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
        if (!isUserVerified) {
            Toast.makeText(getContext(), "🔒 You must verify your account to apply.", Toast.LENGTH_LONG).show();
            return;
        }

        ApplyConfirmationDialogFragment dialog = new ApplyConfirmationDialogFragment();

        dialog.setOnConfirmListener(() -> {
            if (announcement.getLinkedDriveId() == null) {
                Toast.makeText(getContext(), "Error: This drive is not linked properly.", Toast.LENGTH_SHORT).show();
                return;
            }

            SupabaseJavaHelper.applyToDrive(announcement.getLinkedDriveId(), new ApplicationCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        dialog.dismiss();

                        // ⭐ UPDATE UI: Mark as Applied
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