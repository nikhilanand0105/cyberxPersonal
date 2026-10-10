package dev.saathi.personal.ui.screens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import dev.saathi.personal.R;
import dev.saathi.personal.ui.model.FamilyDevice;
import dev.saathi.personal.ui.model.SafetyReport;
import dev.saathi.personal.ui.state.AppState;
import dev.saathi.storage.TrustedHelper;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FamilyScreenController {

    private final Activity activity;
    private final float density;
    private ScrollView rootView;
    private LinearLayout contentLayout;

    public FamilyScreenController(Activity activity) {
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

        // 1. Header
        contentLayout.addView(buildHeader(senior));

        // 2. Explicit Privacy Notice (Zero Covert Surveillance)
        contentLayout.addView(buildPrivacyModelCard(senior));

        // 3. Pairing Code & Invite Card
        contentLayout.addView(buildPairingCodeCard(senior));

        // 4. Paired Family Devices
        contentLayout.addView(buildPairedDevicesSection(senior));

        // 5. Family Safety Agreement Card
        contentLayout.addView(buildSafetyAgreementCard(senior));

        // 6. Child / Senior-Initiated Safety Reports
        contentLayout.addView(buildSafetyReportsSection(senior));

        // 7. Trusted Emergency Contacts
        contentLayout.addView(buildTrustedContactsSection(senior));
    }

    private View buildHeader(boolean senior) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, 0, 0, (int) (16 * density));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(senior ? "FAMILY SAFETY NETWORK" : "FAMILY DIGITAL SAFETY & CONSENT");
        tvTitle.setTextSize(senior ? 24 : 22);
        tvTitle.setTextColor(0xFF141414);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText(senior
                ? "Protect your loved ones through mutual consent, safety agreements, and verified alerts."
                : "Transparent family administration, paired device health, shared agreements & child reports");
        tvSub.setTextSize(senior ? 15 : 13);
        tvSub.setTextColor(0xFF666666);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sp.topMargin = (int) (4 * density);
        tvSub.setLayoutParams(sp);
        header.addView(tvSub);

        return header;
    }

    private View buildPrivacyModelCard(boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (16 * density);
        card.setLayoutParams(params);

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(activity);
        label.setText("ZERO-COVERT-SURVEILLANCE GUARANTEE");
        label.setTextSize(senior ? 13 : 11);
        label.setTextColor(0xFF386B57);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        topRow.addView(label);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        topRow.addView(sp);

        TextView badge = new TextView(activity);
        badge.setText("CONSENT FIRST");
        badge.setTextSize(senior ? 11 : 9);
        badge.setTextColor(0xFF386B57);
        badge.setBackgroundResource(R.drawable.bg_pill_emerald);
        badge.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
        topRow.addView(badge);
        card.addView(topRow);

        TextView explanation = new TextView(activity);
        explanation.setText("Saathi is built on trust, transparency, and family consent. Saathi Personal explicitly NEVER includes:\n" +
                "• No stealth background chat reading or call eavesdropping\n" +
                "• No hidden keylogger or screen recording spyware\n" +
                "• No covert location stalking without active notification\n\n" +
                "Family safety is achieved through mutual agreements, on-device app analysis, and child-initiated alerts.");
        explanation.setTextSize(senior ? 14 : 12);
        explanation.setTextColor(0xFF333333);
        explanation.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.topMargin = (int) (8 * density);
        explanation.setLayoutParams(ep);
        card.addView(explanation);

        return card;
    }

    private View buildPairingCodeCard(boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_interactive);
        card.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (16 * density);
        card.setLayoutParams(params);

        TextView label = new TextView(activity);
        label.setText("PAIR A FAMILY MEMBER'S DEVICE");
        label.setTextSize(senior ? 13 : 11);
        label.setTextColor(0xFF888888);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(label);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = (int) (10 * density);
        row.setLayoutParams(rp);

        TextView tvCode = new TextView(activity);
        tvCode.setText(AppState.get().getFamilyPairingCode());
        tvCode.setTextSize(senior ? 24 : 20);
        tvCode.setTextColor(0xFF141414);
        tvCode.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        row.addView(tvCode);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(sp);

        Button btnCopy = new Button(activity);
        btnCopy.setText("📋 Copy Code");
        btnCopy.setBackgroundResource(R.drawable.bg_pill_black);
        btnCopy.setTextColor(0xFFFFFFFF);
        btnCopy.setTextSize(senior ? 13 : 11);
        btnCopy.setTypeface(null, android.graphics.Typeface.BOLD);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData cd = ClipData.newPlainText("Saathi Pairing Code", AppState.get().getFamilyPairingCode());
            if (cm != null) cm.setPrimaryClip(cd);
            Toast.makeText(activity, "Pairing code copied to clipboard", Toast.LENGTH_SHORT).show();
        });
        row.addView(btnCopy);

        card.addView(row);

        TextView sub = new TextView(activity);
        sub.setText("Enter this code in Saathi Personal on your child's or parent's phone to connect them to this family circle.");
        sub.setTextSize(senior ? 13 : 11);
        sub.setTextColor(0xFF666666);
        LinearLayout.LayoutParams sp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp2.topMargin = (int) (6 * density);
        sub.setLayoutParams(sp2);
        card.addView(sub);

        return card;
    }

    private View buildPairedDevicesSection(boolean senior) {
        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (18 * density);
        section.setLayoutParams(params);

        TextView label = new TextView(activity);
        label.setText("PAIRED FAMILY DEVICES (3 CONNECTED)");
        label.setTextSize(senior ? 14 : 11);
        label.setTextColor(0xFF888888);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (10 * density);
        label.setLayoutParams(lp);
        section.addView(label);

        List<FamilyDevice> devices = AppState.get().getFamilyDevices();
        for (FamilyDevice dev : devices) {
            section.addView(createDeviceCard(dev, senior));
        }

        return section;
    }

    private View createDeviceCard(FamilyDevice dev, boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (8 * density);
        card.setLayoutParams(params);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(activity);
        tvName.setText(dev.getDeviceName());
        tvName.setTextSize(senior ? 16 : 14);
        tvName.setTextColor(0xFF141414);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvName);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(sp);

        TextView tvStatus = new TextView(activity);
        tvStatus.setText(dev.getStatus());
        tvStatus.setTextSize(senior ? 12 : 10);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStatus.setTextColor(0xFF386B57);
        tvStatus.setBackgroundResource(R.drawable.bg_pill_emerald);
        tvStatus.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
        row.addView(tvStatus);

        card.addView(row);

        TextView tvDetails = new TextView(activity);
        tvDetails.setText("Assigned to: " + dev.getAssignedMember() + " • Sync: " + dev.getLastSync());
        tvDetails.setTextSize(senior ? 13 : 11);
        tvDetails.setTextColor(0xFF666666);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dp.topMargin = (int) (4 * density);
        tvDetails.setLayoutParams(dp);
        card.addView(tvDetails);

        return card;
    }

    private View buildSafetyAgreementCard(boolean senior) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_cred);
        card.setPadding((int) (18 * density), (int) (16 * density), (int) (18 * density), (int) (16 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (18 * density);
        card.setLayoutParams(params);

        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText("FAMILY DIGITAL SAFETY AGREEMENT");
        title.setTextSize(senior ? 14 : 12);
        title.setTextColor(0xFF141414);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        topRow.addView(title);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        topRow.addView(sp);

        TextView tvStatus = new TextView(activity);
        tvStatus.setText("MUTUALLY SIGNED");
        tvStatus.setTextSize(senior ? 11 : 9);
        tvStatus.setTextColor(0xFF386B57);
        tvStatus.setBackgroundResource(R.drawable.bg_pill_emerald);
        tvStatus.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
        topRow.addView(tvStatus);
        card.addView(topRow);

        TextView rules = new TextView(activity);
        rules.setText("1. Parents agree not to install hidden surveillance or read personal messages.\n" +
                "2. Children agree to consult parents before sideloading apps from unknown websites.\n" +
                "3. If anyone receives a threatening or uncomfortable message, they agree to share it safely without fear of punishment.\n" +
                "4. Financial accounts and OTPs must never be shared without family consultation.");
        rules.setTextSize(senior ? 14 : 12);
        rules.setTextColor(0xFF444444);
        rules.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = (int) (10 * density);
        rules.setLayoutParams(rp);
        card.addView(rules);

        return card;
    }

    private View buildSafetyReportsSection(boolean senior) {
        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = (int) (18 * density);
        section.setLayoutParams(params);

        TextView label = new TextView(activity);
        label.setText("CHILD & SENIOR INITIATED SAFETY REPORTS");
        label.setTextSize(senior ? 14 : 11);
        label.setTextColor(0xFF888888);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (10 * density);
        label.setLayoutParams(lp);
        section.addView(label);

        List<SafetyReport> reports = AppState.get().getSafetyReports();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

        for (SafetyReport rep : reports) {
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setBackgroundResource(R.drawable.bg_card_interactive);
            item.setPadding((int) (16 * density), (int) (14 * density), (int) (16 * density), (int) (14 * density));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ip.bottomMargin = (int) (8 * density);
            item.setLayoutParams(ip);

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvCategory = new TextView(activity);
            tvCategory.setText(rep.getCategory() + " (" + rep.getSubmittedBy() + ")");
            tvCategory.setTextSize(senior ? 15 : 13);
            tvCategory.setTextColor(0xFF141414);
            tvCategory.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tvCategory);

            View sp = new View(activity);
            sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
            row.addView(sp);

            TextView tvDate = new TextView(activity);
            tvDate.setText(sdf.format(rep.getTimestamp()));
            tvDate.setTextSize(senior ? 12 : 10);
            tvDate.setTextColor(0xFF888888);
            row.addView(tvDate);
            item.addView(row);

            TextView tvExcerpt = new TextView(activity);
            tvExcerpt.setText("\"" + rep.getExcerpt() + "\"");
            tvExcerpt.setTextSize(senior ? 14 : 12);
            tvExcerpt.setTextColor(0xFF666666);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ep.topMargin = (int) (6 * density);
            tvExcerpt.setLayoutParams(ep);
            item.addView(tvExcerpt);

            TextView tvAction = new TextView(activity);
            tvAction.setText("Action: " + rep.getActionTaken());
            tvAction.setTextSize(senior ? 13 : 11);
            tvAction.setTextColor(0xFF386B57);
            tvAction.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ap.topMargin = (int) (4 * density);
            tvAction.setLayoutParams(ap);
            item.addView(tvAction);

            section.addView(item);
        }

        return section;
    }

    private View buildTrustedContactsSection(boolean senior) {
        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        section.setLayoutParams(params);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = (int) (10 * density);
        row.setLayoutParams(rp);

        TextView label = new TextView(activity);
        label.setText("TRUSTED EMERGENCY HELPERS");
        label.setTextSize(senior ? 14 : 11);
        label.setTextColor(0xFF888888);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(label);

        View sp = new View(activity);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(sp);

        TextView btnAdd = new TextView(activity);
        btnAdd.setText("+ Add Helper");
        btnAdd.setTextSize(senior ? 14 : 12);
        btnAdd.setTextColor(0xFF141414);
        btnAdd.setTypeface(null, android.graphics.Typeface.BOLD);
        btnAdd.setOnClickListener(v -> showAddHelperDialog());
        row.addView(btnAdd);

        section.addView(row);

        List<TrustedHelper> helpers = AppState.get().getStorageRepository().getTrustedHelpers();
        for (TrustedHelper th : helpers) {
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setBackgroundResource(R.drawable.bg_card_cred);
            item.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ip.bottomMargin = (int) (8 * density);
            item.setLayoutParams(ip);

            LinearLayout col = new LinearLayout(activity);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            col.setLayoutParams(cp);

            TextView tvName = new TextView(activity);
            tvName.setText(th.getName());
            tvName.setTextSize(senior ? 16 : 14);
            tvName.setTextColor(0xFF141414);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            col.addView(tvName);

            TextView tvPhone = new TextView(activity);
            tvPhone.setText(th.getPhoneNumber() + " • " + th.getRelationship());
            tvPhone.setTextSize(senior ? 13 : 11);
            tvPhone.setTextColor(0xFF666666);
            col.addView(tvPhone);

            item.addView(col);

            Button btnCall = new Button(activity);
            btnCall.setText("📞 Call");
            btnCall.setBackgroundResource(R.drawable.bg_pill_black);
            btnCall.setTextColor(0xFFFFFFFF);
            btnCall.setTextSize(senior ? 13 : 11);
            btnCall.setTypeface(null, android.graphics.Typeface.BOLD);
            btnCall.setOnClickListener(v -> {
                Intent dial = new Intent(Intent.ACTION_DIAL);
                dial.setData(Uri.parse("tel:" + th.getPhoneNumber().replaceAll("[^0-9+]", "")));
                try {
                    activity.startActivity(dial);
                } catch (Exception ignored) {}
            });
            item.addView(btnCall);

            section.addView(item);
        }

        return section;
    }

    private void showAddHelperDialog() {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding((int) (20 * density), (int) (10 * density), (int) (20 * density), (int) (10 * density));

        EditText etName = new EditText(activity);
        etName.setHint("Helper Name (e.g., Uncle Suresh)");
        form.addView(etName);

        EditText etPhone = new EditText(activity);
        etPhone.setHint("Phone Number (+91 ...)");
        form.addView(etPhone);

        new AlertDialog.Builder(activity)
                .setTitle("Add Trusted Helper")
                .setView(form)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    if (!name.isEmpty() && !phone.isEmpty()) {
                        TrustedHelper helper = new TrustedHelper("th-" + System.currentTimeMillis(), name, "TRUSTED_FAMILY", phone, true);
                        AppState.get().getStorageRepository().addTrustedHelper(helper);
                        render();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
