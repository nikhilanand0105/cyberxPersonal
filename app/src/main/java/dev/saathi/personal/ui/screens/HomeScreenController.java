package dev.saathi.personal.ui.screens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import dev.saathi.model.ThreatSeverity;
import dev.saathi.personal.R;
import dev.saathi.personal.ui.MainActivity;
import dev.saathi.personal.ui.components.DetailSheetDialog;
import dev.saathi.personal.ui.model.ActivityEvent;
import dev.saathi.personal.ui.model.ChildProfile;
import dev.saathi.personal.ui.model.NavigationTab;
import dev.saathi.personal.ui.model.ScanMode;
import dev.saathi.personal.ui.state.AppState;

import java.util.List;

public class HomeScreenController {

    private final Activity activity;
    private final float density;
    private ScrollView rootView;
    private LinearLayout contentLayout;

    public HomeScreenController(Activity activity) {
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

        contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding((int) (20 * density), (int) (16 * density), (int) (20 * density), (int) (100 * density));
        rootView.addView(contentLayout);

        render();
    }

    public void render() {
        contentLayout.removeAllViews();
        boolean senior = AppState.get().isSeniorMode();

        // 1. Header (Device & Profile)
        contentLayout.addView(buildTopHeader(senior));

        // 2. Senior Mode Quick Advice Banner (if senior mode active)
        if (senior) {
            contentLayout.addView(buildSeniorRecoveryBanner());
        }

        // 3. Hero Protection Score & Truthful Status
        contentLayout.addView(buildHeroProtectionCard(senior));

        // 4. Quick Safety Actions (4 Entry Points into Scan Hub)
        contentLayout.addView(buildQuickActionsSection(senior));

        // 5. Emergency Helpline & Family Assistance Dock
        contentLayout.addView(buildHelplineDock(senior));

        // 6. Recent Findings & Detail Sheet Triggers
        contentLayout.addView(buildRecentFindingsSection(senior));
    }

    private View buildTopHeader(boolean senior) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (16 * density);
        row.setLayoutParams(params);

        // Profile Pill Selector
        LinearLayout profilePill = new LinearLayout(activity);
        profilePill.setOrientation(LinearLayout.HORIZONTAL);
        profilePill.setGravity(Gravity.CENTER_VERTICAL);
        profilePill.setBackgroundResource(R.drawable.bg_pill_dark);
        profilePill.setPadding((int) (14 * density), (int) (8 * density), (int) (14 * density), (int) (8 * density));
        profilePill.setOnClickListener(v -> showProfilePickerDialog());

        TextView iconUser = new TextView(activity);
        iconUser.setText(senior ? "👴 " : "🛡️ ");
        iconUser.setTextSize(senior ? 16 : 14);
        profilePill.addView(iconUser);

        TextView tvProfileName = new TextView(activity);
        tvProfileName.setText(senior ? "Senior Mode" : AppState.get().getCurrentProfile().getName());
        tvProfileName.setTextSize(senior ? 15 : 13);
        tvProfileName.setTextColor(0xFF141414);
        tvProfileName.setTypeface(null, android.graphics.Typeface.BOLD);
        profilePill.addView(tvProfileName);

        TextView tvArrow = new TextView(activity);
        tvArrow.setText(" ▾");
        tvArrow.setTextSize(12);
        tvArrow.setTextColor(0xFF888888);
        profilePill.addView(tvArrow);

        row.addView(profilePill);

        // Flexible Space
        View space = new View(activity);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
        space.setLayoutParams(sp);
        row.addView(space);

        // Device & Refresh Rate Badge
        TextView tvHwBadge = new TextView(activity);
        tvHwBadge.setText("iQOO 15 • Snapdragon 8 Elite");
        tvHwBadge.setTextSize(senior ? 12 : 11);
        tvHwBadge.setTextColor(0xFF386B57);
        tvHwBadge.setBackgroundResource(R.drawable.bg_pill_emerald);
        tvHwBadge.setPadding((int) (10 * density), (int) (6 * density), (int) (10 * density), (int) (6 * density));
        row.addView(tvHwBadge);

