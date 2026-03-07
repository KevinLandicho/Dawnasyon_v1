package com.example.dawnasyon_v1;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.textfield.TextInputEditText;

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

  public static String currentDonationType = "";
  public static String currentItemDescription = "";

  // ⭐ STATE PRESERVATION CACHE
  // This keeps the data alive even if the fragment is destroyed and recreated
  private static final Map<String, String> savedQuantities = new HashMap<>();
  private static final Map<String, Integer> savedUnits = new HashMap<>();
  private static final List<CustomItemEntry> savedCustomItems = new ArrayList<>();

  private static class CustomItemEntry {
    String name;
    int unitPos;
    String qty;
    CustomItemEntry(String name, int unitPos, String qty) {
      this.name = name; this.unitPos = unitPos; this.qty = qty;
    }
  }

  private static final String[] UNITS_WEIGHT = {"Kilo", "Sack", "Grams", "Tons"};
  private static final String[] UNITS_PIECES = {"PCS", "Box", "Case", "Tray"};
  private static final String[] UNITS_LIQUID = {"Liter", "Bottle", "Gallon", "Box"};
  private static final String[] UNITS_PACKS  = {"Pack", "Set", "Box"};
  private static final String[] UNITS_GENERIC = {"PCS", "Set", "Box"};

  private static class ItemData {
    String name;
    String[] specificUnits;
    String description;
    int layoutType;

    ItemData(String name, String[] specificUnits) {
      this.name = name;
      this.specificUnits = specificUnits;
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
            new ItemData("Rice", UNITS_WEIGHT),
            new ItemData("Instant noodles", UNITS_PIECES),
            new ItemData("Canned Goods", UNITS_PIECES),
            new ItemData("Biscuits", UNITS_PACKS),
            new ItemData("Water", UNITS_LIQUID)
    ));

    PRESET_ITEMS.put("HYGIENE KITS", Arrays.asList(
            new ItemData("Body Care", "e.g. Soap, shampoo", 2),
            new ItemData("Sanitation", "e.g. Alcohol, wipes", 2),
            new ItemData("Laundry", "e.g. Detergent", 2),
            new ItemData("Protection", "e.g. Masks", 2)
    ));

    PRESET_ITEMS.put("MEDICINE", Arrays.asList(
            new ItemData("Pain Relievers", UNITS_PACKS),
            new ItemData("Vitamins", UNITS_LIQUID),
            new ItemData("Cough Syrup", UNITS_LIQUID),
            new ItemData("First Aid Kit", UNITS_PACKS)
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

    // Logic for cleaning cache if category changes
    if (!currentDonationType.equals(categoryKey)) {
      savedQuantities.clear();
      savedUnits.clear();
      savedCustomItems.clear();
      currentDonationType = categoryKey;
    }

    View btnBack = view.findViewById(R.id.btnBack);
    itemInputsContainer = view.findViewById(R.id.itemInputsContainer);
    btnAddCustomItem = view.findViewById(R.id.btnAddCustomItem);
    customItemInputLayout = view.findViewById(R.id.customItemInputLayout);
    Button btnStep3 = view.findViewById(R.id.btnStep3);

    LinearLayout reliefPackContainer = view.findViewById(R.id.reliefPackContainer);
    TextInputEditText etPackQuantity = view.findViewById(R.id.etPackQuantity);
    TextInputEditText etPackContents = view.findViewById(R.id.etPackContents);

    if (btnBack != null) btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

    TextView txtTitle = view.findViewById(R.id.detailsTitle);
    TextView txtDesc = view.findViewById(R.id.detailsDescription);
    TextView txtStatus = view.findViewById(R.id.detailsStatus);
    ImageView imgIcon = view.findViewById(R.id.detailsImage);

    if(txtTitle != null) txtTitle.setText(fTitle);
    if(txtDesc != null) txtDesc.setText(fDescription);
    if(txtStatus != null) txtStatus.setText(fStatus);
    if(imgIcon != null) imgIcon.setImageResource(fImageRes);

    if (categoryKey.equalsIgnoreCase("RELIEF PACKS")) {
      reliefPackContainer.setVisibility(View.VISIBLE);
      itemInputsContainer.setVisibility(View.GONE);
      btnAddCustomItem.setVisibility(View.GONE);
    } else {
      reliefPackContainer.setVisibility(View.GONE);
      itemInputsContainer.setVisibility(View.VISIBLE);

      // Load Presets
      List<ItemData> items = PRESET_ITEMS.get(categoryKey);
      if (items != null) {
        for (ItemData item : items) {
          addPresetItem(item);
        }
      }

      // Load Custom Items from Cache
      for (CustomItemEntry entry : savedCustomItems) {
        addCustomItemInput(entry);
      }

      if (categoryKey.equals("HYGIENE KITS")) {
        btnAddCustomItem.setVisibility(View.GONE);
      } else {
        btnAddCustomItem.setVisibility(View.VISIBLE);
        btnAddCustomItem.setOnClickListener(v -> {
          addCustomItemInput(null);
          customItemInputLayout.setVisibility(View.VISIBLE);
        });
      }
    }

    btnStep3.setOnClickListener(v -> {
      ArrayList<ItemForSummary> collectedItems = new ArrayList<>();
      if (categoryKey.equalsIgnoreCase("RELIEF PACKS")) {
        String quantityStr = etPackQuantity.getText().toString().trim();
        String contents = etPackContents.getText().toString().trim();
        if (quantityStr.isEmpty() || contents.isEmpty()) {
          Toast.makeText(getContext(), "Fill in all fields.", Toast.LENGTH_SHORT).show();
          return;
        }
        collectedItems.add(new ItemForSummary("Relief Pack", quantityStr + " Pack(s)"));
        currentItemDescription = contents;
      } else {
        collectedItems = collectAllInputs();
        if (collectedItems == null || collectedItems.isEmpty()) {
          Toast.makeText(getContext(), "Add at least one item.", Toast.LENGTH_SHORT).show();
          return;
        }
      }
      launchSummaryFragment(collectedItems);
    });

    applyTagalogTranslation(view);
  }

  private void addPresetItem(ItemData item) {
    View itemView = inflater.inflate(item.layoutType == 2 ? R.layout.item_input_desc : R.layout.item_input, itemInputsContainer, false);
    TextView txtName = itemView.findViewById(R.id.txtItemName);
    txtName.setText(item.name);

    if (item.layoutType == 1) {
      Spinner spinner = itemView.findViewById(R.id.spinnerUnit);
      setupUnitSpinner(spinner, item.specificUnits, item.name);
    } else {
      TextView txtDesc = itemView.findViewById(R.id.txtItemDescription);
      txtDesc.setText(item.description);
    }

    setupQuantityControls(itemView, item.name);
    itemInputsContainer.addView(itemView);
  }

  private void addCustomItemInput(@Nullable CustomItemEntry entry) {
    View customView = inflater.inflate(R.layout.item_input, customItemInputLayout, false);
    ConstraintLayout parent = customView.findViewById(R.id.constraintLayoutRoot);
    TextView originalNameLabel = customView.findViewById(R.id.txtItemName);
    parent.removeView(originalNameLabel);

    EditText editName = new EditText(requireContext());
    editName.setId(View.generateViewId());
    editName.setHint("Enter Item");
    editName.setTextSize(16f);
    if (entry != null) editName.setText(entry.name);

    TextView closeBtn = new TextView(requireContext());
    closeBtn.setId(View.generateViewId());
    closeBtn.setText("✕");
    closeBtn.setTextColor(android.graphics.Color.RED);
    closeBtn.setPadding(20, 10, 20, 10);

    // Layout Params for X and Edit
    ConstraintLayout.LayoutParams clParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    clParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
    clParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
    clParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
    closeBtn.setLayoutParams(clParams);
    parent.addView(closeBtn);

    ConstraintLayout.LayoutParams editParams = new ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
    editParams.startToEnd = closeBtn.getId();
    editParams.endToStart = R.id.spinnerUnit;
    editParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
    editParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
    editParams.setMarginStart(10);
    editName.setLayoutParams(editParams);
    parent.addView(editName);

    Spinner spinner = customView.findViewById(R.id.spinnerUnit);
    setupUnitSpinner(spinner, UNITS_GENERIC, null);
    if (entry != null) spinner.setSelection(entry.unitPos);

    TextView txtQty = customView.findViewById(R.id.txtQty);
    if (entry != null) txtQty.setText(entry.qty);

    setupQuantityControls(customView, null);
    closeBtn.setOnClickListener(v -> customItemInputLayout.removeView(customView));
    customItemInputLayout.addView(customView);
  }

  private void setupQuantityControls(View itemView, @Nullable String itemName) {
    TextView txtQty = itemView.findViewById(R.id.txtQty);

    // Restore from cache if preset
    if (itemName != null && savedQuantities.containsKey(itemName)) {
      txtQty.setText(savedQuantities.get(itemName));
    }

    itemView.findViewById(R.id.btnPlus).setOnClickListener(v -> {
      int q = Integer.parseInt(txtQty.getText().toString()) + 1;
      txtQty.setText(String.valueOf(q));
      if (itemName != null) savedQuantities.put(itemName, String.valueOf(q));
    });

    itemView.findViewById(R.id.btnMinus).setOnClickListener(v -> {
      int q = Math.max(0, Integer.parseInt(txtQty.getText().toString()) - 1);
      txtQty.setText(String.valueOf(q));
      if (itemName != null) savedQuantities.put(itemName, String.valueOf(q));
    });
  }

  private void setupUnitSpinner(Spinner spinner, String[] units, @Nullable String itemName) {
    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, units);
    spinner.setAdapter(adapter);
    if (itemName != null && savedUnits.containsKey(itemName)) {
      spinner.setSelection(savedUnits.get(itemName));
    }
  }

  private ArrayList<ItemForSummary> collectAllInputs() {
    ArrayList<ItemForSummary> items = new ArrayList<>();
    savedCustomItems.clear(); // Refresh custom cache on step 3 click

    // Collect Presets & Save Units
    for (int i = 0; i < itemInputsContainer.getChildCount(); i++) {
      View v = itemInputsContainer.getChildAt(i);
      TextView nameTv = v.findViewById(R.id.txtItemName);
      TextView qtyTv = v.findViewById(R.id.txtQty);
      Spinner unitSp = v.findViewById(R.id.spinnerUnit);

      String name = nameTv.getText().toString();
      String qty = qtyTv.getText().toString();

      if (unitSp != null) savedUnits.put(name, unitSp.getSelectedItemPosition());

      if (!qty.equals("0")) {
        String unit = (unitSp != null) ? unitSp.getSelectedItem().toString() : "";
        items.add(new ItemForSummary(name, qty + " " + unit));
      }
    }

    // Collect Customs & Update Cache
    for (int i = 0; i < customItemInputLayout.getChildCount(); i++) {
      View v = customItemInputLayout.getChildAt(i);
      EditText nameEt = null;
      // Find the dynamic EditText
      for(int j=0; j<((ConstraintLayout)v.findViewById(R.id.constraintLayoutRoot)).getChildCount(); j++) {
        View child = ((ConstraintLayout)v.findViewById(R.id.constraintLayoutRoot)).getChildAt(j);
        if(child instanceof EditText) nameEt = (EditText)child;
      }

      TextView qtyTv = v.findViewById(R.id.txtQty);
      Spinner unitSp = v.findViewById(R.id.spinnerUnit);

      if (nameEt != null) {
        String name = nameEt.getText().toString().trim();
        String qty = qtyTv.getText().toString();
        int unitPos = unitSp.getSelectedItemPosition();

        if (!name.isEmpty()) {
          savedCustomItems.add(new CustomItemEntry(name, unitPos, qty));
          if (!qty.equals("0")) {
            items.add(new ItemForSummary(name, qty + " " + unitSp.getSelectedItem().toString()));
          }
        }
      }
    }
    return items;
  }

  private void launchSummaryFragment(ArrayList<ItemForSummary> collectedItems) {
    Fragment summaryFragment = Summary_fragment.newInstance(collectedItems);
    getActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, summaryFragment)
            .addToBackStack(null)
            .commit();
  }
}