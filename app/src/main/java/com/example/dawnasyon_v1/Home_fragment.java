package com.example.dawnasyon_v1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private ImageView brgyLogo;

    // Carousel Components
    private View carouselContainer;
    private ViewPager2 imageCarouselViewPager;
    private ImageCarouselAdapter carouselAdapter;
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
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

    // Preference Keys
    private static final String PREF_NAME = "UserPrefs";
    private static final String CACHE_PREF = "ProfileCache";

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

    public Home_fragment() {}

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
        brgyLogo = view.findViewById(R.id.brgy_logo);

        carouselContainer = view.findViewById(R.id.carousel_container);
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

        if (brgyLogo != null) {
            brgyLogo.setOnClickListener(v -> {
                BrgyInfoDialog dialog = new BrgyInfoDialog();
                dialog.show(getParentFragmentManager(), "BrgyInfoDialog");
            });
        }

        // 3. Setup Components
        setupCarousel();
        setupAnnouncementsList();
        setupSearch();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences profileCache = requireContext().getSharedPreferences(CACHE_PREF, Context.MODE_PRIVATE);
        String cachedName = profileCache.getString("full_name", "");
        String cachedAvatar = profileCache.getString("avatar_name", "");

        if (!cachedName.isEmpty()) {
            welcomeText.setText("Welcome, " + cachedName + "!");
        } else {
            welcomeText.setText("Welcome!");
        }

        if (!cachedAvatar.isEmpty()) {
            try {
                if (cachedAvatar.startsWith("http://") || cachedAvatar.startsWith("https://")) {
                    Glide.with(this).load(cachedAvatar).placeholder(R.drawable.ic_profile_avatar).circleCrop().into(userAvatar);
                } else {
                    int resId = getResources().getIdentifier(cachedAvatar, "drawable", requireContext().getPackageName());
                    if (resId != 0) Glide.with(this).load(resId).placeholder(R.drawable.ic_profile_avatar).circleCrop().into(userAvatar);
                }
            } catch (Exception ignored) {}
        }

        applyTagalogTranslation(view);
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
    // DATA LOADING
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
                    SharedPreferences profileCache = requireContext().getSharedPreferences(CACHE_PREF, Context.MODE_PRIVATE);
                    profileCache.edit()
                            .putString("full_name", profile.getFull_name())
                            .putString("avatar_name", profile.getAvatarName())
                            .apply();

                    String welcomeMsg = "Welcome, " + profile.getFull_name() + "!";
                    welcomeText.setText(welcomeMsg);

                    TranslationHelper.autoTranslate(getContext(), welcomeText, welcomeMsg);

                    isUserVerified = Boolean.TRUE.equals(profile.getVerified());
                    if (profile.getType() != null) userType = profile.getType();
                    currentUserStreet = (profile.getStreet() != null) ? profile.getStreet().trim() : "";

                    String avatarName = profile.getAvatarName();
                    try {
                        if (avatarName != null && (avatarName.startsWith("http://") || avatarName.startsWith("https://"))) {
                            Glide.with(Home_fragment.this).load(avatarName).placeholder(R.drawable.ic_profile_avatar).circleCrop().into(userAvatar);
                        } else {
                            int avatarResId = R.drawable.ic_profile_avatar;
                            if (avatarName != null && !avatarName.isEmpty()) {
                                int resId = getResources().getIdentifier(avatarName, "drawable", getContext().getPackageName());
                                if (resId != 0) avatarResId = resId;
                            }
                            Glide.with(Home_fragment.this).load(avatarResId).placeholder(R.drawable.ic_profile_avatar).circleCrop().into(userAvatar);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date todayZero = cal.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (Announcement item : data) {
                    // ⭐ STRICT FILTER: Only show "Approved" status AND "General" type announcements!
                    if (item.getStatus() == null || !item.getStatus().equalsIgnoreCase("Approved")) {
                        continue;
                    }
                    if (item.getType() == null || !item.getType().equalsIgnoreCase("General")) {
                        continue;
                    }

                    boolean showIt = true;

                    // Date Expiration Checks
                    String endDateStr = item.getDriveEndDate();
                    if (showIt && endDateStr != null && !endDateStr.trim().isEmpty() && !endDateStr.equalsIgnoreCase("null")) {
                        try {
                            Date endDate = sdf.parse(endDateStr);
                            if (endDate != null && todayZero.after(endDate)) {
                                showIt = false;
                            }
                        } catch (ParseException e) { e.printStackTrace(); }
                    }

                    String startDateStr = item.getDriveStartDate();
                    if (showIt && startDateStr != null && !startDateStr.trim().isEmpty() && !startDateStr.equalsIgnoreCase("null")) {
                        try {
                            Date startDate = sdf.parse(startDateStr);
                            if (startDate != null && todayZero.before(startDate)) {
                                showIt = false;
                            }
                        } catch (ParseException e) { e.printStackTrace(); }
                    }

                    if (showIt) {
                        visibleList.add(item);
                    }
                }

                fullAnnouncementList.clear();
                fullAnnouncementList.addAll(visibleList);

                applyFilters(searchView.getQuery().toString());

                isFirstLoad = false;
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || getActivity() == null) return;
                if (getActivity() instanceof BaseActivity) ((BaseActivity) getActivity()).hideLoading();
                if (fullAnnouncementList.isEmpty()) updatePlaceholder(true);

                isFirstLoad = false;
            }
        });
    }
    // ====================================================
    // FILTERING LOGIC
    // ====================================================

    private void toggleBookmarkFilter() {
        showBookmarksOnly = !showBookmarksOnly;

        updateCarouselVisibility();

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

    private void updateCarouselVisibility() {
        View targetToHide = (carouselContainer != null) ? carouselContainer : imageCarouselViewPager;
        if (targetToHide != null) {
            if (!showBookmarksOnly) {
                targetToHide.setVisibility(View.VISIBLE);
            } else {
                targetToHide.setVisibility(View.GONE);
            }
        }
    }

    private void applyFilters(String query) {
        List<Announcement> filteredList = new ArrayList<>();

        String queryLower = query != null ? query.toLowerCase().trim() : "";
        String[] keywords = queryLower.split("\\s+");

        SharedPreferences translationCache = null;
        if (getContext() != null) {
            translationCache = getContext().getSharedPreferences("TranslationCache", Context.MODE_PRIVATE);
        }

        for (Announcement item : fullAnnouncementList) {
            boolean matchesSearch = true;
            boolean matchesBookmark = true;

            if (!queryLower.isEmpty() && keywords.length > 0 && !keywords[0].isEmpty()) {
                String origTitle = item.getTitle() != null ? item.getTitle() : "";
                String origDesc = item.getDescription() != null ? item.getDescription() : "";

                String transTitle = origTitle;
                String transDesc = origDesc;

                if (translationCache != null) {
                    transTitle = translationCache.getString(origTitle, origTitle);
                    transDesc = translationCache.getString(origDesc, origDesc);
                }

                String searchTitleOrig = origTitle.toLowerCase();
                String searchDescOrig = origDesc.toLowerCase();
                String searchTitleTrans = transTitle.toLowerCase();
                String searchDescTrans = transDesc.toLowerCase();

                for (String keyword : keywords) {
                    if (keyword.isEmpty()) continue;

                    boolean foundInTitle = searchTitleOrig.contains(keyword) || searchTitleTrans.contains(keyword);
                    boolean foundInDesc = searchDescOrig.contains(keyword) || searchDescTrans.contains(keyword);

                    if (!foundInTitle && !foundInDesc) {
                        matchesSearch = false;
                        break;
                    }
                }
            }

            if (showBookmarksOnly) matchesBookmark = item.isBookmarked();

            if (matchesSearch && matchesBookmark) {
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
            public void onApplyClick(Announcement announcement) { showClaimingStub(announcement); }
            @Override
            public void onLikeClick(Announcement announcement, int position) { handleLike(announcement, position); }
            @Override
            public void onBookmarkClick(Announcement announcement, int position) { handleBookmark(announcement, position); }
            @Override
            public void onCardClick(Announcement announcement) { showReliefGoodsDialog(announcement); }
        });
        announcementRecyclerView.setAdapter(announcementAdapter);
    }

    private void showReliefGoodsDialog(Announcement announcement) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        String title = announcement.getTitle();
        String reliefItems = announcement.getReliefItemList();

        if (reliefItems == null || reliefItems.trim().isEmpty() || reliefItems.equalsIgnoreCase("null")) {
            reliefItems = "Details regarding relief items will be updated soon.";
        }

        String message = "📦 Included Relief Goods:\n\n" + reliefItems;

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            TranslationHelper.autoTranslate(getContext(), messageView, message);
        }
    }

    private void handleLike(Announcement item, int position) {
        boolean currentState = item.isLiked();
        boolean newState = !currentState;
        item.setLiked(newState);
        int currentCount = item.getLikeCount();
        item.setLikeCount(newState ? currentCount + 1 : Math.max(0, currentCount - 1));

        announcementAdapter.notifyItemChanged(position, "LIKE_UPDATE");

        SupabaseJavaHelper.toggleLike(item.getPostId(), newState, new SupabaseJavaHelper.SimpleCallback() {
            @Override
            public void onSuccess() { }
            @Override
            public void onError(String msg) {
                if (isAdded()) {
                    item.setLiked(currentState);
                    item.setLikeCount(currentCount);
                    announcementAdapter.notifyItemChanged(position, "LIKE_UPDATE");
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
            applyFilters(searchView.getQuery().toString());
        } else {
            announcementAdapter.notifyItemChanged(position, "BOOKMARK_UPDATE");
        }

        SupabaseJavaHelper.toggleBookmark(item.getPostId(), newState, new SupabaseJavaHelper.SimpleCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onError(String msg) {
                if (isAdded()) {
                    item.setBookmarked(currentState);
                    if (showBookmarksOnly && !newState) {
                        applyFilters(searchView.getQuery().toString());
                    } else {
                        announcementAdapter.notifyItemChanged(position, "BOOKMARK_UPDATE");
                    }
                }
            }
        });
    }

    // ⭐ THE FIX: Replaced "Apply" logic with the Automated Distribution stub viewer
    private void showClaimingStub(Announcement announcement) {
        if (userType != null && (userType.equalsIgnoreCase("Overseas") || userType.equalsIgnoreCase("Non-Resident"))) {
            Toast.makeText(getContext(), "🚫 Only Residents are eligible for relief packs.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!isUserVerified) {
            Toast.makeText(getContext(), "🔒 You must be a VERIFIED Resident to claim relief goods.", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        String title = "Claiming Reserved";
        String message = "✅ You are automatically registered for this distribution drive based on your official Brgy. Sta. Lucia address!\n\n" +
                "Please present your digital QR Code (found in your Profile tab) at the distribution center to claim your family's relief pack.";

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Go to Profile", (dialog, which) -> {
                    dialog.dismiss();
                    // Navigate to profile fragment to view QR
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new Profile_fragment())
                            .addToBackStack(null)
                            .commit();
                })
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            TranslationHelper.autoTranslate(getContext(), messageView, message);
        }
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
        TextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { applyFilters(query); return true; }
            @Override
            public boolean onQueryTextChange(String newText) { applyFilters(newText); return true; }
        });
    }
}