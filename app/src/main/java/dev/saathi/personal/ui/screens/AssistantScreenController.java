package dev.saathi.personal.ui.screens;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
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

import dev.saathi.personal.R;
import dev.saathi.personal.ui.model.ChatMessage;
import dev.saathi.personal.ui.state.AppState;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AssistantScreenController {

    private final Activity activity;
    private final float density;
    private ScrollView rootView;
    private LinearLayout contentLayout;

    private LinearLayout containerMessages;
    private ScrollView svChat;
    private EditText etInput;
    private Runnable closeCallback;

    public AssistantScreenController(Activity activity) {
        this.activity = activity;
        this.density = activity.getResources().getDisplayMetrics().density;
        createView();
    }

    public View getView() {
        return rootView;
    }

    public void setCloseCallback(Runnable closeCallback) {
        this.closeCallback = closeCallback;
        render();
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

        // 1. Header & Privacy Badge
        contentLayout.addView(buildHeader(senior));

        // 2. Suggestion Prompt Pills
        contentLayout.addView(buildSuggestionPills(senior));

        // 3. Chat Messages Stream Container
        svChat = new ScrollView(activity);
        LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (380 * density)
        );
        scp.topMargin = (int) (8 * density);
        scp.bottomMargin = (int) (12 * density);
        svChat.setLayoutParams(scp);
        svChat.setBackgroundResource(R.drawable.bg_card_cred);
        svChat.setPadding((int) (12 * density), (int) (12 * density), (int) (12 * density), (int) (12 * density));

        containerMessages = new LinearLayout(activity);
        containerMessages.setOrientation(LinearLayout.VERTICAL);
        svChat.addView(containerMessages);
        contentLayout.addView(svChat);

        // Render message bubbles
        List<ChatMessage> messages = AppState.get().getChatMessages();
        for (ChatMessage msg : messages) {
            containerMessages.addView(createMessageBubble(msg, senior));
        }

        // Auto-scroll to bottom
        svChat.post(() -> svChat.fullScroll(View.FOCUS_DOWN));

        // 4. Input Row
        contentLayout.addView(buildInputRow(senior));
    }

    private View buildHeader(boolean senior) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, 0, 0, (int) (10 * density));

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText(senior ? "SAFETY COPILOT" : "SAATHI AI SAFETY COPILOT");
        title.setTextSize(senior ? 24 : 22);
        title.setTextColor(0xFF141414);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        topRow.addView(title);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        topRow.addView(sp);

        if (closeCallback != null) {
            Button btnClose = new Button(activity);
            btnClose.setText("✕ Close");
            btnClose.setBackgroundResource(R.drawable.bg_pill_dark);
            btnClose.setTextColor(0xFF141414);
            btnClose.setTextSize(12);
            btnClose.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (36 * density));
            btnClose.setLayoutParams(cp);
            btnClose.setOnClickListener(v -> closeCallback.run());
            topRow.addView(btnClose);
        }

        header.addView(topRow);

        TextView sub = new TextView(activity);
        sub.setText("Conversational security intelligence running privately on Snapdragon 8 Elite NPU");
        sub.setTextSize(senior ? 14 : 13);
        sub.setTextColor(0xFF666666);
        LinearLayout.LayoutParams sp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sp2.topMargin = (int) (4 * density);
        sub.setLayoutParams(sp2);
        header.addView(sub);

        TextView tvPrivacy = new TextView(activity);
        tvPrivacy.setText("🔒 ZERO CLOUD TELEMETRY • NPU ISOLATED • 100% PRIVATE");
        tvPrivacy.setTextSize(senior ? 11 : 10);
        tvPrivacy.setTextColor(0xFF386B57);
        tvPrivacy.setBackgroundResource(R.drawable.bg_pill_emerald);
        tvPrivacy.setPadding((int) (10 * density), (int) (5 * density), (int) (10 * density), (int) (5 * density));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        pp.topMargin = (int) (8 * density);
        tvPrivacy.setLayoutParams(pp);
        header.addView(tvPrivacy);

        return header;
    }

    private View buildSuggestionPills(boolean senior) {
        HorizontalScrollView hsv = new HorizontalScrollView(activity);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hp.topMargin = (int) (6 * density);
        hp.bottomMargin = (int) (6 * density);
        hsv.setLayoutParams(hp);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);

        row.addView(createSuggestionChip("What is a Trojan APK?", "What is a Trojan APK and why is it dangerous?", senior));
        row.addView(createSuggestionChip("When to call 1930?", "What is the 1930 National Cyber Helpline and how quickly should I call?", senior));
        row.addView(createSuggestionChip("Recognize Phishing Links", "How do I recognize fake banking and KYC update links?", senior));
        row.addView(createSuggestionChip("Is GB WhatsApp safe?", "Is modified WhatsApp or GB WhatsApp safe to install?", senior));
        row.addView(createSuggestionChip("साइबर सुरक्षा टिप्स", "कृपया मुझे ऑनलाइन फ्रॉड से बचने के ज़रूरी टिप्स बताइए।", senior));

        hsv.addView(row);
        return hsv;
    }

    private Button createSuggestionChip(String label, String prompt, boolean senior) {
        Button chip = new Button(activity);
        chip.setText(label);
        chip.setTextSize(senior ? 12 : 11);
        chip.setTextColor(0xFF141414);
        chip.setBackgroundResource(R.drawable.bg_pill_dark);
        chip.setOnClickListener(v -> sendMessage(prompt));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (int) (36 * density)
        );
        cp.rightMargin = (int) (8 * density);
        chip.setLayoutParams(cp);
        return chip;
    }

    private View buildInputRow(boolean senior) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rp);

        etInput = new EditText(activity);
        etInput.setHint("Ask Copilot about any security question...");
        etInput.setHintTextColor(0xFF888888);
        etInput.setTextColor(0xFF141414);
        etInput.setTextSize(senior ? 15 : 13);
        etInput.setBackgroundResource(R.drawable.bg_input_field);
        etInput.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, (int) (48 * density), 1f);
        ep.rightMargin = (int) (8 * density);
        etInput.setLayoutParams(ep);
        row.addView(etInput);

        Button btnSend = new Button(activity);
        btnSend.setText("Send");
        btnSend.setTextSize(senior ? 14 : 12);
        btnSend.setTextColor(0xFFFFFFFF);
        btnSend.setBackgroundResource(R.drawable.bg_pill_black);
        btnSend.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams((int) (72 * density), (int) (48 * density));
        btnSend.setLayoutParams(bp);
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                etInput.setText("");
                sendMessage(text);
            }
        });
        row.addView(btnSend);

        return row;
    }

    private View createMessageBubble(ChatMessage msg, boolean senior) {
        LinearLayout bubbleContainer = new LinearLayout(activity);
        bubbleContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bcp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bcp.bottomMargin = (int) (10 * density);
        bubbleContainer.setLayoutParams(bcp);
        bubbleContainer.setGravity(msg.isUser ? Gravity.END : Gravity.START);

        LinearLayout bubble = new LinearLayout(activity);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding((int) (14 * density), (int) (10 * density), (int) (14 * density), (int) (10 * density));
        bubble.setBackgroundResource(msg.isUser ? R.drawable.bg_pill_black : R.drawable.bg_pill_dark);

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bp.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.75f);
        bubble.setLayoutParams(bp);

        TextView tvSender = new TextView(activity);
        tvSender.setText(msg.badge);
        tvSender.setTextSize(senior ? 11 : 10);
        tvSender.setTextColor(msg.isUser ? 0xFFFFFFFF : 0xFF386B57);
        tvSender.setTypeface(null, android.graphics.Typeface.BOLD);
        bubble.addView(tvSender);

        TextView tvText = new TextView(activity);
        tvText.setText(msg.text);
        tvText.setTextSize(senior ? 15 : 13);
        tvText.setTextColor(msg.isUser ? 0xFFFFFFFF : 0xFF141414);
        tvText.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tp.topMargin = (int) (4 * density);
        tvText.setLayoutParams(tp);
        bubble.addView(tvText);

        TextView tvTime = new TextView(activity);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvTime.setText(sdf.format(msg.timestamp));
        tvTime.setTextSize(senior ? 10 : 9);
        tvTime.setTextColor(msg.isUser ? 0xFFCCCCCC : 0xFF888888);
        LinearLayout.LayoutParams tmp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tmp.topMargin = (int) (4 * density);
        tmp.gravity = Gravity.END;
        tvTime.setLayoutParams(tmp);
        bubble.addView(tvTime);

        bubbleContainer.addView(bubble);
        return bubbleContainer;
    }

    public void promptAssistant(String prompt) {
        if (prompt != null && !prompt.isBlank()) {
            sendMessage(prompt);
        }
    }

    private void sendMessage(String prompt) {
        AppState.get().addChatMessage(new ChatMessage(true, prompt, "You"));
        render();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String answer = generateOnDeviceAnswer(prompt);
            AppState.get().addChatMessage(new ChatMessage(false, answer, "Saathi Safety Copilot"));
            render();
        }, 250);
    }

    private String generateOnDeviceAnswer(String query) {
        String lower = query.toLowerCase(Locale.ROOT);

        if (lower.contains("trojan") || lower.contains("apk") || lower.contains("whatsapp")) {
            return "🛡️ Android Banking Trojans & Modded APKs:\n\n" +
                    "Fake or modified APKs (like GB WhatsApp or fake banking updates) often bundle malicious DEX payloads. " +
                    "They abuse Android's BIND_ACCESSIBILITY_SERVICE to read screen contents, inject invisible tap overlays (SYSTEM_ALERT_WINDOW), " +
                    "and intercept incoming SMS to steal OTPs.\n\n" +
                    "Saathi inspects these packages statically before installation on your iQOO 15 without ever executing untrusted code.";
        }

        if (lower.contains("link") || lower.contains("phishing") || lower.contains("url") || lower.contains("sbi")) {
            return "🔗 Phishing & Fraud Link Guidance:\n\n" +
                    "Banks never send SMS messages with links on domains ending in .top, .xyz, or raw IP addresses.\n" +
                    "1. Never enter your net banking username, password, or debit card number on unverified links.\n" +
                    "2. Always type the official bank website directly in Chrome (e.g., https://onlinesbi.sbi).\n" +
                    "3. If you clicked an unfamiliar link, change your banking password immediately from another device.";
        }

        if (lower.contains("grooming") || lower.contains("child") || lower.contains("secret")) {
            return "👁️ Child Online Safety & Grooming Detection:\n\n" +
                    "Groomers manipulate children using 3 consistent psychological cues:\n" +
                    "1. Isolation / Secrecy: 'Mummy-papa ko mat batana'.\n" +
                    "2. Platform Hopping: Moving from public gaming chats to private disappearing apps.\n" +
                    "3. Boundary Erosion: Asking to turn on video cameras or demanding private photos.\n\n" +
                    "Saathi Sense runs on-device Hinglish NLP to flag these cues immediately without uploading personal chats to the cloud.";
        }

        if (lower.contains("1930") || lower.contains("helpline") || lower.contains("fraud") || lower.contains("money")) {
            return "📞 National Cyber Crime Helpline (1930):\n\n" +
                    "If you or a family member have been tricked into transferring money or sharing an OTP, dial 1930 immediately.\n\n" +
                    "Under India's Citizen Financial Cyber Fraud Reporting System (CFCFRMS), calls made within the 'Golden Hour' (first 2 hours) allow law enforcement to freeze the fraudster's bank accounts and UPI nodes before funds are withdrawn at ATMs.";
        }

        if (lower.contains("हिंदी") || lower.contains("hindi") || lower.contains("टिप्स")) {
            return "🛡️ आवश्यक डिजिटल सुरक्षा सलाह (हिन्दी):\n\n" +
                    "1. कभी भी किसी अनजान कॉलर के कहने पर कोई ऐप (.apk) डाउनलोड न करें।\n" +
                    "2. बैंक या बिजली बिल के नाम पर आए अनजान लिंक (SMS) पर क्लिक न करें।\n" +
                    "3. यदि पैसे कट जाएं, तो तुरंत 1930 पर कॉल करें।\n" +
                    "4. बच्चों को सिखाएं कि इंटरनेट पर किसी अजनबी से गुप्त बातें या फ़ोटो कभी साझा न करें।";
        }

        return "Saathi AI Safety Copilot is monitoring your device actively. " +
                "I analyze APK structures, conversation manipulation cues, and fraudulent bank links in real time on your Snapdragon 8 Elite. " +
                "Feel free to ask about any specific app, link, or scam message!";
    }
}
