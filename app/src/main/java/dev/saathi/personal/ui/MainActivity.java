package dev.saathi.personal.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import dev.saathi.model.ApkAnalysisReport;
import dev.saathi.model.ScanVerdict;
import dev.saathi.model.ThreatSeverity;
import dev.saathi.personal.R;
import dev.saathi.personal.ui.components.DetailSheetDialog;
import dev.saathi.personal.ui.model.ActivityEvent;
import dev.saathi.personal.ui.model.NavigationTab;
import dev.saathi.personal.ui.screens.ActivityScreenController;
import dev.saathi.personal.ui.screens.AssistantScreenController;
import dev.saathi.personal.ui.screens.FamilyScreenController;
import dev.saathi.personal.ui.screens.HomeScreenController;
import dev.saathi.personal.ui.screens.ScanScreenController;
import dev.saathi.personal.ui.screens.SettingsScreenController;
import dev.saathi.personal.ui.state.AppState;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;

public class MainActivity extends Activity {

    private FrameLayout screenContainer;
    private FrameLayout modalContainer;
    private FrameLayout onboardingContainer;

    private LinearLayout navItemHome;
    private LinearLayout navItemScan;
    private LinearLayout navItemActivity;
    private LinearLayout navItemFamily;
    private LinearLayout navItemSettings;

    private ImageView ivNavHome;
    private ImageView ivNavScan;
    private ImageView ivNavActivity;
    private ImageView ivNavFamily;
    private ImageView ivNavSettings;

    private TextView tvNavHome;
    private TextView tvNavScan;
    private TextView tvNavActivity;
    private TextView tvNavFamily;
    private TextView tvNavSettings;

    private HomeScreenController homeController;
    private ScanScreenController scanController;
    private ActivityScreenController activityController;
    private FamilyScreenController familyController;
    private SettingsScreenController settingsController;
    private AssistantScreenController assistantController;

    private final Deque<NavigationTab> navigationBackStack = new ArrayDeque<>();
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hardware Optimization: 144Hz LTPO AMOLED sync for iQOO 15 (Snapdragon 8 Elite)
        optimizeDisplayForIqoo15();

