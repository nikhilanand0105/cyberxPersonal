package dev.saathi.personal.ui.state;

import android.content.Context;
import android.content.SharedPreferences;

import dev.saathi.engine.SaathiApkIntelligence;
import dev.saathi.model.ThreatSeverity;
import dev.saathi.personal.ui.model.ActivityEvent;
import dev.saathi.personal.ui.model.ChatMessage;
import dev.saathi.personal.ui.model.ChildProfile;
import dev.saathi.personal.ui.model.FamilyDevice;
import dev.saathi.personal.ui.model.NavigationTab;
import dev.saathi.personal.ui.model.SafetyReport;
import dev.saathi.personal.ui.model.ScanMode;
import dev.saathi.sense.SaathiSenseEngine;
import dev.saathi.shield.network.LocalFirewallEngine;
import dev.saathi.shield.network.PhishingUrlScanner;
import dev.saathi.storage.LocalStorageRepository;
import dev.saathi.storage.TrustedHelper;

import java.util.*;

public class AppState {

    private static final String PREF_NAME = "saathi_personal_prefs";
    private static final String KEY_SENIOR_MODE = "pref_senior_mode";
    private static final String KEY_LARGE_TEXT = "pref_large_text";
    private static final String KEY_HIGH_CONTRAST = "pref_high_contrast";
    private static final String KEY_HINDI = "pref_hindi";

    private static AppState instance;

    public static synchronized AppState get() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    private SharedPreferences preferences;

    private final SaathiApkIntelligence apkEngine;
    private final SaathiSenseEngine senseEngine;
    private final PhishingUrlScanner phishingScanner;
    private final LocalFirewallEngine firewallEngine;
    private final LocalStorageRepository storageRepository;

    private ChildProfile currentProfile = ChildProfile.AARAV;
    private NavigationTab currentTab = NavigationTab.HOME;
    private ScanMode currentScanMode = ScanMode.APK_CHECK;

    private boolean isSeniorMode = false;
    private boolean isLargeText = false;
    private boolean isHighContrast = false;
    private boolean isHindi = false;
    private int safetyScore = 96;

    private String familyPairingCode = "SAATHI-FAM-7482";
    private boolean safetyAgreementAccepted = true;

    private final List<FamilyDevice> familyDevices = new ArrayList<>();
    private final List<SafetyReport> safetyReports = new ArrayList<>();
    private final List<ActivityEvent> activityEvents = new ArrayList<>();
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private final List<Runnable> stateListeners = new ArrayList<>();

    private AppState() {
        this.apkEngine = new SaathiApkIntelligence();
        this.senseEngine = new SaathiSenseEngine();
        this.phishingScanner = new PhishingUrlScanner();
        this.firewallEngine = new LocalFirewallEngine();
        this.storageRepository = new LocalStorageRepository();

        // Seed initial trusted helpers
        storageRepository.addTrustedHelper(new TrustedHelper("th-1", "Sunita (Mother)", "PRIMARY_PARENT", "+91 98101 23456", true));
        storageRepository.addTrustedHelper(new TrustedHelper("th-2", "Rajesh (Father)", "GUARDIAN", "+91 98202 34567", true));

        // Seed initial family devices (transparent, mutual safety)
        familyDevices.add(new FamilyDevice("dev-1", "Aarav's Phone (iQOO Z9)", "Aarav (Teen)", "Protected • Online", "12 mins ago", false));
        familyDevices.add(new FamilyDevice("dev-2", "Ananya's Tablet (Galaxy Tab)", "Ananya (Child)", "Protected • Online", "45 mins ago", false));
        familyDevices.add(new FamilyDevice("dev-3", "Dadaji's Phone (Redmi Note)", "Dadaji (Elderly)", "Senior Mode Active", "2 hours ago", true));

        // Seed child-initiated transparent safety reports
        safetyReports.add(new SafetyReport(
                "rep-201",
                "Aarav",
                new Date(System.currentTimeMillis() - 1000 * 60 * 45),
                "Suspicious Discord Contact",
                "User 'ModHelper_99' sent link claiming free Discord Nitro and asked Aarav to keep it secret.",
                "Reviewed with Parents",
                "Link neutralized & contact blocked"
        ));
        safetyReports.add(new SafetyReport(
                "rep-202",
                "Dadaji",
                new Date(System.currentTimeMillis() - 1000 * 60 * 180),
                "Electricity Bill SMS Inquiry",
                "Received SMS warning electricity will be disconnected tonight at 9 PM unless ₹12 fee paid via link.",
                "Verified Scam",
                "Marked as phishing, no payment made"
        ));

        // Seed initial activity events
        seedInitialActivityEvents();

        // Seed initial assistant greeting
        seedInitialChatMessages();
    }

