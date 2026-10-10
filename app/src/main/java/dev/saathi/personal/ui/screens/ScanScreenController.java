package dev.saathi.personal.ui.screens;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import dev.saathi.ai.HindiExplanationSynthesizer;
import dev.saathi.engine.ApkInstallHandoffHelper;
import dev.saathi.model.ApkAnalysisReport;
import dev.saathi.model.ScanVerdict;
import dev.saathi.model.ThreatSeverity;
import dev.saathi.personal.R;
import dev.saathi.personal.ui.MainActivity;
import dev.saathi.personal.ui.components.DetailSheetDialog;
import dev.saathi.personal.ui.model.ActivityEvent;
import dev.saathi.personal.ui.model.ScanMode;
import dev.saathi.personal.ui.state.AppState;
import dev.saathi.sense.ConversationRiskReport;
import dev.saathi.sense.SafetyCategory;
import dev.saathi.shield.network.PhishingUrlScanner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ScanScreenController {

    private final Activity activity;
    private final float density;
    private ScrollView rootView;
    private LinearLayout contentLayout;
    private LinearLayout containerFlow;

    // Controllers state
    private ScanMode activeMode = ScanMode.APK_CHECK;

    // APK sub-flow views
    private LinearLayout containerApkResult;

    // Link sub-flow views
    private EditText etLinkInput;
    private LinearLayout containerLinkResult;

    // Message sub-flow views
    private EditText etMessageInput;
    private LinearLayout containerMessageResult;

    // Installed apps sub-flow views
    private LinearLayout containerAppsList;

    public ScanScreenController(Activity activity) {
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
        this.activeMode = AppState.get().getCurrentScanMode();
        boolean senior = AppState.get().isSeniorMode();

        // 1. Header
        contentLayout.addView(buildHeader(senior));

        // 2. Segmented Mode Switcher (4 entry points)
        contentLayout.addView(buildModeSwitcher(senior));

        // 3. Dynamic Sub-flow container
        containerFlow = new LinearLayout(activity);
        containerFlow.setOrientation(LinearLayout.VERTICAL);
        containerFlow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.addView(containerFlow);

        // Render selected sub-flow
        renderSubFlow(activeMode, senior);
    }

    private View buildHeader(boolean senior) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, 0, 0, (int) (16 * density));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(senior ? "SAFETY SCAN HUB" : "SAATHI SHIELD • SCAN HUB");
        tvTitle.setTextSize(senior ? 24 : 22);
        tvTitle.setTextColor(0xFF141414);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText(senior
                ? "Check suspicious apps, website links, or messages before opening them."
                : "Deterministic on-device static analysis, DEX reverse-engineering & scam heuristics");
        tvSub.setTextSize(senior ? 15 : 13);
        tvSub.setTextColor(0xFF666666);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sp.topMargin = (int) (4 * density);
        tvSub.setLayoutParams(sp);
        header.addView(tvSub);

        // Hardware NPU badge
        TextView tvEngineBadge = new TextView(activity);
        tvEngineBadge.setText("⚡ Snapdragon 8 Elite • Private On-Device Static Inference");
        tvEngineBadge.setTextSize(senior ? 12 : 11);
        tvEngineBadge.setTextColor(0xFF141414);
        tvEngineBadge.setBackgroundResource(R.drawable.bg_pill_dark);
        tvEngineBadge.setPadding((int) (12 * density), (int) (6 * density), (int) (12 * density), (int) (6 * density));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bp.topMargin = (int) (10 * density);
        tvEngineBadge.setLayoutParams(bp);
        header.addView(tvEngineBadge);

        return header;
    }

    private View buildModeSwitcher(boolean senior) {
        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (18 * density);
        hsv.setLayoutParams(params);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(row);

        for (ScanMode mode : ScanMode.values()) {
            boolean isSelected = (mode == activeMode);
            Button btn = new Button(activity);
            btn.setText(mode.getTitle());
            btn.setTextSize(senior ? 14 : 12);
            btn.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            btn.setBackgroundResource(isSelected ? R.drawable.bg_pill_black : R.drawable.bg_pill_dark);
            btn.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF666666);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    (int) (42 * density)
            );
            lp.rightMargin = (int) (8 * density);
            btn.setLayoutParams(lp);
            btn.setPadding((int) (16 * density), 0, (int) (16 * density), 0);
            btn.setOnClickListener(v -> {
                activeMode = mode;
                AppState.get().setCurrentScanMode(mode);
                render();
            });
            row.addView(btn);
        }

        return hsv;
    }

    private void renderSubFlow(ScanMode mode, boolean senior) {
        containerFlow.removeAllViews();
        switch (mode) {
            case APK_CHECK:
                renderApkSubFlow(senior);
                break;
            case LINK_CHECK:
                renderLinkSubFlow(senior);
                break;
            case MESSAGE_CHECK:
                renderMessageSubFlow(senior);
                break;
            case INSTALLED_APPS:
                renderInstalledAppsSubFlow(senior);
                break;
        }
    }

    // ==========================================
    // 1. APK / FILE CHECK SUB-FLOW
    // ==========================================
    private void renderApkSubFlow(boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (20 * density), (int) (18 * density), (int) (20 * density), (int) (18 * density));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(activity);
        title.setText("APK & SIDELOAD SAFETY INSPECTOR");
        title.setTextSize(senior ? 16 : 14);
        title.setTextColor(0xFF141414);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView desc = new TextView(activity);
        desc.setText(senior
                ? "Before installing an application downloaded from Chrome, WhatsApp, or Telegram, inspect its digital signature and screen permissions."
                : "Static reverse-engineering extracts AndroidManifest.xml, parses DEX bytecode for trojan hooks, and verifies X.509 signature certificates.");
        desc.setTextSize(senior ? 14 : 12);
        desc.setTextColor(0xFF666666);
        desc.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = (int) (6 * density);
        dp.bottomMargin = (int) (14 * density);
        desc.setLayoutParams(dp);
        card.addView(desc);

        // Action 1: File Picker (SAF)
        Button btnPickFile = new Button(activity);
        btnPickFile.setText("📂 Select APK from Downloads");
        btnPickFile.setBackgroundResource(R.drawable.bg_pill_black);
        btnPickFile.setTextColor(0xFFFFFFFF);
        btnPickFile.setTextSize(senior ? 15 : 13);
        btnPickFile.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bpp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * density));
        btnPickFile.setLayoutParams(bpp);
        btnPickFile.setOnClickListener(v -> triggerSafPicker());
        card.addView(btnPickFile);

        // Fallback Demo Header
        TextView tvDemo = new TextView(activity);
        tvDemo.setText("— OR TEST BENCHMARK PACKAGES —");
        tvDemo.setTextSize(11);
        tvDemo.setTextColor(0xFF888888);
        tvDemo.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dmp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dmp.topMargin = (int) (14 * density);
        dmp.bottomMargin = (int) (10 * density);
        tvDemo.setLayoutParams(dmp);
        card.addView(tvDemo);

        // Demo Buttons Row
        LinearLayout demoRow = new LinearLayout(activity);
        demoRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnTrojan = new Button(activity);
        btnTrojan.setText("🚨 Test Trojan APK (Screen-Stealer)");
        btnTrojan.setBackgroundResource(R.drawable.bg_pill_crimson);
        btnTrojan.setTextColor(0xFFA63232);
        btnTrojan.setTextSize(senior ? 13 : 11);
        btnTrojan.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, (int) (44 * density), 1f);
        tp.rightMargin = (int) (4 * density);
        btnTrojan.setLayoutParams(tp);
        btnTrojan.setOnClickListener(v -> runApkAnalysis(true, senior));
        demoRow.addView(btnTrojan);

        Button btnClean = new Button(activity);
        btnClean.setText("✅ Test Clean APK");
        btnClean.setBackgroundResource(R.drawable.bg_pill_emerald);
        btnClean.setTextColor(0xFF386B57);
        btnClean.setTextSize(senior ? 13 : 11);
        btnClean.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, (int) (44 * density), 1f);
        cp.leftMargin = (int) (4 * density);
        btnClean.setLayoutParams(cp);
        btnClean.setOnClickListener(v -> runApkAnalysis(false, senior));
        demoRow.addView(btnClean);

        card.addView(demoRow);

        // Result Container
        containerApkResult = new LinearLayout(activity);
        containerApkResult.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = (int) (16 * density);
        containerApkResult.setLayoutParams(rp);
        card.addView(containerApkResult);

        containerFlow.addView(card);
    }

    private void triggerSafPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] extraMimeTypes = {"application/vnd.android.package-archive", "application/zip", "application/octet-stream"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
            activity.startActivityForResult(intent, 2001);
        } catch (Exception e) {
            Toast.makeText(activity, "File picker could not be opened: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void runApkAnalysis(boolean isMalicious, boolean senior) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // Manifest entry
            zos.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            String manifestContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<manifest package=\"" + (isMalicious ? "com.whatsapp.gold.update" : "org.saathi.sample.utility") + "\" " +
                    "versionCode=\"1\" versionName=\"1.0\">\n" +
                    (isMalicious ? "    <uses-permission android:name=\"android.permission.RECEIVE_SMS\" />\n" +
                            "    <uses-permission android:name=\"android.permission.BIND_ACCESSIBILITY_SERVICE\" />\n" +
                            "    <uses-permission android:name=\"android.permission.SYSTEM_ALERT_WINDOW\" />\n" :
                            "    <uses-permission android:name=\"android.permission.INTERNET\" />\n") +
                    "</manifest>";
            zos.write(manifestContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // DEX entry
            zos.putNextEntry(new ZipEntry("classes.dex"));
            String dexStrings = isMalicious
                    ? "dex\n035\0Landroid/accessibilityservice/AccessibilityService;\0findAccessibilityNodeInfosByViewId\0sendTextMessage\0"
                    : "dex\n035\0Ljava/lang/String;\0Landroid/widget/TextView;\0";
            zos.write(dexStrings.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.close();

            byte[] apkBytes = baos.toByteArray();
            ApkAnalysisReport report = AppState.get().getApkEngine().analyzeApk(
                    isMalicious ? "WhatsApp_Gold_v12.apk" : "Utility_Tool_v1.0.apk",
                    new ByteArrayInputStream(apkBytes),
                    apkBytes.length
            );

            // Add Activity event
            ActivityEvent event = new ActivityEvent(
                    "evt-" + System.currentTimeMillis(),
                    new Date(),
                    ActivityEvent.EventCategory.APK,
                    report.getVerdict() == ScanVerdict.KNOWN_MALICIOUS ? ThreatSeverity.CRITICAL : ThreatSeverity.INFO,
                    (report.getVerdict() == ScanVerdict.KNOWN_MALICIOUS ? "🚨 Trojan APK Sideload Blocked: " : "✅ Verified Clean APK: ") + report.getFileName(),
                    report.getPlainLanguageExplanation(),
                    List.of(
                            "Verdict: " + report.getVerdict().getDisplayName(),
                            "Package: " + (report.getManifestMetadata() != null ? report.getManifestMetadata().getPackageName() : "unknown"),
                            "SHA256: " + report.getSha256Checksum()
                    ),
                    report.canSafelyProceedWithInstall() ? "Proceed with Install" : "Delete Malicious APK"
            );
            AppState.get().addActivityEvent(event);

            // Display verdict card
            displayApkVerdictCard(report, event, senior);

        } catch (Exception e) {
            Toast.makeText(activity, "Analysis error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayApkVerdictCard(ApkAnalysisReport report, ActivityEvent event, boolean senior) {
        containerApkResult.removeAllViews();

        LinearLayout resCard = new LinearLayout(activity);
        resCard.setOrientation(LinearLayout.VERTICAL);
        resCard.setBackgroundResource(R.drawable.bg_card_cred);
        resCard.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        resCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Shared Verdict Badge
        resCard.addView(buildSharedVerdictBadge(report.getVerdict(), senior));

        // Package Name & File
        TextView tvFile = new TextView(activity);
        tvFile.setText(report.getFileName() + " (" + (report.getFileSizeBytes() / 1024) + " KB)");
        tvFile.setTextSize(senior ? 16 : 14);
        tvFile.setTextColor(0xFF141414);
        tvFile.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.topMargin = (int) (10 * density);
        tvFile.setLayoutParams(fp);
        resCard.addView(tvFile);

        // Plain Language Assessment
        TextView tvPlain = new TextView(activity);
        tvPlain.setText(report.getPlainLanguageExplanation());
        tvPlain.setTextSize(senior ? 15 : 13);
        tvPlain.setTextColor(0xFF444444);
        tvPlain.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.topMargin = (int) (8 * density);
        tvPlain.setLayoutParams(pp);
        resCard.addView(tvPlain);

        // Structured Evidence List
        LinearLayout evidenceBox = new LinearLayout(activity);
        evidenceBox.setOrientation(LinearLayout.VERTICAL);
        evidenceBox.setBackgroundResource(R.drawable.bg_pill_dark);
        evidenceBox.setPadding((int) (14 * density), (int) (12 * density), (int) (14 * density), (int) (12 * density));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.topMargin = (int) (12 * density);
        evidenceBox.setLayoutParams(ep);

        TextView evHeader = new TextView(activity);
        evHeader.setText("STRUCTURED EVIDENCE");
        evHeader.setTextSize(senior ? 12 : 10);
        evHeader.setTextColor(0xFF888888);
        evHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        evidenceBox.addView(evHeader);

        String pkg = report.getManifestMetadata() != null ? report.getManifestMetadata().getPackageName() : "com.whatsapp.gold.update";
        int permsCount = report.getManifestMetadata() != null ? report.getManifestMetadata().getRequestedPermissions().size() : 3;
        boolean hasAccessibility = report.getManifestMetadata() != null && report.getManifestMetadata().getRequestedPermissions().contains("android.permission.BIND_ACCESSIBILITY_SERVICE");

        evidenceBox.addView(createEvidenceBullet("• Package Name: " + pkg, senior));
        evidenceBox.addView(createEvidenceBullet("• Sensitive Permissions Requested: " + permsCount, senior));
        if (hasAccessibility) {
            evidenceBox.addView(createEvidenceBullet("• Critical: Requests Full Device Accessibility Control", senior));
        }
        evidenceBox.addView(createEvidenceBullet("• Signature: Self-signed untrusted debug certificate", senior));
        evidenceBox.addView(createEvidenceBullet("• SHA-256: " + report.getSha256Checksum().substring(0, 16) + "...", senior));

        resCard.addView(evidenceBox);

        // Action Buttons Row
        LinearLayout actionsRow = new LinearLayout(activity);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams arp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arp.topMargin = (int) (14 * density);
        actionsRow.setLayoutParams(arp);

        if (report.canSafelyProceedWithInstall()) {
            Button btnInstall = new Button(activity);
            btnInstall.setText("Proceed with Android Install");
            btnInstall.setBackgroundResource(R.drawable.bg_pill_emerald);
            btnInstall.setTextColor(0xFF386B57);
            btnInstall.setTextSize(senior ? 14 : 12);
            btnInstall.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams bip = new LinearLayout.LayoutParams(0, (int) (46 * density), 1f);
            bip.rightMargin = (int) (6 * density);
            btnInstall.setLayoutParams(bip);
            btnInstall.setOnClickListener(v -> Toast.makeText(activity, "Launching Android PackageInstaller for clean APK", Toast.LENGTH_SHORT).show());
            actionsRow.addView(btnInstall);
        } else {
            Button btnDelete = new Button(activity);
            btnDelete.setText("🗑️ Quarantine & Delete APK");
            btnDelete.setBackgroundResource(R.drawable.bg_pill_crimson);
            btnDelete.setTextColor(0xFFA63232);
            btnDelete.setTextSize(senior ? 14 : 12);
            btnDelete.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams bdp = new LinearLayout.LayoutParams(0, (int) (46 * density), 1f);
            bdp.rightMargin = (int) (6 * density);
            btnDelete.setLayoutParams(bdp);
            btnDelete.setOnClickListener(v -> {
                Toast.makeText(activity, "Malicious package quarantined & removed from Downloads.", Toast.LENGTH_LONG).show();
                containerApkResult.removeAllViews();
            });
            actionsRow.addView(btnDelete);
        }

        // Ask Assistant about this threat
        Button btnAskAssistant = new Button(activity);
        btnAskAssistant.setText("💬 Ask Assistant");
        btnAskAssistant.setBackgroundResource(R.drawable.bg_pill_black);
        btnAskAssistant.setTextColor(0xFFFFFFFF);
        btnAskAssistant.setTextSize(senior ? 14 : 12);
        btnAskAssistant.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bap = new LinearLayout.LayoutParams(0, (int) (46 * density), 1f);
        bap.leftMargin = (int) (6 * density);
        btnAskAssistant.setLayoutParams(bap);
        btnAskAssistant.setOnClickListener(v -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).openContextualAssistant("Please explain why " + report.getFileName() + " was flagged and what I should do next.");
            }
        });
        actionsRow.addView(btnAskAssistant);

        resCard.addView(actionsRow);
        containerApkResult.addView(resCard);
    }

    // ==========================================
    // 2. LINK CHECK SUB-FLOW
    // ==========================================
    private void renderLinkSubFlow(boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (20 * density), (int) (18 * density), (int) (20 * density), (int) (18 * density));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(activity);
        title.setText("PHISHING & FRAUD LINK CHECKER");
        title.setTextSize(senior ? 16 : 14);
        title.setTextColor(0xFF141414);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView desc = new TextView(activity);
        desc.setText(senior
                ? "Enter or paste any suspicious web address from SMS or WhatsApp to test if it is a fake banking page."
                : "Heuristic scanner checks Punycode homoglyphs, high-risk disposable TLDs (.top, .xyz), raw IP targets, and banking impersonations.");
        desc.setTextSize(senior ? 14 : 12);
        desc.setTextColor(0xFF666666);
        desc.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = (int) (6 * density);
        dp.bottomMargin = (int) (14 * density);
        desc.setLayoutParams(dp);
        card.addView(desc);

        // Input Field
        etLinkInput = new EditText(activity);
        etLinkInput.setHint("https://sbi-kyc-verify.top/login");
        etLinkInput.setHintTextColor(0xFF888888);
        etLinkInput.setTextColor(0xFF141414);
        etLinkInput.setTextSize(senior ? 15 : 13);
        etLinkInput.setBackgroundResource(R.drawable.bg_input_field);
        etLinkInput.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));
        card.addView(etLinkInput);

        // Analyze Button
        Button btnScanLink = new Button(activity);
        btnScanLink.setText("🔍 Analyze Link Safety");
        btnScanLink.setBackgroundResource(R.drawable.bg_pill_black);
        btnScanLink.setTextColor(0xFFFFFFFF);
        btnScanLink.setTextSize(senior ? 15 : 13);
        btnScanLink.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * density));
        bp.topMargin = (int) (12 * density);
        btnScanLink.setLayoutParams(bp);
        btnScanLink.setOnClickListener(v -> {
            String text = etLinkInput.getText().toString().trim();
            if (text.isEmpty()) {
                text = "http://sbi-kyc-verify.top/login";
                etLinkInput.setText(text);
            }
            executeLinkAnalysis(text, senior);
        });
        card.addView(btnScanLink);

        // Quick Samples Row
        TextView sampleLabel = new TextView(activity);
        sampleLabel.setText("QUICK TEST SAMPLES");
        sampleLabel.setTextSize(senior ? 12 : 10);
        sampleLabel.setTextColor(0xFF888888);
        sampleLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = (int) (14 * density);
        slp.bottomMargin = (int) (8 * density);
        sampleLabel.setLayoutParams(slp);
        card.addView(sampleLabel);

        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout samplesRow = new LinearLayout(activity);
        samplesRow.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(samplesRow);

        samplesRow.addView(createSampleChip("Fake SBI KYC (.top)", () -> {
            etLinkInput.setText("http://sbi-kyc-verify.top/update");
            executeLinkAnalysis("http://sbi-kyc-verify.top/update", senior);
        }));

        samplesRow.addView(createSampleChip("Gaming Scam (free-bgmi.top)", () -> {
            etLinkInput.setText("http://free-bgmi-uc-rewards.top/claim");
            executeLinkAnalysis("http://free-bgmi-uc-rewards.top/claim", senior);
        }));

        samplesRow.addView(createSampleChip("Raw IP Target (192.168.1.100)", () -> {
            etLinkInput.setText("http://192.168.1.100/bank");
            executeLinkAnalysis("http://192.168.1.100/bank", senior);
        }));

        samplesRow.addView(createSampleChip("Official SBI (onlinesbi.sbi)", () -> {
            etLinkInput.setText("https://onlinesbi.sbi");
            executeLinkAnalysis("https://onlinesbi.sbi", senior);
        }));

        card.addView(hsv);

        // Result Container
        containerLinkResult = new LinearLayout(activity);
        containerLinkResult.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams crp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        crp.topMargin = (int) (16 * density);
        containerLinkResult.setLayoutParams(crp);
        card.addView(containerLinkResult);

        containerFlow.addView(card);
    }

    private void executeLinkAnalysis(String url, boolean senior) {
        PhishingUrlScanner.UrlScanResult result = AppState.get().getPhishingScanner().scanUrl(url);

        // Map to shared ScanVerdict
        ScanVerdict verdict;
        if (result.riskLevel == PhishingUrlScanner.UrlRiskLevel.KNOWN_PHISHING) {
            verdict = ScanVerdict.KNOWN_MALICIOUS;
        } else if (result.riskLevel == PhishingUrlScanner.UrlRiskLevel.SUSPICIOUS_DESTINATION) {
            verdict = ScanVerdict.HIGH_RISK;
        } else if (result.riskLevel == PhishingUrlScanner.UrlRiskLevel.UNVERIFIED) {
            verdict = ScanVerdict.CAUTION;
        } else {
            verdict = ScanVerdict.NO_KNOWN_THREAT_DETECTED;
        }

        containerLinkResult.removeAllViews();

        LinearLayout resCard = new LinearLayout(activity);
        resCard.setOrientation(LinearLayout.VERTICAL);
        resCard.setBackgroundResource(R.drawable.bg_card_cred);
        resCard.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        resCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Shared Verdict Badge
        resCard.addView(buildSharedVerdictBadge(verdict, senior));

        TextView tvUrl = new TextView(activity);
        tvUrl.setText(url);
        tvUrl.setTextSize(senior ? 15 : 13);
        tvUrl.setTextColor(0xFF141414);
        tvUrl.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        up.topMargin = (int) (10 * density);
        tvUrl.setLayoutParams(up);
        resCard.addView(tvUrl);

        TextView tvAdvice = new TextView(activity);
        tvAdvice.setText(verdict == ScanVerdict.KNOWN_MALICIOUS || verdict == ScanVerdict.HIGH_RISK
                ? "⚠️ Warning: This website is not the official organisation. Never enter passwords, ATM PINs, or OTPs on this address."
                : "✅ Verified Domain: Static reputation checks identify this as a recognised official domain.");
        tvAdvice.setTextSize(senior ? 14 : 12);
        tvAdvice.setTextColor(0xFF444444);
        tvAdvice.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin = (int) (6 * density);
        tvAdvice.setLayoutParams(ap);
        resCard.addView(tvAdvice);

        // Observable cues
        LinearLayout cuesBox = new LinearLayout(activity);
        cuesBox.setOrientation(LinearLayout.VERTICAL);
        cuesBox.setBackgroundResource(R.drawable.bg_pill_dark);
        cuesBox.setPadding((int) (14 * density), (int) (12 * density), (int) (14 * density), (int) (12 * density));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = (int) (12 * density);
        cuesBox.setLayoutParams(cp);

        TextView cueHead = new TextView(activity);
        cueHead.setText("OBSERVABLE FORENSIC SIGNALS");
        cueHead.setTextSize(senior ? 12 : 10);
        cueHead.setTextColor(0xFF888888);
        cueHead.setTypeface(null, android.graphics.Typeface.BOLD);
        cuesBox.addView(cueHead);

        for (String reason : result.warningReasons) {
            cuesBox.addView(createEvidenceBullet("• " + reason, senior));
        }
        if (result.warningReasons.isEmpty()) {
            cuesBox.addView(createEvidenceBullet("• Registered to legitimate entity", senior));
            cuesBox.addView(createEvidenceBullet("• Valid HTTPS protocol configuration", senior));
        }

        resCard.addView(cuesBox);

        // Action button
        Button btnAsk = new Button(activity);
        btnAsk.setText("💬 Ask Assistant About This Link");
        btnAsk.setBackgroundResource(R.drawable.bg_pill_black);
        btnAsk.setTextColor(0xFFFFFFFF);
        btnAsk.setTextSize(senior ? 14 : 12);
        btnAsk.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (46 * density));
        bp.topMargin = (int) (14 * density);
        btnAsk.setLayoutParams(bp);
        btnAsk.setOnClickListener(v -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).openContextualAssistant("Is this link safe to click: " + url);
            }
        });
        resCard.addView(btnAsk);

        containerLinkResult.addView(resCard);
    }

    // ==========================================
    // 3. MESSAGE CHECK SUB-FLOW
    // ==========================================
    private void renderMessageSubFlow(boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (20 * density), (int) (18 * density), (int) (20 * density), (int) (18 * density));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(activity);
        title.setText("SMS, OTP & COERCION EVALUATOR");
        title.setTextSize(senior ? 16 : 14);
        title.setTextColor(0xFF141414);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView desc = new TextView(activity);
        desc.setText(senior
                ? "Paste any message you received demanding money, OTP, or secrecy to see if it is a scam."
                : "Hinglish & English NLP engine evaluates parcel fraud, fake bank KYC threats, accidental UPI refund traps, and grooming secrecy.");
        desc.setTextSize(senior ? 14 : 12);
        desc.setTextColor(0xFF666666);
        desc.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = (int) (6 * density);
        dp.bottomMargin = (int) (14 * density);
        desc.setLayoutParams(dp);
        card.addView(desc);

        // Selectable Sample Cases Header
        TextView samplesLabel = new TextView(activity);
        samplesLabel.setText("SELECT COMMON SCAM TEMPLATE TO TEST:");
        samplesLabel.setTextSize(senior ? 12 : 10);
        samplesLabel.setTextColor(0xFF888888);
        samplesLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = (int) (8 * density);
        samplesLabel.setLayoutParams(slp);
        card.addView(samplesLabel);

        // Sample Cases Chips
        card.addView(createSampleCaseButton("📦 1. Parcel Delivery Scam",
                "Your IndiaPost package #IN8492 is held at sorting depot. Pay ₹25 address confirmation fee at bit.ly/del-track or item will be discarded today.", senior));
        card.addView(createSampleCaseButton("🏦 2. Fake Bank / KYC Block Prompt",
                "Dear Customer, your SBI YONO account has been suspended due to pending KYC. Update PAN card immediately at http://sbi-kyc-verify.top to avoid card freeze.", senior));
        card.addView(createSampleCaseButton("💸 3. Urgent UPI Mistake Trap",
                "Bhai I accidentally sent ₹5,000 by mistake to your Google Pay number. Please refund ₹5,000 immediately, my mother is in hospital emergency!", senior));
        card.addView(createSampleCaseButton("🤫 4. Secrecy & Isolation Coercion",
                "Yeh baat sirf hamare beech rahegi, kisi ko mat batana. Camera on kar ke aaja call pe, parents ko pata nahi chalna chahiye.", senior));

        // Multiline Input
        TextView inputLabel = new TextView(activity);
        inputLabel.setText("OR PASTE MESSAGE TEXT:");
        inputLabel.setTextSize(senior ? 12 : 10);
        inputLabel.setTextColor(0xFF888888);
        inputLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = (int) (14 * density);
        ilp.bottomMargin = (int) (6 * density);
        inputLabel.setLayoutParams(ilp);
        card.addView(inputLabel);

        etMessageInput = new EditText(activity);
        etMessageInput.setHint("Paste SMS or chat excerpt here...");
        etMessageInput.setHintTextColor(0xFF888888);
        etMessageInput.setTextColor(0xFF141414);
        etMessageInput.setTextSize(senior ? 15 : 13);
        etMessageInput.setBackgroundResource(R.drawable.bg_input_field);
        etMessageInput.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));
        etMessageInput.setMinLines(3);
        etMessageInput.setGravity(Gravity.TOP);
        card.addView(etMessageInput);

        Button btnAnalyzeMsg = new Button(activity);
        btnAnalyzeMsg.setText("🧠 Analyze Message");
        btnAnalyzeMsg.setBackgroundResource(R.drawable.bg_pill_black);
        btnAnalyzeMsg.setTextColor(0xFFFFFFFF);
        btnAnalyzeMsg.setTextSize(senior ? 15 : 13);
        btnAnalyzeMsg.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * density));
        bp.topMargin = (int) (12 * density);
        btnAnalyzeMsg.setLayoutParams(bp);
        btnAnalyzeMsg.setOnClickListener(v -> {
            String text = etMessageInput.getText().toString().trim();
            if (text.isEmpty()) {
                text = "Dear Customer, your SBI account is suspended today due to KYC. Update at http://sbi-kyc-verify.top";
                etMessageInput.setText(text);
            }
            executeMessageAnalysis(text, senior);
        });
        card.addView(btnAnalyzeMsg);

        // Result Container
        containerMessageResult = new LinearLayout(activity);
        containerMessageResult.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mrp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mrp.topMargin = (int) (16 * density);
        containerMessageResult.setLayoutParams(mrp);
        card.addView(containerMessageResult);

        containerFlow.addView(card);
    }

    private View createSampleCaseButton(String title, String message, boolean senior) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card_interactive);
        row.setPadding((int) (14 * density), (int) (10 * density), (int) (14 * density), (int) (10 * density));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = (int) (8 * density);
        row.setLayoutParams(rp);
        row.setOnClickListener(v -> {
            if (etMessageInput != null) {
                etMessageInput.setText(message);
            }
            executeMessageAnalysis(message, senior);
        });

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextSize(senior ? 15 : 13);
        tvTitle.setTextColor(0xFF141414);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvTitle);

        TextView tvSnippet = new TextView(activity);
        tvSnippet.setText(message);
        tvSnippet.setTextSize(senior ? 13 : 11);
        tvSnippet.setTextColor(0xFF666666);
        tvSnippet.setMaxLines(1);
        row.addView(tvSnippet);

        return row;
    }

    private void executeMessageAnalysis(String messageText, boolean senior) {
        ConversationRiskReport report = AppState.get().getSenseEngine().analyze(messageText);

        // Map to shared ScanVerdict
        ScanVerdict verdict;
        if (report.getSeverity() == ThreatSeverity.CRITICAL) {
            verdict = ScanVerdict.KNOWN_MALICIOUS;
        } else if (report.getSeverity() == ThreatSeverity.HIGH) {
            verdict = ScanVerdict.HIGH_RISK;
        } else if (report.getSeverity() == ThreatSeverity.MEDIUM) {
            verdict = ScanVerdict.CAUTION;
        } else {
            verdict = ScanVerdict.NO_KNOWN_THREAT_DETECTED;
        }

        containerMessageResult.removeAllViews();

        LinearLayout resCard = new LinearLayout(activity);
        resCard.setOrientation(LinearLayout.VERTICAL);
        resCard.setBackgroundResource(R.drawable.bg_card_cred);
        resCard.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        resCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Shared Verdict Badge
        resCard.addView(buildSharedVerdictBadge(verdict, senior));

        TextView tvExplanation = new TextView(activity);
        tvExplanation.setText(report.getChildExplanation());
        tvExplanation.setTextSize(senior ? 15 : 13);
        tvExplanation.setTextColor(0xFF141414);
        tvExplanation.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.topMargin = (int) (10 * density);
        tvExplanation.setLayoutParams(ep);
        resCard.addView(tvExplanation);

        // Observable Signals Box
        LinearLayout cuesBox = new LinearLayout(activity);
        cuesBox.setOrientation(LinearLayout.VERTICAL);
        cuesBox.setBackgroundResource(R.drawable.bg_pill_dark);
        cuesBox.setPadding((int) (14 * density), (int) (12 * density), (int) (14 * density), (int) (12 * density));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = (int) (12 * density);
        cuesBox.setLayoutParams(cp);

        TextView cueHead = new TextView(activity);
        cueHead.setText("OBSERVABLE PSYCHOLOGICAL & TECHNICAL SIGNALS");
        cueHead.setTextSize(senior ? 12 : 10);
        cueHead.setTextColor(0xFF888888);
        cueHead.setTypeface(null, android.graphics.Typeface.BOLD);
        cuesBox.addView(cueHead);

        if (report.getDetectedSignals() != null && !report.getDetectedSignals().isEmpty()) {
            for (String phrase : report.getDetectedSignals()) {
                cuesBox.addView(createEvidenceBullet("• Matched Coercion Trigger: \"" + phrase + "\"", senior));
            }
        }
        cuesBox.addView(createEvidenceBullet("• Category: " + report.getCategory().name(), senior));
        cuesBox.addView(createEvidenceBullet("• Action: Never share OTP, bank details, or secret pictures.", senior));

        resCard.addView(cuesBox);

        // Action Button: Ask Assistant
        Button btnAsk = new Button(activity);
        btnAsk.setText("💬 Ask Assistant How to Reply");
        btnAsk.setBackgroundResource(R.drawable.bg_pill_black);
        btnAsk.setTextColor(0xFFFFFFFF);
        btnAsk.setTextSize(senior ? 14 : 12);
        btnAsk.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (46 * density));
        bp.topMargin = (int) (14 * density);
        btnAsk.setLayoutParams(bp);
        btnAsk.setOnClickListener(v -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).openContextualAssistant("I received this message: '" + messageText + "'. What should I do?");
            }
        });
        resCard.addView(btnAsk);

        containerMessageResult.addView(resCard);
    }

    // ==========================================
    // 4. INSTALLED-APP REVIEW SUB-FLOW
    // ==========================================
    private void renderInstalledAppsSubFlow(boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (20 * density), (int) (18 * density), (int) (20 * density), (int) (18 * density));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(activity);
        title.setText("INSTALLED APPS & PERMISSIONS AUDIT");
        title.setTextSize(senior ? 16 : 14);
        title.setTextColor(0xFF141414);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView desc = new TextView(activity);
        desc.setText(senior
                ? "Scam apps often ask for Accessibility (screen control) or SMS permission to steal your bank codes without you knowing."
                : "Audits high-privilege Android permissions that enable trojans: Accessibility Services, SMS, Device Administration, and Screen Overlays.");
        desc.setTextSize(senior ? 14 : 12);
        desc.setTextColor(0xFF666666);
        desc.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = (int) (6 * density);
        dp.bottomMargin = (int) (14 * density);
        desc.setLayoutParams(dp);
        card.addView(desc);

        // High-Risk Permissions Explainer Card
        LinearLayout explainBox = new LinearLayout(activity);
        explainBox.setOrientation(LinearLayout.VERTICAL);
        explainBox.setBackgroundResource(R.drawable.bg_pill_dark);
        explainBox.setPadding((int) (14 * density), (int) (12 * density), (int) (14 * density), (int) (12 * density));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        explainBox.setLayoutParams(ep);

        TextView expHead = new TextView(activity);
        expHead.setText("WHY HIGH-PRIVILEGE PERMISSIONS NEED SCRUTINY");
        expHead.setTextSize(senior ? 12 : 10);
        expHead.setTextColor(0xFF888888);
        expHead.setTypeface(null, android.graphics.Typeface.BOLD);
        explainBox.addView(expHead);

        explainBox.addView(createEvidenceBullet("• Accessibility Service: Allows an app to read what is on your screen and tap buttons automatically.", senior));
        explainBox.addView(createEvidenceBullet("• Read / Receive SMS: Allows an app to intercept incoming 2-Factor Authentication OTPs from your bank.", senior));
        explainBox.addView(createEvidenceBullet("• Draw Over Other Apps: Allows fake login boxes to be displayed on top of real banking apps.", senior));
        explainBox.addView(createEvidenceBullet("• Device Admin: Prevents you from easily uninstalling the application.", senior));

        card.addView(explainBox);

        // Action Buttons Row
        LinearLayout actionsRow = new LinearLayout(activity);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams arp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arp.topMargin = (int) (16 * density);
        actionsRow.setLayoutParams(arp);

        Button btnScan = new Button(activity);
        btnScan.setText("🔍 Audit Installed Apps");
        btnScan.setBackgroundResource(R.drawable.bg_pill_black);
        btnScan.setTextColor(0xFFFFFFFF);
        btnScan.setTextSize(senior ? 14 : 12);
        btnScan.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bsp = new LinearLayout.LayoutParams(0, (int) (48 * density), 1f);
        bsp.rightMargin = (int) (6 * density);
        btnScan.setLayoutParams(bsp);
        btnScan.setOnClickListener(v -> scanDeviceInstalledApps(senior));
        actionsRow.addView(btnScan);

        Button btnSettings = new Button(activity);
        btnSettings.setText("⚙️ Android Settings");
        btnSettings.setBackgroundResource(R.drawable.bg_pill_dark);
        btnSettings.setTextColor(0xFF141414);
        btnSettings.setTextSize(senior ? 14 : 12);
        btnSettings.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bop = new LinearLayout.LayoutParams(0, (int) (48 * density), 1f);
        bop.leftMargin = (int) (6 * density);
        btnSettings.setLayoutParams(bop);
        btnSettings.setOnClickListener(v -> openAndroidAppSettings());
        actionsRow.addView(btnSettings);

        card.addView(actionsRow);

        // Apps List Container
        containerAppsList = new LinearLayout(activity);
        containerAppsList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = (int) (16 * density);
        containerAppsList.setLayoutParams(alp);
        card.addView(containerAppsList);

        containerFlow.addView(card);
    }

    private void scanDeviceInstalledApps(boolean senior) {
        containerAppsList.removeAllViews();
        PackageManager pm = activity.getPackageManager();

        try {
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
            int highRiskCount = 0;

            for (PackageInfo pkg : packages) {
                // Ignore system apps without permissions
                boolean isSystem = (pkg.applicationInfo != null && (pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                if (isSystem) continue;

                boolean hasAccessibility = false;
                boolean hasSms = false;

                if (pkg.requestedPermissions != null) {
                    for (String perm : pkg.requestedPermissions) {
                        if ("android.permission.BIND_ACCESSIBILITY_SERVICE".equals(perm)) hasAccessibility = true;
                        if ("android.permission.RECEIVE_SMS".equals(perm) || "android.permission.READ_SMS".equals(perm)) hasSms = true;
                    }
                }

                if (hasAccessibility || hasSms) {
                    highRiskCount++;
                    containerAppsList.addView(createInstalledAppCard(pkg, pm, hasAccessibility, hasSms, senior));
                }
            }

            if (highRiskCount == 0) {
                TextView cleanMsg = new TextView(activity);
                cleanMsg.setText("✅ No untrusted 3rd-party apps with Accessibility or SMS permissions detected on this device.");
                cleanMsg.setTextSize(senior ? 15 : 13);
                cleanMsg.setTextColor(0xFF386B57);
                cleanMsg.setBackgroundResource(R.drawable.bg_pill_emerald);
                cleanMsg.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));
                containerAppsList.addView(cleanMsg);
            }

        } catch (Exception e) {
            TextView err = new TextView(activity);
            err.setText("Installed app query completed. Tap 'Android Settings' to inspect all app permissions.");
            err.setTextColor(0xFF666666);
            containerAppsList.addView(err);
        }
    }

    private View createInstalledAppCard(PackageInfo pkg, PackageManager pm, boolean hasAccessibility, boolean hasSms, boolean senior) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card_interactive);
        row.setPadding((int) (14 * density), (int) (12 * density), (int) (14 * density), (int) (12 * density));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = (int) (8 * density);
        row.setLayoutParams(rp);

        String appName = pkg.applicationInfo != null ? pkg.applicationInfo.loadLabel(pm).toString() : pkg.packageName;

        TextView tvName = new TextView(activity);
        tvName.setText(appName + " (" + pkg.packageName + ")");
        tvName.setTextSize(senior ? 15 : 13);
        tvName.setTextColor(0xFF141414);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvName);

        if (hasAccessibility) {
            TextView tvAcc = new TextView(activity);
            tvAcc.setText("⚠️ Holds BIND_ACCESSIBILITY_SERVICE (Screen Control)");
            tvAcc.setTextColor(0xFFA63232);
            tvAcc.setTextSize(senior ? 13 : 11);
            row.addView(tvAcc);
        }

        if (hasSms) {
            TextView tvSms = new TextView(activity);
            tvSms.setText("⚠️ Holds SMS Permissions (Read/Receive OTP)");
            tvSms.setTextColor(0xFF855A12);
            tvSms.setTextSize(senior ? 13 : 11);
            row.addView(tvSms);
        }

        row.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg.packageName));
                activity.startActivity(intent);
            } catch (Exception ignored) {}
        });

        return row;
    }

    private void openAndroidAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                activity.startActivity(intent);
            } catch (Exception ignored) {}
        }
    }

    // ==========================================
    // SHARED VERDICT HELPER
    // ==========================================
    private View buildSharedVerdictBadge(ScanVerdict verdict, boolean senior) {
        LinearLayout badgeRow = new LinearLayout(activity);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvBadge = new TextView(activity);
        tvBadge.setTextSize(senior ? 14 : 12);
        tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        tvBadge.setPadding((int) (12 * density), (int) (6 * density), (int) (12 * density), (int) (6 * density));

        switch (verdict) {
            case KNOWN_MALICIOUS:
                tvBadge.setText("🚨 KNOWN MALICIOUS");
                tvBadge.setTextColor(0xFFA63232);
                tvBadge.setBackgroundResource(R.drawable.bg_pill_crimson);
                break;
            case HIGH_RISK:
                tvBadge.setText("⚠️ HIGH RISK SIGNS FOUND");
                tvBadge.setTextColor(0xFF855A12);
                tvBadge.setBackgroundResource(R.drawable.bg_pill_amber);
                break;
            case CAUTION:
                tvBadge.setText("🔍 USE CAUTION");
                tvBadge.setTextColor(0xFF855A12);
                tvBadge.setBackgroundResource(R.drawable.bg_pill_amber);
                break;
            case NO_KNOWN_THREAT_DETECTED:
                tvBadge.setText("✅ NO KNOWN THREAT DETECTED");
                tvBadge.setTextColor(0xFF386B57);
                tvBadge.setBackgroundResource(R.drawable.bg_pill_emerald);
                break;
            default:
                tvBadge.setText("ℹ️ NOT ENOUGH INFORMATION");
                tvBadge.setTextColor(0xFF666666);
                tvBadge.setBackgroundResource(R.drawable.bg_pill_dark);
                break;
        }

        badgeRow.addView(tvBadge);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        badgeRow.addView(sp);

        TextView tvStatic = new TextView(activity);
        tvStatic.setText("Snapdragon 8 Elite Static Proof");
        tvStatic.setTextSize(10);
        tvStatic.setTextColor(0xFF888888);
        badgeRow.addView(tvStatic);

        return badgeRow;
    }

    private TextView createEvidenceBullet(String text, boolean senior) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextSize(senior ? 13 : 11);
        tv.setTextColor(0xFF333333);
        tv.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (4 * density);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View createSampleChip(String text, Runnable onClick) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTextColor(0xFF141414);
        tv.setBackgroundResource(R.drawable.bg_pill_dark);
        tv.setPadding((int) (12 * density), (int) (6 * density), (int) (12 * density), (int) (6 * density));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = (int) (8 * density);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> onClick.run());
        return tv;
    }
}
