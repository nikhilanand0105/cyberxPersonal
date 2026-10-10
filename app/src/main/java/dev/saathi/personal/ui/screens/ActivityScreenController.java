package dev.saathi.personal.ui.screens;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import dev.saathi.model.ThreatSeverity;
import dev.saathi.personal.R;
import dev.saathi.personal.ui.components.DetailSheetDialog;
import dev.saathi.personal.ui.model.ActivityEvent;
import dev.saathi.personal.ui.state.AppState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityScreenController {

    private final Activity activity;
    private final float density;
    private ScrollView rootView;

    private EditText etSearch;
    private LinearLayout containerChips;
    private LinearLayout containerEventsList;
    private TextView tvEmptyState;

    private String currentSearchQuery = "";
    private ActivityEvent.EventCategory selectedCategoryFilter = null; // null = ALL

    public ActivityScreenController(Activity activity) {
        this.activity = activity;
        this.density = activity.getResources().getDisplayMetrics().density;
        createView();
    }

    public View getView() {
        return rootView;
    }

    private void createView() {
        rootView = new ScrollView(activity);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootView.setFillViewport(true);
        rootView.setBackgroundColor(0xFFF7F7F5);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (20 * density), (int) (16 * density), (int) (20 * density), (int) (100 * density));

        // 1. Header
        TextView title = new TextView(activity);
        title.setText("SECURITY AUDIT & ACTIVITY");
        title.setTextSize(22);
        title.setTextColor(0xFF141414);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        TextView sub = new TextView(activity);
        sub.setText("Chronological forensic telemetry and threat interceptions stored locally on-device");
        sub.setTextSize(13);
        sub.setTextColor(0xFF666666);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sp.topMargin = (int) (4 * density);
        sp.bottomMargin = (int) (16 * density);
        sub.setLayoutParams(sp);
        layout.addView(sub);

        // 2. Search Bar
        etSearch = new EditText(activity);
        etSearch.setHint("🔍 Search alerts, DEX hooks, domains...");
        etSearch.setHintTextColor(0xFF888888);
        etSearch.setTextColor(0xFF141414);
        etSearch.setTextSize(13);
        etSearch.setBackgroundResource(R.drawable.bg_input_field);
        etSearch.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                render();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        layout.addView(etSearch);

        // 3. Category Filter Chips (Horizontal Scroll)
        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hp.topMargin = (int) (12 * density);
        hp.bottomMargin = (int) (16 * density);
        hsv.setLayoutParams(hp);

        containerChips = new LinearLayout(activity);
        containerChips.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(containerChips);
        layout.addView(hsv);

        buildChips();

        // 4. Events Container
        containerEventsList = new LinearLayout(activity);
        containerEventsList.setOrientation(LinearLayout.VERTICAL);
        layout.addView(containerEventsList);

        // 5. Empty State
        tvEmptyState = new TextView(activity);
        tvEmptyState.setText("✅ NO MATCHING SECURITY EVENTS\nAll monitored vectors are safe and nominal.");
        tvEmptyState.setGravity(Gravity.CENTER);
        tvEmptyState.setTextSize(14);
        tvEmptyState.setTextColor(0xFF888888);
        tvEmptyState.setLineSpacing(0, 1.3f);
        tvEmptyState.setPadding(0, (int) (40 * density), 0, (int) (40 * density));
        tvEmptyState.setVisibility(View.GONE);
        layout.addView(tvEmptyState);

        rootView.addView(layout);
        render();
    }

    private void buildChips() {
        containerChips.removeAllViews();

        containerChips.addView(createFilterChip("ALL", null));
        containerChips.addView(createFilterChip("MESSAGES", ActivityEvent.EventCategory.SENSE));
        containerChips.addView(createFilterChip("LINKS", ActivityEvent.EventCategory.PHISHING));
        containerChips.addView(createFilterChip("FILES / APKS", ActivityEvent.EventCategory.APK));
        containerChips.addView(createFilterChip("DEVICE", ActivityEvent.EventCategory.FIREWALL));
    }

    private Button createFilterChip(String label, ActivityEvent.EventCategory category) {
        Button chip = new Button(activity);
        chip.setText(label);
        chip.setTextSize(11);
        boolean isSelected = (selectedCategoryFilter == category);

        if (isSelected) {
            chip.setTextColor(0xFFFFFFFF);
            chip.setBackgroundResource(R.drawable.bg_pill_black);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setTextColor(0xFF666666);
            chip.setBackgroundResource(R.drawable.bg_pill_dark);
        }

        chip.setOnClickListener(v -> {
            selectedCategoryFilter = category;
            buildChips();
            render();
        });

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (int) (36 * density)
        );
        cp.rightMargin = (int) (8 * density);
        chip.setLayoutParams(cp);
        return chip;
    }

    public void render() {
        containerEventsList.removeAllViews();
        boolean senior = AppState.get().isSeniorMode();
        List<ActivityEvent> allEvents = AppState.get().getActivityEvents();
        List<ActivityEvent> filtered = new ArrayList<>();

        for (ActivityEvent ev : allEvents) {
            // Category filter
            if (selectedCategoryFilter != null && ev.getCategory() != selectedCategoryFilter) {
                continue;
            }
            // Search text filter
            if (!currentSearchQuery.isEmpty()) {
                String title = ev.getTitle().toLowerCase(Locale.ROOT);
                String sum = ev.getSummary().toLowerCase(Locale.ROOT);
                if (!title.contains(currentSearchQuery) && !sum.contains(currentSearchQuery)) {
                    continue;
                }
            }
            filtered.add(ev);
        }

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            for (ActivityEvent ev : filtered) {
                containerEventsList.addView(createEventCard(ev, senior));
            }
        }
    }

    private View createEventCard(ActivityEvent ev, boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (12 * density);
        card.setLayoutParams(params);
        card.setOnClickListener(v -> DetailSheetDialog.showEventDetails(activity, ev));

        // Top row: Category, Severity, Timestamp
        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvCategory = new TextView(activity);
        tvCategory.setText(ev.getCategory().getDisplayName().toUpperCase());
        tvCategory.setTextSize(senior ? 12 : 10);
        tvCategory.setTextColor(0xFF141414);
        tvCategory.setBackgroundResource(R.drawable.bg_pill_dark);
        tvCategory.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
        topRow.addView(tvCategory);

        TextView tvSeverity = new TextView(activity);
        LinearLayout.LayoutParams svp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        svp.leftMargin = (int) (6 * density);
        tvSeverity.setLayoutParams(svp);
        tvSeverity.setText(ev.getSeverity().name());
        tvSeverity.setTextSize(senior ? 12 : 10);
        tvSeverity.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));

        if (ev.getSeverity() == ThreatSeverity.CRITICAL) {
            tvSeverity.setTextColor(0xFFA63232);
            tvSeverity.setBackgroundResource(R.drawable.bg_pill_crimson);
        } else if (ev.getSeverity() == ThreatSeverity.HIGH) {
            tvSeverity.setTextColor(0xFF855A12);
            tvSeverity.setBackgroundResource(R.drawable.bg_pill_amber);
        } else {
            tvSeverity.setTextColor(0xFF386B57);
            tvSeverity.setBackgroundResource(R.drawable.bg_pill_emerald);
        }
        topRow.addView(tvSeverity);

        View space = new View(activity);
        topRow.addView(space, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView tvTime = new TextView(activity);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvTime.setText(sdf.format(ev.getTimestamp()));
        tvTime.setTextSize(senior ? 12 : 11);
        tvTime.setTextColor(0xFF888888);
        topRow.addView(tvTime);

        card.addView(topRow);

        // Title
        TextView tvTitle = new TextView(activity);
        tvTitle.setText(ev.getTitle());
        tvTitle.setTextSize(senior ? 17 : 15);
        tvTitle.setTextColor(0xFF141414);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tp.topMargin = (int) (8 * density);
        tvTitle.setLayoutParams(tp);
        card.addView(tvTitle);

        // Summary
        TextView tvSum = new TextView(activity);
        tvSum.setText(ev.getSummary());
        tvSum.setTextSize(senior ? 14 : 12);
        tvSum.setTextColor(0xFF666666);
        tvSum.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sp.topMargin = (int) (4 * density);
        tvSum.setLayoutParams(sp);
        card.addView(tvSum);

        // Remediation Link
        TextView tvAction = new TextView(activity);
        tvAction.setText(ev.getActionLabel() + " →");
        tvAction.setTextSize(senior ? 14 : 12);
        tvAction.setTextColor(0xFF141414);
        tvAction.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        ap.topMargin = (int) (10 * density);
        tvAction.setLayoutParams(ap);
        card.addView(tvAction);

        return card;
    }
}
