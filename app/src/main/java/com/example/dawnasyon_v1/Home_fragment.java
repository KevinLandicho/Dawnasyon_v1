package com.example.dawnasyon_v1;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

public class Home_fragment extends BaseFragment {

    // UI Elements
    private TextView welcomeText;
    private ImageView userAvatar;
    private SearchView searchView;
    private ImageView iconFilter; // Bookmark Icon

    // Filter Buttons
    private Button btnFilterAll, btnFilterGeneral, btnFilterDrive;

    // Carousel Components
    private ViewPager2 imageCarouselViewPager;
    private ImageCarouselAdapter carouselAdapter;
    private Handler sliderHandler = new Handler();
    private final int SLIDE_INTERVAL_MS = 3000;

    // Announcement List Components
    private RecyclerView announcementRecyclerView;
    private AnnouncementAdapter announcementAdapter;
    private TextView tvEmptyPlaceholder;

    // Data Lists
    private List<Announcement> announcementList = new ArrayList<>();
    private List<Announcement> fullAnnouncementList = new ArrayList<>();

    // User State
    private boolean isUserVerified = false;
    private String userType = "Resident";
    private String currentUserStreet = "";
    private boolean isFirstLoad = true;

    // Filter State Variables
    private boolean showBookmarksOnly = false;
    private String currentCategoryFilter = "ALL"; // Options: "ALL", "General", "Donation drive"

    // Carousel Runnable
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

    public Home_fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Initialize UI
        welcomeText = view.findViewById(R.id.welcome_text);
        userAvatar = view.findViewById(R.id.user_avatar);
        searchView = view.findViewById(R.id.search_view);
        iconFilter = view.findViewById(R.id.icon_filter);

        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterGeneral = view.findViewById(R.id.btn_filter_general);
        btnFilterDrive = view.findViewById(R.id.btn_filter_drive);

        imageCarouselViewPager = view.findViewById(R.id.image_carousel_view_pager);
        announcementRecyclerView = view.findViewById(R.id.announcement_recycler_view);
        tvEmptyPlaceholder = view.findViewById(R.id.tv_empty_placeholder);

