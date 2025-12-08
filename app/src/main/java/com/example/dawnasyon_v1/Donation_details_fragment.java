package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.dawnasyon_v1.Summary_fragment.ItemForSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Donation_details_fragment extends BaseFragment {

  private static final String ARG_TITLE = "arg_title";
  private static final String ARG_DESCRIPTION = "arg_description";
  private static final String ARG_STATUS = "arg_status";
  private static final String ARG_IMAGE = "arg_image";

  private LinearLayout itemInputsContainer;
  private Button btnAddCustomItem;
  private LinearLayout customItemInputLayout;
  private LayoutInflater inflater;

  private String fTitle;
  private String fDescription;
  private String fStatus;
  private int fImageRes;

  // --- Unit Definitions ---
  private static final String[] FOOD_UNITS = {"PCS", "Kilo", "Liter", "Pack"};
  private static final String[] MEDICINE_UNITS = {"Pack", "Bottles", "Set"};
  private static final String[] RELIEF_PACKS_UNITS = {"Set"};
  private static final String[] DEFAULT_UNITS = {"PCS", "Pack", "Set"};

  // --- Data Structure ---
  private static class ItemData {
    String name;
    String defaultUnit;
    String description;
    int layoutType; // 1 = Standard (Spinner), 2 = Description (No Spinner)

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
    PRESET_ITEMS.put("FOOD", Arrays.asList(
            new ItemData("Rice", "Kilo"),
            new ItemData("Instant noodles", "PCS"),
            new ItemData("Canned Goods", "PCS"),
            new ItemData("Biscuits", "PCS")
    ));

    PRESET_ITEMS.put("HYGIENE KITS", Arrays.asList(
            new ItemData("Body Care", "e.g. Soap, shampoo", 2),
            new ItemData("Sanitation", "e.g. Alcohol, wipes", 2),
            new ItemData("Laundry", "e.g. Detergent", 2),
            new ItemData("Protection", "e.g. Masks", 2)
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

  public Donation_details_fragment() { }

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
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    this.inflater = inflater;
    return inflater.inflate(R.layout.donation_details, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final String categoryKey = fTitle != null ? fTitle : "FOOD";

    // 1. CASH LOGIC - Redirect Immediately
    if (categoryKey.equals("CASH")) {
      if (getActivity() != null) {
        Fragment cashFragment = CashInfo_fragment.newInstance(fTitle, fDescription, fStatus, fImageRes);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, cashFragment)
                .addToBackStack(null)
                .commit();
        return;
      }
    }

    // 2. Setup Views
    View btnBack = view.findViewById(R.id.btnBack);
    itemInputsContainer = view.findViewById(R.id.itemInputsContainer);
    btnAddCustomItem = view.findViewById(R.id.btnAddCustomItem);
    customItemInputLayout = view.findViewById(R.id.customItemInputLayout);
    Button btnStep3 = view.findViewById(R.id.btnStep3);

    if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

    TextView txtTitle = view.findViewById(R.id.detailsTitle);
    TextView txtDesc = view.findViewById(R.id.detailsDescription);
    TextView txtStatus = view.findViewById(R.id.detailsStatus);
    ImageView imgIcon = view.findViewById(R.id.detailsImage);

    if(txtTitle != null) txtTitle.setText(fTitle);
    if(txtDesc != null) txtDesc.setText(fDescription);
    if(txtStatus != null) txtStatus.setText(fStatus);
    if(imgIcon != null) imgIcon.setImageResource(fImageRes);

    // 3. Load Presets
    List<ItemData> items = PRESET_ITEMS.get(categoryKey);
    if (items != null) {
      for (ItemData item : items) {
        addPresetItem(item, categoryKey);
      }
    }

    // 4. Custom Item Button Logic
    if (categoryKey.equals("HYGIENE KITS")) {
      if(btnAddCustomItem != null) btnAddCustomItem.setVisibility(View.GONE);
    } else {
      if(btnAddCustomItem != null) {
        btnAddCustomItem.setVisibility(View.VISIBLE);
        btnAddCustomItem.setOnClickListener(v -> {
          addCustomItemInput();
          customItemInputLayout.setVisibility(View.VISIBLE);
        });
      }
    }

    // 5. Next Button Logic
    if(btnStep3 != null) {
      btnStep3.setOnClickListener(v -> {
        ArrayList<ItemForSummary> collectedItems = collectAllInputs();

        // ⭐ FIX: Now requires at least one item for ALL categories
        if (collectedItems.isEmpty()) {
          Toast.makeText(getContext(), "Please enter a quantity for at least one item.", Toast.LENGTH_SHORT).show();
          return;
        }

        launchSummaryFragment(collectedItems);
      });
    }
  }

  private void addPresetItem(ItemData item, String categoryKey) {
    if(itemInputsContainer == null) return;

    View itemView;
    if (item.layoutType == 2) {
      // Description Type (Hygiene Kits)
      itemView = inflater.inflate(R.layout.item_input_desc, itemInputsContainer, false);
      TextView txtName = itemView.findViewById(R.id.txtItemName);
      TextView txtDesc = itemView.findViewById(R.id.txtItemDescription);
      if (txtName != null) txtName.setText(item.name);
      if (txtDesc != null) txtDesc.setText(item.description);
    } else {
      // Standard Type (Food)
      itemView = inflater.inflate(R.layout.item_input, itemInputsContainer, false);
      TextView txtName = itemView.findViewById(R.id.txtItemName);
      Spinner spinner = itemView.findViewById(R.id.spinnerUnit);
      if (txtName != null) txtName.setText(item.name);
      setupUnitSpinner(spinner, item.defaultUnit, categoryKey);
    }
    setupQuantityControls(itemView);
    itemInputsContainer.addView(itemView);
  }

  private void addCustomItemInput() {
    if (customItemInputLayout == null) return;

    View customView = inflater.inflate(R.layout.item_input, customItemInputLayout, false);
    ConstraintLayout parent = customView.findViewById(R.id.constraintLayoutRoot);
    TextView originalName = customView.findViewById(R.id.txtItemName);

    if (parent != null && originalName != null) {
      parent.removeView(originalName);

      EditText editName = new EditText(requireContext());
      editName.setId(R.id.txtItemName);
      editName.setHint("Enter Item");
      editName.setTextSize(16f);
      editName.setBackground(null);
      editName.setLayoutParams(originalName.getLayoutParams());
      parent.addView(editName, 0);

      // Close Button
      TextView btnPlus = customView.findViewById(R.id.btnPlus);
      TextView closeBtn = new TextView(requireContext());
      closeBtn.setText("X");
      closeBtn.setTextSize(18f);
      closeBtn.setPadding(10, 10, 10, 10);

      int dp35 = (int) (35 * getResources().getDisplayMetrics().density);
      ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(dp35, dp35);

      if(btnPlus != null) {
        params.topToTop = btnPlus.getId();
        params.bottomToBottom = btnPlus.getId();
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
      }

      parent.addView(closeBtn, params);
      closeBtn.setOnClickListener(v -> customItemInputLayout.removeView(customView));
    }

    Spinner spinner = customView.findViewById(R.id.spinnerUnit);
    setupUnitSpinner(spinner, "PCS", "FOOD");
    setupQuantityControls(customView);

    customItemInputLayout.addView(customView);
  }

  private void setupQuantityControls(View itemView) {
    View vMinus = itemView.findViewById(R.id.btnMinus);
    View vQty = itemView.findViewById(R.id.txtQty);
    View vPlus = itemView.findViewById(R.id.btnPlus);

    // Safe Check: If controls don't exist in this layout, skip setup
    if (vMinus == null || vQty == null || vPlus == null) return;

    // ⭐ FIX: Treat as TextView to support both EditText (Food) and TextView (Hygiene)
    TextView txtQty = (TextView) vQty;

    if (txtQty.getText().toString().isEmpty()) txtQty.setText("0");

    vMinus.setOnClickListener(v -> {
      try {
        int q = Integer.parseInt(txtQty.getText().toString());
        if (q > 0) txtQty.setText(String.valueOf(q - 1));
      } catch (Exception e) { txtQty.setText("0"); }
    });

    vPlus.setOnClickListener(v -> {
      try {
        int q = Integer.parseInt(txtQty.getText().toString());
        txtQty.setText(String.valueOf(q + 1));
      } catch (Exception e) { txtQty.setText("1"); }
    });
  }

  private void setupUnitSpinner(Spinner spinner, String defaultUnit, String categoryKey) {
    if (spinner == null) return;
    String[] units;
    switch (categoryKey) {
      case "FOOD": units = FOOD_UNITS; break;
      case "MEDICINE": units = MEDICINE_UNITS; break;
      case "RELIEF PACKS": units = RELIEF_PACKS_UNITS; break;
      default: units = DEFAULT_UNITS; break;
    }
    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, units);
    spinner.setAdapter(adapter);
    int pos = adapter.getPosition(defaultUnit);
    if (pos >= 0) spinner.setSelection(pos);
  }

  private ArrayList<ItemForSummary> collectAllInputs() {
    ArrayList<ItemForSummary> items = new ArrayList<>();
    collectInputsFromContainer(itemInputsContainer, items);
    collectInputsFromContainer(customItemInputLayout, items);
    return items;
  }

  private void collectInputsFromContainer(LinearLayout container, ArrayList<ItemForSummary> items) {
    if (container == null) return;
    for (int i = 0; i < container.getChildCount(); i++) {
      View view = container.getChildAt(i);

      // ⭐ FIX: Use TextView here too to prevent ClassCastException
      TextView txtQty = view.findViewById(R.id.txtQty);
      if (txtQty == null) continue;

      int qty = 0;
      try { qty = Integer.parseInt(txtQty.getText().toString()); } catch (Exception e) {}

      if (qty > 0) {
        TextView txtName = view.findViewById(R.id.txtItemName);
        String name = (txtName != null) ? txtName.getText().toString() : "Unknown";

        Spinner spinner = view.findViewById(R.id.spinnerUnit);
        String unit = (spinner != null) ? spinner.getSelectedItem().toString() : "";

        items.add(new ItemForSummary(name, qty + " " + unit));
      }
    }
  }

  private void launchSummaryFragment(ArrayList<ItemForSummary> collectedItems) {
    if (getActivity() != null) {
      Fragment summaryFragment = Summary_fragment.newInstance(collectedItems);
      getActivity().getSupportFragmentManager().beginTransaction()
              .replace(R.id.fragment_container, summaryFragment)
              .addToBackStack(null)
              .commit();
    }
  }
}