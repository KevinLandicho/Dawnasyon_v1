package com.example.dawnasyon_v1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Home_fragment extends BaseFragment {

    // UI Elements
    private TextView welcomeText;
    private ImageView userAvatar;
    private SearchView searchView;
    private ImageView iconFilter;

    // Filter Buttons
    private Button btnFilterAll, btnFilterGeneral, btnFilterDrive;

    // Badge TextView
    private TextView tvDriveBadge;

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
    private String currentCategoryFilter = "ALL";

    // Preference Key for "Last Checked"
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_LAST_CHECKED_DRIVE = "last_checked_drive_time";

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

        tvDriveBadge = view.findViewById(R.id.tv_drive_badge);

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
        btnFilterGeneral.setOnClickListener(v -> setCategoryFilter("General")); // Maps to "Not Donation drive"

        // Mark as read when clicked
        btnFilterDrive.setOnClickListener(v -> {
            setCategoryFilter("Donation drive");
            markDrivesAsRead();
        });

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
    // ⭐ DATA LOADING & FILTER LOGIC
    // ====================================================

    private void loadUserProfileAndAnnouncements() {
        if (isFirstLoad && getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).showLoading();
        }
        if (getContext() == null) return;

        SupabaseJavaHelper.fetchUserProfile(getContext(), new SupabaseJavaHelper.ProfileCallback() {
            @Override
            public void onLoaded(Profile profile) {
                if (!isAdded() || getActivity() == null) return;

                if (profile != null) {
                    welcomeText.setText("Welcome, " + profile.getFull_name() + "!");
                    isUserVerified = Boolean.TRUE.equals(profile.getVerified());
                    if (profile.getType() != null) userType = profile.getType();
                    currentUserStreet = (profile.getStreet() != null) ? profile.getStreet().trim() : "";

                    String avatarName = profile.getAvatarName();
                    int avatarResId = R.drawable.ic_profile_avatar;
                    if (avatarName != null && !avatarName.isEmpty()) {
                        int resId = getResources().getIdentifier(avatarName, "drawable", getContext().getPackageName());
                        if (resId != 0) avatarResId = resId;
                    }
                    try {
                        Glide.with(Home_fragment.this).load(avatarResId).placeholder(R.drawable.ic_profile_avatar).circleCrop().into(userAvatar);
                    } catch (Exception e) {}
                }
                fetchAnnouncementsFromSupabase();
            }

            @Override
            public void onError(String message) {
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
                if (!isAdded() || getActivity() == null) return;
                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();

                List<Announcement> visibleList = new ArrayList<>();
                int newDriveCount = 0;

                SharedPreferences prefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                long lastCheckedTime = prefs.getLong(KEY_LAST_CHECKED_DRIVE, 0);

                // ⭐ DATE CHECK: Get Today (Time zeroed out)
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date todayZero = cal.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (Announcement item : data) {
                    boolean showIt = true;
                    boolean isDrive = (item.getType() != null && item.getType().equalsIgnoreCase("Donation drive"));

                    // 1. Street Filter (Only strictly applies to Drives)
                    if (isDrive) {
                        String targetStreet = item.getAffected_street();
                        if (targetStreet == null || targetStreet.trim().isEmpty() ||
                                targetStreet.equalsIgnoreCase("All Streets") ||
                                targetStreet.equalsIgnoreCase("All")) {
                            // Keep true
                        } else {
                            // If street doesn't match, hide it
                            if (!targetStreet.equalsIgnoreCase(currentUserStreet)) {
                                showIt = false;
                            }
                        }
                    }

                    // ⭐ 2. END DATE CHECK (Strict: If date exists and passed, remove from Home)
                    String endDateStr = item.getDriveEndDate();
                    if (showIt && endDateStr != null && !endDateStr.isEmpty()) {
                        try {
                            Date endDate = sdf.parse(endDateStr);
                            // If Today is AFTER End Date -> HIDE IT
                            if (endDate != null && todayZero.after(endDate)) {
                                showIt = false;
                            }
                        } catch (ParseException e) { e.printStackTrace(); }
                    }

                    // ⭐ 3. START DATE CHECK (Hide Advance Posts)
                    String startDateStr = item.getDriveStartDate();
                    if (showIt && startDateStr != null && !startDateStr.isEmpty()) {
                        try {
                            Date startDate = sdf.parse(startDateStr);
                            // If Today is BEFORE Start Date -> HIDE IT
                            if (startDate != null && todayZero.before(startDate)) {
                                showIt = false;
                            }
                        } catch (ParseException e) { e.printStackTrace(); }
                    }

                    // NOTE: If Start/End dates are NULL/Empty, code skips the checks above
                    // and 'showIt' remains TRUE. (This covers your requirement to show items with empty dates)

                    // 4. Badge Count (Only count active Drives)
                    if (showIt && isDrive) {
                        long itemTime = parseDateToMillis(item.getCreated_at());
                        if (itemTime > lastCheckedTime) {
                            newDriveCount++;
                        }
                    }

                    if (showIt) {
                        visibleList.add(item);
                    }
                }

                fullAnnouncementList.clear();
                fullAnnouncementList.addAll(visibleList);

                updateDriveBadge(newDriveCount);

                applyFilters(searchView.getQuery().toString());
                isFirstLoad = false;
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || getActivity() == null) return;
                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                if (fullAnnouncementList.isEmpty()) updatePlaceholder(true);
            }
        });
    }

    private long parseDateToMillis(String dateStr) {
        if (dateStr == null) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            return (date != null) ? date.getTime() : 0;
        } catch (Exception e) { return 0; }
    }

    private void markDrivesAsRead() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LAST_CHECKED_DRIVE, System.currentTimeMillis());
        editor.apply();
        updateDriveBadge(0);
    }

    private void updateDriveBadge(int count) {
        if (tvDriveBadge == null) return;
        if (count > 0) {
            tvDriveBadge.setVisibility(View.VISIBLE);
            tvDriveBadge.setText(String.valueOf(count));
        } else {
            tvDriveBadge.setVisibility(View.GONE);
        }
    }

    // ====================================================
    // FILTERING LOGIC
    // ====================================================

    private void toggleBookmarkFilter() {
        showBookmarksOnly = !showBookmarksOnly;
        if (showBookmarksOnly) {
            iconFilter.setColorFilter(Color.parseColor("#F5901A"));
            iconFilter.setImageResource(R.drawable.ic_bookmark_filled);
            Toast.makeText(getContext(), "Showing Saved Items", Toast.LENGTH_SHORT).show();
        } else {
            iconFilter.setColorFilter(Color.parseColor("#757575"));
            iconFilter.setImageResource(R.drawable.ic_bookmark_outline);
            Toast.makeText(getContext(), "Showing All", Toast.LENGTH_SHORT).show();
        }
        applyFilters(searchView.getQuery().toString());
    }

    private void setCategoryFilter(String category) {
        currentCategoryFilter = category;
        updateButtonState(btnFilterAll, false);
        updateButtonState(btnFilterGeneral, false);
        updateButtonState(btnFilterDrive, false);
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

            // 1. Search Logic
            if (!lowerCaseQuery.isEmpty()) {
                boolean titleMatch = item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery);
                boolean descMatch = item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerCaseQuery);
                matchesSearch = titleMatch || descMatch;
            }

            // 2. Bookmark Logic
            if (showBookmarksOnly) matchesBookmark = item.isBookmarked();

            // ⭐ 3. Category Logic (UPDATED)
            if (!currentCategoryFilter.equals("ALL")) {
                if (currentCategoryFilter.equals("General")) {
                    // Logic: Show everything that is NOT a Donation drive
                    if (item.getType() != null && item.getType().equalsIgnoreCase("Donation drive")) {
                        matchesCategory = false;
                    } else {
                        matchesCategory = true;
                    }
                } else if (currentCategoryFilter.equals("Donation drive")) {
                    // Logic: Show ONLY Donation drives
                    if (item.getType() != null && item.getType().equalsIgnoreCase("Donation drive")) {
                        matchesCategory = true;
                    } else {
                        matchesCategory = false;
                    }
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
    // INTERACTIONS
    // ====================================================

    private void setupAnnouncementsList() {
        announcementRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        announcementAdapter = new AnnouncementAdapter(announcementList, new AnnouncementAdapter.OnItemClickListener() {
            @Override
            public void onApplyClick(Announcement announcement) { showApplyDialog(announcement); }
            @Override
            public void onLikeClick(Announcement announcement, int position) { handleLike(announcement, position); }
            @Override
            public void onBookmarkClick(Announcement announcement, int position) { handleBookmark(announcement, position); }
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
            public void onSuccess() { }
            @Override
            public void onError(String msg) {
                if (isAdded()) {
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
                    item.setBookmarked(currentState);
                    if (showBookmarksOnly && !newState) loadUserProfileAndAnnouncements();
                    else announcementAdapter.notifyItemChanged(position);
                }
            }
        });
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

    private void setupCarousel() {
        List<Integer> imageList = new ArrayList<>();
        imageList.add(R.drawable.img1);
        imageList.add(R.drawable.img2);
        imageList.add(R.drawable.img3);
        imageList.add(R.drawable.img4);
        imageList.add(R.drawable.img5);
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