    public void initContext(Context context) {
        if (context == null || preferences != null) return;
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        isSeniorMode = preferences.getBoolean(KEY_SENIOR_MODE, false);
        isLargeText = preferences.getBoolean(KEY_LARGE_TEXT, false);
        isHighContrast = preferences.getBoolean(KEY_HIGH_CONTRAST, false);
        isHindi = preferences.getBoolean(KEY_HINDI, false);
    }

    private void seedInitialActivityEvents() {
        activityEvents.add(new ActivityEvent(
                "evt-101",
                new Date(System.currentTimeMillis() - 1000 * 60 * 25), // 25 mins ago
                ActivityEvent.EventCategory.APK,
                ThreatSeverity.CRITICAL,
                "Malicious Sideload Blocked: GB_WhatsApp_v19.apk",
                "App requested BIND_ACCESSIBILITY_SERVICE and SMS reading. Neutralized before installation.",
                List.of(
                        "DEX Hook: findAccessibilityNodeInfosByViewId",
                        "Permissions: RECEIVE_SMS, SYSTEM_ALERT_WINDOW",
                        "Certificate: Untrusted Self-Signed Debug Key"
                ),
                "View Forensic Evidence"
        ));

        activityEvents.add(new ActivityEvent(
                "evt-102",
                new Date(System.currentTimeMillis() - 1000 * 60 * 85), // 85 mins ago
                ActivityEvent.EventCategory.SENSE,
                ThreatSeverity.HIGH,
                "Secrecy & Isolation Coercion Flagged in Chat",
                "Stranger messaged: 'Video call pe aa ja, kisi ko mat batana'. Sense flagged isolation cue.",
                List.of(
                        "Category: GROOMING_MANIPULATION",
                        "Matched Linguistic Patterns: 'kisi ko mat batana', 'camera on kar'",
                        "Recommendation: Advised child not to share images & informed family"
                ),
                "Review Conversation Excerpt"
        ));

        activityEvents.add(new ActivityEvent(
                "evt-103",
                new Date(System.currentTimeMillis() - 1000 * 60 * 240), // 4 hrs ago
                ActivityEvent.EventCategory.PHISHING,
                ThreatSeverity.HIGH,
                "Fake SBI KYC Update Link Neutralized",
                "Received SMS with link: http://sbi-kyc-verify.top. Credential harvesting phishing kit identified.",
                List.of(
                        "Domain: sbi-kyc-verify.top",
                        "High-Risk TLD: .top (Common disposable phishing host)",
                        "Lure: Banking KYC suspension impersonation"
                ),
                "Block & Report Link"
        ));

        activityEvents.add(new ActivityEvent(
                "evt-104",
                new Date(System.currentTimeMillis() - 1000 * 60 * 600), // 10 hrs ago
                ActivityEvent.EventCategory.FIREWALL,
                ThreatSeverity.INFO,
                "Local DNS Firewall Blocked 14 Ad/Tracker Requests",
                "Background telemetry to known user-tracking endpoints blocked without cloud relaying.",
                List.of(
                        "Destinations: telemetry-ads.tracking-network.com, analytics-leak.net",
                        "Zero battery drain: Snapdragon 8 Elite hardware-accelerated rule table"
                ),
                "View Firewall Log"
        ));
    }

    private void seedInitialChatMessages() {
        chatMessages.add(new ChatMessage(
                false,
                "Namaste! I am your Saathi Digital Safety Copilot, running locally on your Snapdragon 8 Elite.\n\nI can help explain suspicious messages, verify APK sideloads, guide you through banking scam recovery, and help you report fraud on the 1930 National Portal. How can I help you today?",
                "Saathi Safety Copilot"
        ));
    }