        // Enforce Light Status Bar and Light Navigation Bar (Dark Icons)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
            getWindow().setStatusBarColor(0xFFF7F7F5);
            getWindow().setNavigationBarColor(0xFFFFFFFF);
        }

        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize persistent preferences in AppState
        AppState.get().initContext(this);

        initViews();
        setupScreenControllers();
        setupNavigation();

        // Listen for centralized state changes
        AppState.get().addListener(this::onAppStateChanged);

        // Check incoming intent (APK shared from Chrome, WhatsApp, Files)
        handleIncomingIntent(getIntent());

        // Initial tab
        switchTab(NavigationTab.HOME, false);
    }

    private void optimizeDisplayForIqoo15() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                WindowManager.LayoutParams params = getWindow().getAttributes();
                Display display = getDisplay();
                if (display != null) {
                    Display.Mode[] modes = display.getSupportedModes();
                    for (Display.Mode mode : modes) {
                        if (Math.round(mode.getRefreshRate()) == 144) {
                            params.preferredDisplayModeId = mode.getModeId();
                            break;
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                }
                getWindow().setAttributes(params);
            } catch (Exception ignored) {}
        }
    }

    private void initViews() {
        screenContainer = findViewById(R.id.screen_container);
        modalContainer = findViewById(R.id.modal_container);
        onboardingContainer = findViewById(R.id.onboarding_container);

        navItemHome = findViewById(R.id.nav_item_home);
        navItemScan = findViewById(R.id.nav_item_scan);
        navItemActivity = findViewById(R.id.nav_item_activity);
        navItemFamily = findViewById(R.id.nav_item_family);
        navItemSettings = findViewById(R.id.nav_item_settings);

        ivNavHome = findViewById(R.id.iv_nav_home);
        ivNavScan = findViewById(R.id.iv_nav_scan);
        ivNavActivity = findViewById(R.id.iv_nav_activity);
        ivNavFamily = findViewById(R.id.iv_nav_family);
        ivNavSettings = findViewById(R.id.iv_nav_settings);

        tvNavHome = findViewById(R.id.tv_nav_home);
        tvNavScan = findViewById(R.id.tv_nav_scan);
        tvNavActivity = findViewById(R.id.tv_nav_activity);
        tvNavFamily = findViewById(R.id.tv_nav_family);
        tvNavSettings = findViewById(R.id.tv_nav_settings);
    }

    private void setupScreenControllers() {
        homeController = new HomeScreenController(this);
        scanController = new ScanScreenController(this);
        activityController = new ActivityScreenController(this);
        familyController = new FamilyScreenController(this);
        settingsController = new SettingsScreenController(this);
        assistantController = new AssistantScreenController(this);

        // Add 5 main screens to screenContainer
        screenContainer.addView(homeController.getView());
        screenContainer.addView(scanController.getView());
        screenContainer.addView(activityController.getView());
        screenContainer.addView(familyController.getView());
        screenContainer.addView(settingsController.getView());

        // Contextual assistant container setup
        assistantController.setCloseCallback(() -> {
            if (modalContainer != null) {
                modalContainer.setVisibility(View.GONE);
                modalContainer.removeAllViews();
            }
        });
    }

    private void setupNavigation() {
        navItemHome.setOnClickListener(v -> switchTab(NavigationTab.HOME, true));
        navItemScan.setOnClickListener(v -> switchTab(NavigationTab.SCAN, true));
        navItemActivity.setOnClickListener(v -> switchTab(NavigationTab.ACTIVITY, true));
        navItemFamily.setOnClickListener(v -> switchTab(NavigationTab.FAMILY, true));
        navItemSettings.setOnClickListener(v -> switchTab(NavigationTab.SETTINGS, true));
    }

    public void switchTab(NavigationTab targetTab, boolean addToStack) {
        // Dismiss modal if open
        if (modalContainer != null && modalContainer.getVisibility() == View.VISIBLE) {
            modalContainer.setVisibility(View.GONE);
            modalContainer.removeAllViews();
        }

        NavigationTab currentTab = AppState.get().getCurrentTab();
        if (addToStack && currentTab != targetTab) {
            navigationBackStack.push(currentTab);
        }

        AppState.get().setCurrentTab(targetTab);
        performHaptic();

        // Update Visibility of Screens
        homeController.getView().setVisibility(targetTab == NavigationTab.HOME ? View.VISIBLE : View.GONE);
        scanController.getView().setVisibility(targetTab == NavigationTab.SCAN ? View.VISIBLE : View.GONE);
        activityController.getView().setVisibility(targetTab == NavigationTab.ACTIVITY ? View.VISIBLE : View.GONE);
        familyController.getView().setVisibility(targetTab == NavigationTab.FAMILY ? View.VISIBLE : View.GONE);
        settingsController.getView().setVisibility(targetTab == NavigationTab.SETTINGS ? View.VISIBLE : View.GONE);

        // Refresh views
        if (targetTab == NavigationTab.HOME) homeController.render();
        else if (targetTab == NavigationTab.SCAN) scanController.render();
        else if (targetTab == NavigationTab.ACTIVITY) activityController.render();
        else if (targetTab == NavigationTab.FAMILY) familyController.render();
        else if (targetTab == NavigationTab.SETTINGS) settingsController.render();

        updateNavHighlight(targetTab);
    }

    public void openContextualAssistant(String prompt) {
        if (modalContainer != null) {
            modalContainer.removeAllViews();
            modalContainer.addView(assistantController.getView());
            modalContainer.setVisibility(View.VISIBLE);
            assistantController.render();
            if (prompt != null && !prompt.isBlank()) {
                assistantController.promptAssistant(prompt);
            }
        }
    }

    private void updateNavHighlight(NavigationTab tab) {
        int activeColor = 0xFF141414; // Solid Monochrome Black
        int inactiveColor = 0xFF888888; // Muted Neutral Grey

        ivNavHome.setColorFilter(tab == NavigationTab.HOME ? activeColor : inactiveColor);
        tvNavHome.setTextColor(tab == NavigationTab.HOME ? activeColor : inactiveColor);
        tvNavHome.setTypeface(null, tab == NavigationTab.HOME ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        ivNavScan.setColorFilter(tab == NavigationTab.SCAN ? activeColor : inactiveColor);
        tvNavScan.setTextColor(tab == NavigationTab.SCAN ? activeColor : inactiveColor);
        tvNavScan.setTypeface(null, tab == NavigationTab.SCAN ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        ivNavActivity.setColorFilter(tab == NavigationTab.ACTIVITY ? activeColor : inactiveColor);
        tvNavActivity.setTextColor(tab == NavigationTab.ACTIVITY ? activeColor : inactiveColor);
        tvNavActivity.setTypeface(null, tab == NavigationTab.ACTIVITY ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        ivNavFamily.setColorFilter(tab == NavigationTab.FAMILY ? activeColor : inactiveColor);
        tvNavFamily.setTextColor(tab == NavigationTab.FAMILY ? activeColor : inactiveColor);
        tvNavFamily.setTypeface(null, tab == NavigationTab.FAMILY ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        ivNavSettings.setColorFilter(tab == NavigationTab.SETTINGS ? activeColor : inactiveColor);
        tvNavSettings.setTextColor(tab == NavigationTab.SETTINGS ? activeColor : inactiveColor);
        tvNavSettings.setTypeface(null, tab == NavigationTab.SETTINGS ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void performHaptic() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(12);
                }
            }
        } catch (Exception ignored) {}
    }

    private void onAppStateChanged() {
        NavigationTab tab = AppState.get().getCurrentTab();
        if (screenContainer != null) {
            homeController.getView().setVisibility(tab == NavigationTab.HOME ? View.VISIBLE : View.GONE);
            scanController.getView().setVisibility(tab == NavigationTab.SCAN ? View.VISIBLE : View.GONE);
            activityController.getView().setVisibility(tab == NavigationTab.ACTIVITY ? View.VISIBLE : View.GONE);
            familyController.getView().setVisibility(tab == NavigationTab.FAMILY ? View.VISIBLE : View.GONE);
            settingsController.getView().setVisibility(tab == NavigationTab.SETTINGS ? View.VISIBLE : View.GONE);
            updateNavHighlight(tab);
        }
    }

    @Override
    public void onBackPressed() {
        if (modalContainer != null && modalContainer.getVisibility() == View.VISIBLE) {
            modalContainer.setVisibility(View.GONE);
            modalContainer.removeAllViews();
            return;
        }

        if (!navigationBackStack.isEmpty()) {
            NavigationTab previous = navigationBackStack.pop();
            switchTab(previous, false);
        } else {
            super.onBackPressed();
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data);
                if (is != null) {
                    ApkAnalysisReport report = AppState.get().getApkEngine().analyzeApk("downloaded_package.apk", is, 0);

                    ActivityEvent event = new ActivityEvent(
                            "evt-" + System.currentTimeMillis(),
                            new Date(),
                            ActivityEvent.EventCategory.APK,
                            report.getVerdict() == ScanVerdict.KNOWN_MALICIOUS ? ThreatSeverity.CRITICAL : ThreatSeverity.INFO,
                            (report.getVerdict() == ScanVerdict.KNOWN_MALICIOUS ? "🚨 Intercepted Trojan APK: " : "✅ Clean APK Verified: ") + report.getFileName(),
                            report.getPlainLanguageExplanation(),
                            List.of("Verdict: " + report.getVerdict().getDisplayName(), "SHA256: " + report.getSha256Checksum()),
                            report.canSafelyProceedWithInstall() ? "Proceed with Install" : "Quarantine & Delete"
                    );

                    AppState.get().addActivityEvent(event);
                    DetailSheetDialog.showEventDetails(this, event);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Could not open APK: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