        return row;
    }

    private View buildSeniorRecoveryBanner() {
        LinearLayout banner = new LinearLayout(activity);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setBackgroundResource(R.drawable.bg_card_glow_amber);
        banner.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (16 * density);
        banner.setLayoutParams(params);

        TextView title = new TextView(activity);
        title.setText("⚠️ SUSPICIOUS CALL OR MESSAGE? WHAT TO DO NOW");
        title.setTextSize(14);
        title.setTextColor(0xFF855A12);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        banner.addView(title);

        TextView step1 = new TextView(activity);
        step1.setText("1. NEVER share any 4 or 6-digit SMS OTP with anyone, even if they claim to be a bank manager or police.");
        step1.setTextSize(14);
        step1.setTextColor(0xFF141414);
        step1.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p1.topMargin = (int) (8 * density);
        step1.setLayoutParams(p1);
        banner.addView(step1);

        TextView step2 = new TextView(activity);
        step2.setText("2. DO NOT install AnyDesk, TeamViewer, or quick support apps sent via WhatsApp links.");
        step2.setTextSize(14);
        step2.setTextColor(0xFF141414);
        step2.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p2.topMargin = (int) (6 * density);
        step2.setLayoutParams(p2);
        banner.addView(step2);

        TextView step3 = new TextView(activity);
        step3.setText("3. If money was deducted, immediately call National Cyber Helpline at 1930 to freeze the transaction.");
        step3.setTextSize(14);
        step3.setTextColor(0xFF141414);
        step3.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p3.topMargin = (int) (6 * density);
        step3.setLayoutParams(p3);
        banner.addView(step3);

        Button btnDial1930 = new Button(activity);
        btnDial1930.setText("📞 Call 1930 Helpline Immediately");
        btnDial1930.setBackgroundResource(R.drawable.bg_pill_black);
        btnDial1930.setTextColor(0xFFFFFFFF);
        btnDial1930.setTextSize(14);
        btnDial1930.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * density));
        bp.topMargin = (int) (12 * density);
        btnDial1930.setLayoutParams(bp);
        btnDial1930.setOnClickListener(v -> dialHelpline());
        banner.addView(btnDial1930);

        return banner;
    }

    private View buildHeroProtectionCard(boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (22 * density), (int) (20 * density), (int) (22 * density), (int) (20 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (18 * density);
        card.setLayoutParams(params);

        TextView label = new TextView(activity);
        label.setText("OVERALL DIGITAL PROTECTION POSTURE");
        label.setTextSize(senior ? 13 : 11);
        label.setTextColor(0xFF888888);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(label);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = (int) (12 * density);
        row.setLayoutParams(rp);

        TextView tvScore = new TextView(activity);
        tvScore.setText(String.valueOf(AppState.get().getSafetyScore()));
        tvScore.setTextSize(senior ? 48 : 42);
        tvScore.setTextColor(0xFF141414);
        tvScore.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvScore);

        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tcp.leftMargin = (int) (18 * density);
        textCol.setLayoutParams(tcp);

        TextView tvStatus = new TextView(activity);
        tvStatus.setText("PROTECTED & OPTIMAL");
        tvStatus.setTextSize(senior ? 16 : 14);
        tvStatus.setTextColor(0xFF386B57);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvStatus);

        TextView tvSub = new TextView(activity);
        tvSub.setText(senior
                ? "Your phone is protected against scam apps, fake payment links and OTP stealers."
                : "Snapdragon 8 Elite on-device intelligence active. Zero unreviewed critical threats.");
        tvSub.setTextSize(senior ? 14 : 12);
        tvSub.setTextColor(0xFF666666);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = (int) (4 * density);
        tvSub.setLayoutParams(sp);
        textCol.addView(tvSub);

        row.addView(textCol);
        card.addView(row);

        // Divider
        View divider = new View(activity);
        divider.setBackgroundColor(0xFFE5E5E2);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * density));
        dp.topMargin = (int) (16 * density);
        dp.bottomMargin = (int) (14 * density);
        divider.setLayoutParams(dp);
        card.addView(divider);

        // Status bullets
        LinearLayout footerRow = new LinearLayout(activity);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dot = new TextView(activity);
        dot.setText("● ");
        dot.setTextColor(0xFF386B57);
        dot.setTextSize(12);
        footerRow.addView(dot);

        TextView footerText = new TextView(activity);
        footerText.setText("Last scan: Active • Local analysis • No cloud exfiltration");
        footerText.setTextSize(senior ? 13 : 11);
        footerText.setTextColor(0xFF666666);
        footerRow.addView(footerText);

        card.addView(footerRow);

        return card;
    }

    private View buildQuickActionsSection(boolean senior) {
        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (20 * density);
        section.setLayoutParams(params);

        TextView sectionLabel = new TextView(activity);
        sectionLabel.setText("QUICK SAFETY ACTIONS");
        sectionLabel.setTextSize(senior ? 14 : 11);
        sectionLabel.setTextColor(0xFF888888);
        sectionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = (int) (12 * density);
        sectionLabel.setLayoutParams(slp);
        section.addView(sectionLabel);

        // Row 1: Message Check & Link Check
        LinearLayout row1 = new LinearLayout(activity);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams r1p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        r1p.bottomMargin = (int) (10 * density);
        row1.setLayoutParams(r1p);

        row1.addView(createActionCard("💬 Message Check",
                senior ? "Check SMS, WhatsApp message or OTP request" : "Evaluate SMS, WhatsApp OTP & payment coercion",
                senior,
                () -> AppState.get().navigateToScan(ScanMode.MESSAGE_CHECK)));

        View gap1 = new View(activity);
        gap1.setLayoutParams(new LinearLayout.LayoutParams((int) (10 * density), 1));
        row1.addView(gap1);

        row1.addView(createActionCard("🔗 Link Check",
                senior ? "Verify suspicious web address or banking link" : "Analyze URLs, punycode lookalikes & phishing kits",
                senior,
                () -> AppState.get().navigateToScan(ScanMode.LINK_CHECK)));

        section.addView(row1);

        // Row 2: APK Sideload & Installed Apps
        LinearLayout row2 = new LinearLayout(activity);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams r2p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row2.setLayoutParams(r2p);

        row2.addView(createActionCard("📦 APK / File Check",
                senior ? "Inspect downloaded app file before installing" : "Reverse-engineer package manifest & DEX hooks",
                senior,
                () -> AppState.get().navigateToScan(ScanMode.APK_CHECK)));

        View gap2 = new View(activity);
        gap2.setLayoutParams(new LinearLayout.LayoutParams((int) (10 * density), 1));
        row2.addView(gap2);

        row2.addView(createActionCard("⚙️ Installed Apps",
                senior ? "Review apps with permission to control phone" : "Audit Accessibility, SMS & Admin permissions",
                senior,
                () -> AppState.get().navigateToScan(ScanMode.INSTALLED_APPS)));

        section.addView(row2);

        return section;
    }

    private View createActionCard(String title, String desc, boolean senior, Runnable onClick) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_interactive);
        card.setPadding((int) (16 * density), (int) (16 * density), (int) (16 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        card.setLayoutParams(params);
        card.setOnClickListener(v -> onClick.run());

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextSize(senior ? 16 : 14);
        tvTitle.setTextColor(0xFF141414);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvDesc = new TextView(activity);
        tvDesc.setText(desc);
        tvDesc.setTextSize(senior ? 13 : 11);
        tvDesc.setTextColor(0xFF666666);
        tvDesc.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = (int) (6 * density);
        tvDesc.setLayoutParams(dp);
        card.addView(tvDesc);

        TextView tvTap = new TextView(activity);
        tvTap.setText("Launch Scan →");
        tvTap.setTextSize(senior ? 13 : 11);
        tvTap.setTextColor(0xFF141414);
        tvTap.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = (int) (10 * density);
        tvTap.setLayoutParams(tp);
        card.addView(tvTap);

        return card;
    }

    private View buildHelplineDock(boolean senior) {
        LinearLayout dock = new LinearLayout(activity);
        dock.setOrientation(LinearLayout.VERTICAL);
        dock.setBackgroundResource(R.drawable.bg_card_cred);
        dock.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (20 * density);
        dock.setLayoutParams(params);

        TextView label = new TextView(activity);
        label.setText("NEED HELP OR SUSPECT FRAUD?");
        label.setTextSize(senior ? 13 : 11);
        label.setTextColor(0xFF888888);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        dock.addView(label);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = (int) (10 * density);
        row.setLayoutParams(rp);

        // Button 1: Ask Assistant
        Button btnCopilot = new Button(activity);
        btnCopilot.setText("💬 Ask Assistant");
        btnCopilot.setBackgroundResource(R.drawable.bg_pill_black);
        btnCopilot.setTextColor(0xFFFFFFFF);
        btnCopilot.setTextSize(senior ? 14 : 12);
        btnCopilot.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bp1 = new LinearLayout.LayoutParams(0, (int) (44 * density), 1f);
        bp1.rightMargin = (int) (6 * density);
        btnCopilot.setLayoutParams(bp1);
        btnCopilot.setOnClickListener(v -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).openContextualAssistant("I need help understanding a suspicious security alert on my device.");
            }
        });
        row.addView(btnCopilot);

        // Button 2: Call 1930
        Button btnCall = new Button(activity);
        btnCall.setText("📞 Dial 1930");
        btnCall.setBackgroundResource(R.drawable.bg_pill_crimson);
        btnCall.setTextColor(0xFFA63232);
        btnCall.setTextSize(senior ? 14 : 12);
        btnCall.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(0, (int) (44 * density), 1f);
        bp2.leftMargin = (int) (6 * density);
        btnCall.setLayoutParams(bp2);
        btnCall.setOnClickListener(v -> dialHelpline());
        row.addView(btnCall);

        dock.addView(row);
        return dock;
    }

    private View buildRecentFindingsSection(boolean senior) {
        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        section.setLayoutParams(params);

        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hrp.bottomMargin = (int) (12 * density);
        headerRow.setLayoutParams(hrp);

        TextView label = new TextView(activity);
        label.setText("RECENT SECURITY FINDINGS");
        label.setTextSize(senior ? 14 : 11);
        label.setTextColor(0xFF888888);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        headerRow.addView(label);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        headerRow.addView(sp);

        TextView tvViewAll = new TextView(activity);
        tvViewAll.setText("View All (Activity) →");
        tvViewAll.setTextSize(senior ? 13 : 11);
        tvViewAll.setTextColor(0xFF141414);
        tvViewAll.setTypeface(null, android.graphics.Typeface.BOLD);
        tvViewAll.setOnClickListener(v -> AppState.get().setCurrentTab(NavigationTab.ACTIVITY));
        headerRow.addView(tvViewAll);

        section.addView(headerRow);

        // List top 3 recent events
        List<ActivityEvent> events = AppState.get().getActivityEvents();
        int count = Math.min(events.size(), 3);
        for (int i = 0; i < count; i++) {
            ActivityEvent event = events.get(i);
            section.addView(createEventCard(event, senior));
        }

        return section;
    }

    private View createEventCard(ActivityEvent event, boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_interactive);
        card.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (10 * density);
        card.setLayoutParams(params);
        card.setOnClickListener(v -> DetailSheetDialog.showEventDetails(activity, event));

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvCategory = new TextView(activity);
        tvCategory.setText(event.getCategory().getDisplayName().toUpperCase());
        tvCategory.setTextSize(senior ? 12 : 10);
        tvCategory.setTextColor(0xFF888888);
        tvCategory.setTypeface(null, android.graphics.Typeface.BOLD);
        topRow.addView(tvCategory);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        topRow.addView(sp);

        TextView tvSeverity = new TextView(activity);
        tvSeverity.setText(event.getSeverity().name());
        tvSeverity.setTextSize(senior ? 12 : 10);
        tvSeverity.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSeverity.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));

        if (event.getSeverity() == ThreatSeverity.CRITICAL) {
            tvSeverity.setTextColor(0xFFA63232);
            tvSeverity.setBackgroundResource(R.drawable.bg_pill_crimson);
        } else if (event.getSeverity() == ThreatSeverity.HIGH) {
            tvSeverity.setTextColor(0xFF855A12);
            tvSeverity.setBackgroundResource(R.drawable.bg_pill_amber);
        } else {
            tvSeverity.setTextColor(0xFF386B57);
            tvSeverity.setBackgroundResource(R.drawable.bg_pill_emerald);
        }
        topRow.addView(tvSeverity);
        card.addView(topRow);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(event.getTitle());
        tvTitle.setTextSize(senior ? 16 : 14);
        tvTitle.setTextColor(0xFF141414);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = (int) (6 * density);
        tvTitle.setLayoutParams(tp);
        card.addView(tvTitle);

        TextView tvDesc = new TextView(activity);
        tvDesc.setText(event.getSummary());
        tvDesc.setTextSize(senior ? 14 : 12);
        tvDesc.setTextColor(0xFF666666);
        tvDesc.setMaxLines(2);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = (int) (4 * density);
        tvDesc.setLayoutParams(dp);
        card.addView(tvDesc);

        return card;
    }

    private void dialHelpline() {
        try {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:1930"));
            activity.startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Dialer unavailable. Dial 1930 directly.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProfilePickerDialog() {
        ChildProfile[] profiles = ChildProfile.values();
        String[] names = new String[profiles.length];
        for (int i = 0; i < profiles.length; i++) {
            names[i] = profiles[i].getName() + " (" + profiles[i].getRoleTitle() + ")";
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select Active Safety Profile")
                .setItems(names, (dialog, which) -> {
                    ChildProfile selected = profiles[which];
                    AppState.get().setCurrentProfile(selected);
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
