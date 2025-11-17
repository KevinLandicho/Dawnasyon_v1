package com.example.dawnasyon_v1;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 🟢 FIX: Import the ItemForSummary class from the Summary_fragment file
import com.example.dawnasyon_v1.Summary_fragment.ItemForSummary;

public class Donation_details_fragment extends Fragment {

    private static final String TAG = "DonationDetailsFrag";

    // --- ARGUMENT CONSTANTS ---
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_IMAGE = "arg_image";

    // --- INSTANCE FIELDS ---
    private LinearLayout itemInputsContainer;
    private Button btnAddCustomItem;
    private LinearLayout customItemInputLayout;
    private LayoutInflater inflater;

    private String fTitle;
    private String fDescription;
    private String fStatus;
    private int fImageRes;

    // 🔴 REMOVED: The redundant ItemForSummary class definition was removed from here.

    // -----------------------------------------------------------
    // --- 1. DEFINE UNIT SETS BY CATEGORY ---
    private static final String[] ALL_UNITS = {"PCS", "Kilo", "Liter", "Pack", "PHP", "Set"};
    private static final String[] CASH_UNITS = {"PHP"};
    private static final String[] FOOD_UNITS = {"PCS", "Kilo", "Liter", "Pack"};
    private static final String[] MEDICINE_UNITS = {"Pack", "Bottles", "Set"};
    private static final String[] RELIEF_PACKS_UNITS = {"Set"};
    private static final String[] DEFAULT_UNITS = {"PCS", "Pack", "Set"};


    // --- DATA STRUCTURE FOR PRESET ITEMS ---
    private static class ItemData {
        String name;
        String defaultUnit;
        String description;
        int layoutType; // 1: Spinner (item_input), 2: Description (item_input_desc)

        ItemData(String name, String defaultUnit) {
            this.name = name;
            this.defaultUnit = defaultUnit;
            this.layoutType = 1;
        }

        ItemData(String name, String description, int layoutType) {
            this.name = name;
            this.description = description;
            this.layoutType = 2;
        }
    }

    private static final Map<String, List<ItemData>> PRESET_ITEMS = new HashMap<>();

    static {
        // Keys MUST match the titles passed from Step 1 (e.g., "FOOD")
        PRESET_ITEMS.put("CASH", Arrays.asList(
                new ItemData("GCash", "PHP"),
                new ItemData("Maya", "PHP"),
                new ItemData("Cash (Physical)", "PHP")
        ));

        PRESET_ITEMS.put("FOOD", Arrays.asList(
                new ItemData("Rice", "Kilo"),
                new ItemData("Instant noodles", "PCS"),
                new ItemData("Canned Goods", "PCS"),
                new ItemData("Biscuits", "PCS")
        ));

        PRESET_ITEMS.put("HYGIENE KITS", Arrays.asList(
                new ItemData("Body Care", "e.g. Soap, shampoo, toothbrush", 2),
                new ItemData("Sanitation", "e.g. Alcohol, wet wipes, tissue, sanitary napkins", 2),
                new ItemData("Laundry", "e.g. Detergent powder, bar soap", 2),
                new ItemData("Protection", "e.g. Face mask, disposable gloves", 2)
        ));

        PRESET_ITEMS.put("MEDICINE", Arrays.asList(
                new ItemData("Pain Relievers", "Pack"),
                new ItemData("Vitamins", "Bottles"),
                new ItemData("Cough Syrup", "Bottles"),
                new ItemData("First Aid Kit", "Set")
        ));

        PRESET_ITEMS.put("RELIEF PACKS", Arrays.asList(
                new ItemData("Relief Pack", "Set")
        ));
    }
    // -----------------------------------------------------------


    public Donation_details_fragment() { /* Required empty public constructor */ }