        // 2. Click Listeners
        userAvatar.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new Profile_fragment())
                    .addToBackStack(null)
                    .commit();
        });

        iconFilter.setOnClickListener(v -> toggleBookmarkFilter());

        btnFilterAll.setOnClickListener(v -> setCategoryFilter("ALL"));
        btnFilterGeneral.setOnClickListener(v -> setCategoryFilter("General"));
        btnFilterDrive.setOnClickListener(v -> setCategoryFilter("Donation drive"));

        // 3. Setup Components
        setupCarousel();
        setupAnnouncementsList();
        setupSearch();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, SLIDE_INTERVAL_MS);
        // Load data every time we come back (ensures cache is fresh)
        loadUserProfileAndAnnouncements();
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

    // ====================================================
    // ⭐ DATA LOADING (CRASH PROOF)
    // ====================================================

    private void loadUserProfileAndAnnouncements() {
        if (isFirstLoad && getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).showLoading();
        }

        // Use getContext() safely
        if (getContext() == null) return;

        // 1. Fetch Profile First
        SupabaseJavaHelper.fetchUserProfile(getContext(), new SupabaseJavaHelper.ProfileCallback() {
            @Override
            public void onLoaded(Profile profile) {
                // Safety check: Is fragment still alive?
                if (!isAdded() || getActivity() == null) return;

                if (profile != null) {
                    welcomeText.setText("Welcome, " + profile.getFull_name() + "!"); // Use First Name for cleaner UI
                    isUserVerified = Boolean.TRUE.equals(profile.getVerified());
                    if (profile.getType() != null) userType = profile.getType();
                    currentUserStreet = (profile.getStreet() != null) ? profile.getStreet().trim() : "";

                    // Avatar Logic
                    String avatarName = profile.getAvatarName();
                    int avatarResId = R.drawable.ic_profile_avatar; // Default

                    if (avatarName != null && !avatarName.isEmpty()) {
                        int resId = getResources().getIdentifier(avatarName, "drawable", getContext().getPackageName());
                        if (resId != 0) {
                            avatarResId = resId;
                        }
                    }

                    // Safe Glide Loading
                    try {
                        Glide.with(Home_fragment.this)
                                .load(avatarResId)
                                .placeholder(R.drawable.ic_profile_avatar)
                                .circleCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(userAvatar);
                    } catch (Exception e) {
                        // Ignore offline image errors
                    }
                }

                // 2. Fetch Announcements (Chained)
                fetchAnnouncementsFromSupabase();
            }

            @Override
            public void onError(String message) {
                // If profile fails, still try to load announcements (maybe we have them cached)
                if (!isAdded()) return;
                fetchAnnouncementsFromSupabase();
            }
        });
    }

    private void fetchAnnouncementsFromSupabase() {
        if (getContext() == null) return;

        SupabaseJavaHelper.fetchAnnouncements(getContext(), new SupabaseJavaHelper.AnnouncementCallback() {
            @Override
            public void onSuccess(List<Announcement> data) {
                // Check Fragment Life
                if (!isAdded() || getActivity() == null) return;

                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }

                List<Announcement> visibleList = new ArrayList<>();

                // Street Filtering Logic
                for (Announcement item : data) {
                    boolean showIt = true;

                    // Only filter "Donation drives" by street
                    if (item.getType() != null && item.getType().equalsIgnoreCase("Donation drive")) {
                        String targetStreet = item.getAffected_street();
                        if (targetStreet == null || targetStreet.trim().isEmpty()) {
                            showIt = true; // No specific street, show to all
                        } else if (targetStreet.equalsIgnoreCase("All Streets") || targetStreet.equalsIgnoreCase("All")) {
                            showIt = true; // Targeted to all
                        } else {
                            // Compare user street vs target street (Case Insensitive)
                            showIt = targetStreet.equalsIgnoreCase(currentUserStreet);
                        }
                    }

                    if (showIt) {
                        visibleList.add(item);
                    }
                }

                fullAnnouncementList.clear();
                fullAnnouncementList.addAll(visibleList);

                // Re-apply current UI filters (Search/Category)
                applyFilters(searchView.getQuery().toString());
                isFirstLoad = false;
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || getActivity() == null) return;

                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).hideLoading();
                }

                // If list is empty and we got an error, show empty state
                if (fullAnnouncementList.isEmpty()) {
                    updatePlaceholder(true);
                }
            }
        });
    }

    // ====================================================
    // ⭐ FILTERING LOGIC
    // ====================================================

    private void toggleBookmarkFilter() {
        showBookmarksOnly = !showBookmarksOnly;

        if (showBookmarksOnly) {
            iconFilter.setColorFilter(Color.parseColor("#F5901A")); // Orange
            iconFilter.setImageResource(R.drawable.ic_bookmark_filled);
            Toast.makeText(getContext(), "Showing Saved Items", Toast.LENGTH_SHORT).show();
        } else {
            iconFilter.setColorFilter(Color.parseColor("#757575")); // Gray
            iconFilter.setImageResource(R.drawable.ic_bookmark_outline);
            Toast.makeText(getContext(), "Showing All", Toast.LENGTH_SHORT).show();
        }
        applyFilters(searchView.getQuery().toString());
    }

    private void setCategoryFilter(String category) {
        currentCategoryFilter = category;

        // Reset Buttons
        updateButtonState(btnFilterAll, false);
        updateButtonState(btnFilterGeneral, false);
        updateButtonState(btnFilterDrive, false);

        // Highlight Active
        if (category.equals("ALL")) updateButtonState(btnFilterAll, true);
        else if (category.equals("General")) updateButtonState(btnFilterGeneral, true);
        else if (category.equals("Donation drive")) updateButtonState(btnFilterDrive, true);

        applyFilters(searchView.getQuery().toString());
    }

    private void updateButtonState(Button btn, boolean isActive) {
        if (isActive) {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5901A")));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btn.setTextColor(Color.parseColor("#757575"));
        }
    }

    private void applyFilters(String query) {
        List<Announcement> filteredList = new ArrayList<>();
        String lowerCaseQuery = (query != null) ? query.toLowerCase().trim() : "";

        for (Announcement item : fullAnnouncementList) {
            boolean matchesSearch = true;
            boolean matchesBookmark = true;
            boolean matchesCategory = true;

            // 1. Search Check
            if (!lowerCaseQuery.isEmpty()) {
                boolean titleMatch = item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery);
                boolean descMatch = item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseQuery);
                matchesSearch = titleMatch || descMatch;
            }

            // 2. Bookmark Check
            if (showBookmarksOnly) {
                matchesBookmark = item.isBookmarked();
            }

            // 3. Category Check
            if (!currentCategoryFilter.equals("ALL")) {
                if (item.getType() != null) {
                    matchesCategory = item.getType().equalsIgnoreCase(currentCategoryFilter);
                } else {
                    matchesCategory = false;
                }
            }

            if (matchesSearch && matchesBookmark && matchesCategory) {
                filteredList.add(item);
            }
        }

        announcementAdapter.updateData(filteredList);
        updatePlaceholder(filteredList.isEmpty());
    }

    private void updatePlaceholder(boolean isEmpty) {
        if (tvEmptyPlaceholder == null) return;
        if (isEmpty) {
            tvEmptyPlaceholder.setVisibility(View.VISIBLE);
            tvEmptyPlaceholder.setText("No announcements match your filters.");
            announcementRecyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyPlaceholder.setVisibility(View.GONE);
            announcementRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // ====================================================
    // ⭐ INTERACTIONS (LIKE, BOOKMARK, APPLY)
    // ====================================================

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

        // Optimistic UI Update (Update instantly)
        item.setLiked(newState);
        int currentCount = item.getLikeCount();
        item.setLikeCount(newState ? currentCount + 1 : Math.max(0, currentCount - 1));
        announcementAdapter.notifyItemChanged(position);

        SupabaseJavaHelper.toggleLike(item.getPostId(), newState, new SupabaseJavaHelper.SimpleCallback() {
            @Override
            public void onSuccess() { /* Synced with server */ }
            @Override
            public void onError(String msg) {
                if (isAdded()) {
                    // Revert on failure
                    item.setLiked(currentState);
                    item.setLikeCount(currentCount);
                    announcementAdapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Failed to like", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleBookmark(Announcement item, int position) {
        boolean currentState = item.isBookmarked();
        boolean newState = !currentState;
        item.setBookmarked(newState);

        if (showBookmarksOnly && !newState) {
            // Remove instantly if in bookmark mode
            announcementList.remove(position);
            announcementAdapter.notifyItemRemoved(position);
            updatePlaceholder(announcementList.isEmpty());
        } else {
            announcementAdapter.notifyItemChanged(position);
        }

        SupabaseJavaHelper.toggleBookmark(item.getPostId(), newState, new SupabaseJavaHelper.SimpleCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(String msg) {
                if (isAdded()) {
                    // Revert
                    item.setBookmarked(currentState);
                    if (showBookmarksOnly && !newState) {
                        loadUserProfileAndAnnouncements(); // Reload to fix list
                    } else {
                        announcementAdapter.notifyItemChanged(position);
                    }
                }
            }
        });
    }

    private void showApplyDialog(Announcement announcement) {
        if (announcement.isApplied()) {
            Toast.makeText(getContext(), "You have already applied to this drive!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate User Type
        if (userType != null && (userType.equalsIgnoreCase("Overseas") || userType.equalsIgnoreCase("Non-Resident"))) {
            Toast.makeText(getContext(), "🚫 Only Residents can apply for relief packs.", Toast.LENGTH_LONG).show();
            return;
        }
        // Validate Verification
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

    // ====================================================
    // ⭐ SETUP UTILS
    // ====================================================

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

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { applyFilters(query); return true; }
            @Override
            public boolean onQueryTextChange(String newText) { applyFilters(newText); return true; }
        });
    }
}