    public SaathiApkIntelligence getApkEngine() {
        return apkEngine;
    }

    public SaathiSenseEngine getSenseEngine() {
        return senseEngine;
    }

    public PhishingUrlScanner getPhishingScanner() {
        return phishingScanner;
    }

    public LocalFirewallEngine getFirewallEngine() {
        return firewallEngine;
    }

    public LocalStorageRepository getStorageRepository() {
        return storageRepository;
    }

    public ChildProfile getCurrentProfile() {
        return currentProfile;
    }

    public void setCurrentProfile(ChildProfile currentProfile) {
        this.currentProfile = currentProfile;
        this.safetyScore = currentProfile.getDefaultSafetyScore();
        if (currentProfile == ChildProfile.DADAJI) {
            setSeniorMode(true);
        } else {
            notifyListeners();
        }
    }

    public NavigationTab getCurrentTab() {
        return currentTab;
    }

    public void setCurrentTab(NavigationTab currentTab) {
        this.currentTab = currentTab;
        notifyListeners();
    }

    public ScanMode getCurrentScanMode() {
        return currentScanMode;
    }

    public void setCurrentScanMode(ScanMode currentScanMode) {
        this.currentScanMode = currentScanMode;
        notifyListeners();
    }

    public void navigateToScan(ScanMode scanMode) {
        this.currentScanMode = scanMode;
        this.currentTab = NavigationTab.SCAN;
        notifyListeners();
    }

    public boolean isSeniorMode() {
        return isSeniorMode;
    }

    public void setSeniorMode(boolean seniorMode) {
        this.isSeniorMode = seniorMode;
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_SENIOR_MODE, seniorMode).apply();
        }
        notifyListeners();
    }

    public boolean isLargeText() {
        return isLargeText || isSeniorMode;
    }

    public void setLargeText(boolean largeText) {
        this.isLargeText = largeText;
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_LARGE_TEXT, largeText).apply();
        }
        notifyListeners();
    }

    public boolean isHighContrast() {
        return isHighContrast || isSeniorMode;
    }

    public void setHighContrast(boolean highContrast) {
        this.isHighContrast = highContrast;
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_HIGH_CONTRAST, highContrast).apply();
        }
        notifyListeners();
    }

    public boolean isHindi() {
        return isHindi;
    }

    public void setHindi(boolean hindi) {
        this.isHindi = hindi;
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_HINDI, hindi).apply();
        }
        notifyListeners();
    }

    public int getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(int safetyScore) {
        this.safetyScore = safetyScore;
        notifyListeners();
    }

    public String getFamilyPairingCode() {
        return familyPairingCode;
    }

    public void setFamilyPairingCode(String familyPairingCode) {
        this.familyPairingCode = familyPairingCode;
        notifyListeners();
    }

    public boolean isSafetyAgreementAccepted() {
        return safetyAgreementAccepted;
    }

    public void setSafetyAgreementAccepted(boolean safetyAgreementAccepted) {
        this.safetyAgreementAccepted = safetyAgreementAccepted;
        notifyListeners();
    }

    public List<FamilyDevice> getFamilyDevices() {
        return Collections.unmodifiableList(familyDevices);
    }

    public void addFamilyDevice(FamilyDevice device) {
        familyDevices.add(device);
        notifyListeners();
    }

    public List<SafetyReport> getSafetyReports() {
        return Collections.unmodifiableList(safetyReports);
    }

    public void addSafetyReport(SafetyReport report) {
        safetyReports.add(0, report);
        notifyListeners();
    }

    public List<ActivityEvent> getActivityEvents() {
        return Collections.unmodifiableList(activityEvents);
    }

    public void addActivityEvent(ActivityEvent event) {
        activityEvents.add(0, event);
        notifyListeners();
    }

    public List<ChatMessage> getChatMessages() {
        return Collections.unmodifiableList(chatMessages);
    }

    public void addChatMessage(ChatMessage message) {
        chatMessages.add(message);
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        stateListeners.remove(listener);
    }

    public void notifyListeners() {
        for (Runnable r : stateListeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