    public static Donation_details_fragment newInstance(String title, String description, String status, int imageRes) {
        Donation_details_fragment fragment = new Donation_details_fragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_STATUS, status);
        args.putInt(ARG_IMAGE, imageRes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fTitle = getArguments().getString(ARG_TITLE);
            fDescription = getArguments().getString(ARG_DESCRIPTION);
            fStatus = getArguments().getString(ARG_STATUS);
            fImageRes = getArguments().getInt(ARG_IMAGE);
            Log.d(TAG, "onCreate: fTitle received: " + fTitle);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        return inflater.inflate(R.layout.donation_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        itemInputsContainer = view.findViewById(R.id.itemInputsContainer);
        btnAddCustomItem = view.findViewById(R.id.btnAddCustomItem);
        customItemInputLayout = view.findViewById(R.id.customItemInputLayout);
        Button btnStep3 = view.findViewById(R.id.btnStep3);

        // --- STEP 3 BUTTON LISTENER (UPDATED FOR NAVIGATION) ---
        btnStep3.setOnClickListener(v -> {
            ArrayList<ItemForSummary> collectedItems = collectAllInputs();

            if (collectedItems.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a quantity for at least one item.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Launch the Summary fragment (Step 3)
            launchSummaryFragment(collectedItems);
        });
        // --------------------------------------------------------

        // Load Preset Items DYNAMICALLY
        final String categoryKey = fTitle != null ? fTitle : "FOOD"; // Capture the category key
        List<ItemData> items = PRESET_ITEMS.get(categoryKey);

        if (items != null && !items.isEmpty()) {
            Log.d(TAG, "onViewCreated: Found " + items.size() + " preset items for " + categoryKey);
            for (ItemData item : items) {
                // Pass the categoryKey when adding a preset item
                addPresetItem(item, categoryKey);
            }
        } else {
            Log.w(TAG, "onViewCreated: No preset items loaded for key: '" + categoryKey + "'.");
        }

        // Set up Add Custom Item button visibility and listener
        if (categoryKey.equals("HYGIENE KITS")) {
            btnAddCustomItem.setVisibility(View.GONE);
        } else {
            btnAddCustomItem.setVisibility(View.VISIBLE);
            btnAddCustomItem.setOnClickListener(v -> {
                if (customItemInputLayout.getChildCount() == 0) {
                    addCustomItemInput();
                    customItemInputLayout.setVisibility(View.VISIBLE);
                    btnAddCustomItem.setVisibility(View.GONE);
                }
            });
        }

        // Set up Category Header
        TextView txtTitle = view.findViewById(R.id.detailsTitle);
        TextView txtDesc = view.findViewById(R.id.detailsDescription);
        TextView txtStatus = view.findViewById(R.id.detailsStatus);

        txtTitle.setText(fTitle != null ? fTitle : "FOOD");
        txtDesc.setText(fDescription != null ? fDescription : "Rice, noodles, and canned goods for nourishment.");
        txtStatus.setText(fStatus != null ? fStatus : "Critical");
    }

    /**
     * Inflates and configures an item_input view based on the ItemData type.
     */
    private void addPresetItem(ItemData item, String categoryKey) {
        View itemView;

        if (item.layoutType == 2) {
            // R.layout.item_input_desc for descriptive items (HYGIENE KITS)
            itemView = inflater.inflate(R.layout.item_input_desc, itemInputsContainer, false);
            TextView txtItemName = itemView.findViewById(R.id.txtItemName);
            TextView txtItemDescription = itemView.findViewById(R.id.txtItemDescription);

            txtItemName.setText(item.name);
            txtItemDescription.setText(item.description);

        } else {
            // R.layout.item_input for standard spinner items
            itemView = inflater.inflate(R.layout.item_input, itemInputsContainer, false);
            TextView txtItemName = itemView.findViewById(R.id.txtItemName);
            Spinner spinnerUnit = itemView.findViewById(R.id.spinnerUnit);

            txtItemName.setText(item.name);
            // Pass categoryKey to filter units
            setupUnitSpinner(spinnerUnit, item.defaultUnit, categoryKey);
        }

        setupQuantityControls(itemView);
        itemInputsContainer.addView(itemView);
    }

    // --- Custom Item Input Logic ---

    private void addCustomItemInput() {
        customItemInputLayout.removeAllViews();

        View customItemView = inflater.inflate(R.layout.item_input, customItemInputLayout, false);

        // Find parent ConstraintLayout inside the CardView
        ConstraintLayout constraintParent = customItemView.findViewById(R.id.constraintLayoutRoot);

        // --- DYNAMICALLY REPLACE TEXTVIEW WITH EDITTEXT (for the Item Name) ---
        TextView presetTxtItemName = customItemView.findViewById(R.id.txtItemName);

        if (constraintParent != null) {
            constraintParent.removeView(presetTxtItemName);

            // Create and configure the custom item name EditText
            EditText customItemEditText = new EditText(requireContext());
            customItemEditText.setId(R.id.txtItemName);
            customItemEditText.setHint("Enter Item");
            customItemEditText.setTextSize(16f);
            customItemEditText.setSingleLine(true);
            customItemEditText.setBackground(null);

            customItemEditText.setLayoutParams(presetTxtItemName.getLayoutParams());
            constraintParent.addView(customItemEditText, 0);
        }

        // --- ADD THE 'X' CLOSE BUTTON ---
        TextView btnPlus = customItemView.findViewById(R.id.btnPlus);
        if (constraintParent != null) {

            TextView btnCloseX = new TextView(requireContext());
            btnCloseX.setId(View.generateViewId());
            btnCloseX.setText("X");
            btnCloseX.setTextSize(18f);
            btnCloseX.setGravity(View.TEXT_ALIGNMENT_CENTER);
            btnCloseX.setTextColor(getResources().getColor(android.R.color.black));

            int dp35 = (int) (35 * getResources().getDisplayMetrics().density);
            int dp8 = (int) (8 * getResources().getDisplayMetrics().density);

            ConstraintLayout.LayoutParams closeParams = new ConstraintLayout.LayoutParams(dp35, dp35);

            closeParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            closeParams.topToTop = btnPlus.getId();
            closeParams.bottomToBottom = btnPlus.getId();
            closeParams.setMarginStart(dp8);

            ConstraintLayout.LayoutParams plusParams = (ConstraintLayout.LayoutParams) btnPlus.getLayoutParams();
            plusParams.endToStart = btnCloseX.getId();

            constraintParent.addView(btnCloseX, closeParams);
            btnPlus.setLayoutParams(plusParams);

            btnCloseX.setOnClickListener(v -> {
                customItemInputLayout.removeAllViews();
                customItemInputLayout.setVisibility(View.GONE);
                btnAddCustomItem.setVisibility(View.VISIBLE);
            });
        }

        // 4. Setup controls
        Spinner spinnerUnit = customItemView.findViewById(R.id.spinnerUnit);
        final String categoryKey = fTitle != null ? fTitle : "FOOD";
        // Use a generic unit for custom items, but filtered by category if possible
        setupUnitSpinner(spinnerUnit, "PCS", categoryKey);

        setupQuantityControls(customItemView); // Uses the updated logic for EditText

        // 5. Add the row
        customItemInputLayout.addView(customItemView);
    }

    // --- HELPER METHODS ---

    /**
     * Configures the Unit Spinner with category-specific unit options.
     */
    private void setupUnitSpinner(Spinner spinner, String defaultUnit, String categoryKey) {

        String[] units;

        // Use the category key to select the appropriate unit set
        switch (categoryKey) {
            case "CASH":
                units = CASH_UNITS;
                break;
            case "FOOD":
                units = FOOD_UNITS;
                break;
            case "MEDICINE":
                units = MEDICINE_UNITS;
                break;
            case "RELIEF PACKS":
                units = RELIEF_PACKS_UNITS;
                break;
            default:
                // Fallback for categories not explicitly listed or custom items
                units = DEFAULT_UNITS;
                break;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                units
        );
        spinner.setAdapter(adapter);

        // Set the default unit selection
        int defaultPosition = adapter.getPosition(defaultUnit);
        if (defaultPosition >= 0) {
            spinner.setSelection(defaultPosition);
        } else if (units.length > 0) {
            // If the default unit isn't in the filtered list, select the first available unit.
            spinner.setSelection(0);
        }
    }

    /**
     * Updates quantity control to use EditText (manual input).
     * This method requires txtQty in XML to be an EditText.
     */
    private void setupQuantityControls(View itemView) {
        TextView btnMinus = itemView.findViewById(R.id.btnMinus);
        // Cast to EditText
        EditText txtQty = itemView.findViewById(R.id.txtQty);
        TextView btnPlus = itemView.findViewById(R.id.btnPlus);

        // Configure EditText properties
        txtQty.setInputType(InputType.TYPE_CLASS_NUMBER);
        // Ensure initial text is set and is a valid number
        if (txtQty.getText().toString().isEmpty()) {
            txtQty.setText("0");
        }

        btnMinus.setOnClickListener(v -> {
            try {
                int currentQty = Integer.parseInt(txtQty.getText().toString());
                if (currentQty > 0) {
                    txtQty.setText(String.valueOf(currentQty - 1));
                }
            } catch (NumberFormatException e) {
                txtQty.setText("0");
            }
        });

        btnPlus.setOnClickListener(v -> {
            try {
                int currentQty = Integer.parseInt(txtQty.getText().toString());
                txtQty.setText(String.valueOf(currentQty + 1));
            } catch (NumberFormatException e) {
                txtQty.setText("1");
            }
        });
    }

    // --- NEW METHODS FOR DATA COLLECTION AND FRAGMENT NAVIGATION ---

    /**
     * Collects data from all item rows (preset and custom) where quantity > 0.
     */
    private ArrayList<ItemForSummary> collectAllInputs() {
        ArrayList<ItemForSummary> items = new ArrayList<>();

        // Check Preset Items Container
        collectInputsFromContainer(itemInputsContainer, items);

        // Check Custom Item Container
        collectInputsFromContainer(customItemInputLayout, items);

        return items;
    }

    /**
     * Helper to iterate through a LinearLayout (itemInputsContainer or customItemInputLayout)
     * and extract the donation details.
     */
    private void collectInputsFromContainer(LinearLayout container, ArrayList<ItemForSummary> items) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View itemView = container.getChildAt(i);

            // Item rows are typically CardView -> ConstraintLayout
            View itemLayout = (itemView instanceof ViewGroup) ? ((ViewGroup) itemView).getChildAt(0) : itemView;

            if (itemLayout instanceof ConstraintLayout) {

                // Get Quantity (EditText)
                EditText txtQty = itemLayout.findViewById(R.id.txtQty);
                if (txtQty == null) continue; // Skip if it doesn't have a quantity EditText

                int quantity = 0;
                try {
                    quantity = Integer.parseInt(txtQty.getText().toString());
                } catch (NumberFormatException ignored) {
                    // Quantity is 0 if not a valid number
                }

                if (quantity > 0) {
                    // Get Item Name (TextView or EditText depending on custom/preset)
                    TextView nameView = itemLayout.findViewById(R.id.txtItemName);
                    String itemName = nameView != null ? nameView.getText().toString() : "Unknown Item";

                    // Get Unit
                    Spinner spinnerUnit = itemLayout.findViewById(R.id.spinnerUnit);
                    String unit = spinnerUnit != null ? (String) spinnerUnit.getSelectedItem() : "";

                    // Skip the row if it's an empty custom item input
                    if (itemName.equals("Enter Item") || itemName.isEmpty()) continue;

                    // Format: e.g., "10KGS" or "60PCS"
                    String quantityUnit = quantity + unit;

                    items.add(new ItemForSummary(itemName, quantityUnit));
                }
            }
        }
    }

    /**
     * Handles the navigation to the Summary Fragment (Step 3).
     * This uses a direct call to the static newInstance method.
     */
    private void launchSummaryFragment(ArrayList<ItemForSummary> collectedItems) {

        // IMPORTANT: Ensure Summary_fragment.java exists and implements the
        // newInstance(ArrayList<ItemForSummary> items) method.

        if (getActivity() != null) {
            try {
                // The safer direct method:
                Fragment summaryFragment = Summary_fragment.newInstance(collectedItems);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, summaryFragment) // Replace R.id.fragment_container with your actual host ID
                        .addToBackStack(null)
                        .commit();
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                // Catches errors if Summary_fragment is missing or misconfigured.
                Log.e(TAG, "Summary_fragment class or newInstance method not found.", e);
                Toast.makeText(getContext(), "ERROR: Step 3 fragment is missing or misconfigured.", Toast.LENGTH_LONG).show();
            }
        }
    }
}