package com.educarpool;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AgreementPanelActivity extends AppCompatActivity {

    private TextInputEditText etPickupTime, etDepartureTime, etPickupLocation, etDropoffLocation, etPrice, etNotes;
    private ChipGroup chipGroupDays;
    private MaterialButton btnProposeAgreement, btnAcceptAgreement, btnRejectAgreement, btnEditAgreement;
    private MaterialButton btnSendReminder, btnAgreementHistory;
    private TextView tvPendingAgreement, tvPendingAgreementDetails, tvActiveAgreement, tvActiveAgreementDetails;
    private MaterialCardView cardPendingAgreement, cardActiveAgreement, cardNewAgreement;
    private View dividerActive, dividerPending;

    private AgreementRepository agreementRepository;
    private UserRepository userRepository;
    private String matchId;
    private String userEmail;
    private String otherUserEmail;
    private String otherUserName;
    private Agreement pendingAgreement;
    private Agreement activeAgreement;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement_panel);

        agreementRepository = new AgreementRepository();
        userRepository = new UserRepository();

        // Get data from intent
        matchId = getIntent().getStringExtra("match_id");
        userEmail = getIntent().getStringExtra("user_email");
        otherUserEmail = getIntent().getStringExtra("other_user_email");

        if (matchId == null || userEmail == null || otherUserEmail == null) {
            Toast.makeText(this, "Error loading agreement panel", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        loadOtherUserName();
        loadAgreements();
    }

    private void initializeViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        etPickupTime = findViewById(R.id.et_pickup_time);
        etDepartureTime = findViewById(R.id.et_departure_time);
        etPickupLocation = findViewById(R.id.et_pickup_location);
        etDropoffLocation = findViewById(R.id.et_dropoff_location);
        etPrice = findViewById(R.id.et_price);
        etNotes = findViewById(R.id.et_notes);
        chipGroupDays = findViewById(R.id.chip_group_days);
        btnProposeAgreement = findViewById(R.id.btn_propose_agreement);
        btnEditAgreement = findViewById(R.id.btn_edit_agreement);

        btnAcceptAgreement = findViewById(R.id.btn_accept_agreement);
        btnRejectAgreement = findViewById(R.id.btn_reject_agreement);
        tvPendingAgreement = findViewById(R.id.tv_pending_agreement_title);
        tvPendingAgreementDetails = findViewById(R.id.tv_pending_agreement_details);
        tvActiveAgreement = findViewById(R.id.tv_active_agreement_title);
        tvActiveAgreementDetails = findViewById(R.id.tv_active_agreement_details);
        cardPendingAgreement = findViewById(R.id.card_pending_agreement);
        cardActiveAgreement = findViewById(R.id.card_active_agreement);
        cardNewAgreement = findViewById(R.id.card_new_agreement);
        dividerActive = findViewById(R.id.divider_active);
        dividerPending = findViewById(R.id.divider_pending);

        // Initialize new buttons
        btnSendReminder = findViewById(R.id.btn_send_reminder);
        btnAgreementHistory = findViewById(R.id.btn_agreement_history);

        if (btnSendReminder != null) {
            btnSendReminder.setOnClickListener(v -> sendManualReminder());
        }
        if (btnAgreementHistory != null) {
            btnAgreementHistory.setOnClickListener(v -> showAgreementHistory());
        }

        // Hide sections initially
        cardPendingAgreement.setVisibility(View.GONE);
        cardActiveAgreement.setVisibility(View.GONE);
        tvPendingAgreement.setVisibility(View.GONE);
        tvActiveAgreement.setVisibility(View.GONE);
        dividerActive.setVisibility(View.GONE);
        dividerPending.setVisibility(View.GONE);
        btnEditAgreement.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        // Time pickers
        etPickupTime.setOnClickListener(v -> showTimePicker(etPickupTime));
        etDepartureTime.setOnClickListener(v -> showTimePicker(etDepartureTime));

        // Propose agreement button
        btnProposeAgreement.setOnClickListener(v -> proposeAgreement());

        // Edit agreement button
        btnEditAgreement.setOnClickListener(v -> enterEditMode());

        // Accept and reject buttons
        btnAcceptAgreement.setOnClickListener(v -> acceptAgreement());
        btnRejectAgreement.setOnClickListener(v -> rejectAgreement());
    }

    private void loadOtherUserName() {
        userRepository.getUserByEmail(otherUserEmail, new UserRepository.UserFetchCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    otherUserName = user.getName();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    otherUserName = otherUserEmail.split("@")[0];
                });
            }
        });
    }

    private void showTimePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute1);
                    editText.setText(time);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void loadAgreements() {
        agreementRepository.getAgreementsByMatch(matchId, new AgreementRepository.AgreementsCallback() {
            @Override
            public void onSuccess(List<Agreement> agreements) {
                runOnUiThread(() -> {
                    if (agreements == null || agreements.isEmpty()) {
                        Log.d("AgreementPanel", "No agreements found for this match");
                        showEmptyState();
                        return;
                    }

                    pendingAgreement = null;
                    activeAgreement = null;

                    // Check for pending agreement (proposed by the other user and status is "proposed")
                    for (Agreement agreement : agreements) {
                        if (agreement.getStatus().equals("proposed") && !agreement.getProposedBy().equals(userEmail)) {
                            pendingAgreement = agreement;
                            showPendingAgreement(agreement);
                        }
                    }

                    // Check for active agreement
                    for (Agreement agreement : agreements) {
                        if (agreement.getStatus().equals("active")) {
                            activeAgreement = agreement;
                            showActiveAgreement(agreement);
                            break;
                        }
                    }

                    // If no active agreement but there are agreements, show the latest proposed one
                    if (activeAgreement == null && !agreements.isEmpty()) {
                        showNewAgreementState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("AgreementPanel", "Error loading agreements: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(AgreementPanelActivity.this, "Error loading agreements", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    private void showEmptyState() {
        cardNewAgreement.setVisibility(View.VISIBLE);
        btnEditAgreement.setVisibility(View.GONE);
    }

    private void showNewAgreementState() {
        cardNewAgreement.setVisibility(View.VISIBLE);
        btnEditAgreement.setVisibility(View.GONE);
        clearForm();
    }

    private void showPendingAgreement(Agreement agreement) {
        tvPendingAgreement.setVisibility(View.VISIBLE);
        cardPendingAgreement.setVisibility(View.VISIBLE);
        dividerPending.setVisibility(View.VISIBLE);

        String proposedByName = agreement.getProposedBy().equals(userEmail) ? "You" : otherUserName;

        StringBuilder details = new StringBuilder();
        details.append("Proposed by: ").append(proposedByName).append("\n\n");

        if (agreement.getPickupTime() != null && !agreement.getPickupTime().isEmpty()) {
            details.append("🕒 Pickup: ").append(agreement.getPickupTime()).append("\n");
        }
        if (agreement.getDepartureTime() != null && !agreement.getDepartureTime().isEmpty()) {
            details.append("🕒 Departure: ").append(agreement.getDepartureTime()).append("\n");
        }
        if (agreement.getPickupLocation() != null && !agreement.getPickupLocation().isEmpty()) {
            details.append("📍 Pickup: ").append(agreement.getPickupLocation()).append("\n");
        }
        if (agreement.getDropoffLocation() != null && !agreement.getDropoffLocation().isEmpty()) {
            details.append("📍 Drop-off: ").append(agreement.getDropoffLocation()).append("\n");
        }
        if (agreement.getPrice() > 0) {
            details.append("💰 Price: R").append(agreement.getPrice()).append("\n");
        }
        if (agreement.getDaysOfWeek() != null && !agreement.getDaysOfWeek().isEmpty()) {
            details.append("🗓️ Days: ").append(String.join(", ", agreement.getDaysOfWeek())).append("\n");
        }
        if (agreement.getNotes() != null && !agreement.getNotes().isEmpty()) {
            details.append("📝 Notes: ").append(agreement.getNotes()).append("\n");
        }

        tvPendingAgreementDetails.setText(details.toString());
    }

    private void showActiveAgreement(Agreement agreement) {
        tvActiveAgreement.setVisibility(View.VISIBLE);
        cardActiveAgreement.setVisibility(View.VISIBLE);
        dividerActive.setVisibility(View.VISIBLE);
        cardNewAgreement.setVisibility(View.GONE);
        btnEditAgreement.setVisibility(View.VISIBLE);

        StringBuilder details = new StringBuilder();
        details.append("✅ ACTIVE AGREEMENT\n\n");

        if (agreement.getPickupTime() != null && !agreement.getPickupTime().isEmpty()) {
            details.append("🕒 Pickup: ").append(agreement.getPickupTime()).append("\n");
        }
        if (agreement.getDepartureTime() != null && !agreement.getDepartureTime().isEmpty()) {
            details.append("🕒 Departure: ").append(agreement.getDepartureTime()).append("\n");
        }
        if (agreement.getPickupLocation() != null && !agreement.getPickupLocation().isEmpty()) {
            details.append("📍 Pickup: ").append(agreement.getPickupLocation()).append("\n");
        }
        if (agreement.getDropoffLocation() != null && !agreement.getDropoffLocation().isEmpty()) {
            details.append("📍 Drop-off: ").append(agreement.getDropoffLocation()).append("\n");
        }
        if (agreement.getPrice() > 0) {
            details.append("💰 Price: R").append(agreement.getPrice()).append("\n");
        }
        if (agreement.getDaysOfWeek() != null && !agreement.getDaysOfWeek().isEmpty()) {
            details.append("🗓️ Days: ").append(String.join(", ", agreement.getDaysOfWeek())).append("\n");
        }
        if (agreement.getNotes() != null && !agreement.getNotes().isEmpty()) {
            details.append("📝 Notes: ").append(agreement.getNotes()).append("\n");
        }

        tvActiveAgreementDetails.setText(details.toString());
    }

    private void enterEditMode() {
        isEditMode = true;
        cardNewAgreement.setVisibility(View.VISIBLE);
        btnEditAgreement.setVisibility(View.GONE);
        btnProposeAgreement.setText("📝 Update Agreement");

        // Pre-fill form with current active agreement data
        if (activeAgreement != null) {
            if (activeAgreement.getPickupTime() != null) {
                etPickupTime.setText(activeAgreement.getPickupTime());
            }
            if (activeAgreement.getDepartureTime() != null) {
                etDepartureTime.setText(activeAgreement.getDepartureTime());
            }
            if (activeAgreement.getPickupLocation() != null) {
                etPickupLocation.setText(activeAgreement.getPickupLocation());
            }
            if (activeAgreement.getDropoffLocation() != null) {
                etDropoffLocation.setText(activeAgreement.getDropoffLocation());
            }
            if (activeAgreement.getPrice() > 0) {
                etPrice.setText(String.valueOf(activeAgreement.getPrice()));
            }
            if (activeAgreement.getNotes() != null) {
                etNotes.setText(activeAgreement.getNotes());
            }
            if (activeAgreement.getDaysOfWeek() != null) {
                for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
                    Chip chip = (Chip) chipGroupDays.getChildAt(i);
                    if (activeAgreement.getDaysOfWeek().contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }
                }
            }
        }
    }

    private void clearForm() {
        etPickupTime.setText("");
        etDepartureTime.setText("");
        etPickupLocation.setText("");
        etDropoffLocation.setText("");
        etPrice.setText("");
        etNotes.setText("");
        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupDays.getChildAt(i);
            chip.setChecked(false);
        }
    }

    private void proposeAgreement() {
        String pickupTime = etPickupTime.getText().toString().trim();
        String departureTime = etDepartureTime.getText().toString().trim();
        String pickupLocation = etPickupLocation.getText().toString().trim();
        String dropoffLocation = etDropoffLocation.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Check if at least one field is filled
        boolean hasPickupTime = !pickupTime.isEmpty();
        boolean hasDepartureTime = !departureTime.isEmpty();
        boolean hasPickupLocation = !pickupLocation.isEmpty();
        boolean hasDropoffLocation = !dropoffLocation.isEmpty();
        boolean hasPrice = !priceStr.isEmpty();
        boolean hasNotes = !notes.isEmpty();

        // Check selected days
        boolean hasSelectedDays = false;
        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupDays.getChildAt(i);
            if (chip.isChecked()) {
                hasSelectedDays = true;
                break;
            }
        }

        if (!hasPickupTime && !hasDepartureTime && !hasPickupLocation &&
                !hasDropoffLocation && !hasPrice && !hasNotes && !hasSelectedDays) {
            Toast.makeText(this, "Please fill in at least one field to propose an agreement", Toast.LENGTH_LONG).show();
            return;
        }

        double price = 0;
        if (hasPrice) {
            try {
                price = Double.parseDouble(priceStr);
                if (price < 0) {
                    Toast.makeText(this, "Price cannot be negative", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Get selected days
        List<String> selectedDays = new ArrayList<>();
        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupDays.getChildAt(i);
            if (chip.isChecked()) {
                selectedDays.add(chip.getText().toString());
            }
        }

        // Show loading
        btnProposeAgreement.setEnabled(false);
        btnProposeAgreement.setText(isEditMode ? "Updating..." : "Proposing...");

        Agreement agreement = new Agreement(matchId, userEmail, pickupTime, departureTime,
                pickupLocation, dropoffLocation, price, selectedDays, notes);

        agreementRepository.createAgreement(agreement, new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    btnProposeAgreement.setEnabled(true);
                    btnProposeAgreement.setText("🤝 Propose Agreement");

                    String message = isEditMode ? "Agreement updated successfully" : "Agreement proposed successfully";
                    Toast.makeText(AgreementPanelActivity.this, message, Toast.LENGTH_SHORT).show();

                    // Send notification message in chat
                    sendAgreementNotification(isEditMode ? "updated" : "proposed");

                    // Reload agreements to reflect changes
                    loadAgreements();
                    isEditMode = false;
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnProposeAgreement.setEnabled(true);
                    btnProposeAgreement.setText("🤝 Propose Agreement");
                    Toast.makeText(AgreementPanelActivity.this, "Failed to propose agreement: " + error, Toast.LENGTH_SHORT).show();
                    isEditMode = false;
                });
            }
        });
    }

    // the acceptAgreement method to schedule reminders
    private void acceptAgreement() {
        if (pendingAgreement == null) {
            Toast.makeText(this, "No pending agreement to accept", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAcceptAgreement.setEnabled(false);
        btnAcceptAgreement.setText("Accepting...");

        // First expire other agreements, then accept this one
        agreementRepository.expireOtherAgreements(matchId, pendingAgreement.getAgreementId(), new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                // Now accept the current agreement
                agreementRepository.updateAgreementStatus(pendingAgreement.getAgreementId(), "active", new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(AgreementPanelActivity.this, "Agreement accepted! 🎉", Toast.LENGTH_SHORT).show();

                            // SCHEDULE REMINDERS
                            scheduleAgreementReminders(pendingAgreement);

                            sendAgreementNotification("accepted");
                            loadAgreements();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            btnAcceptAgreement.setEnabled(true);
                            btnAcceptAgreement.setText("Accept Agreement");
                            Toast.makeText(AgreementPanelActivity.this, "Failed to accept agreement: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnAcceptAgreement.setEnabled(true);
                    btnAcceptAgreement.setText("Accept Agreement");
                    Toast.makeText(AgreementPanelActivity.this, "Failed to accept agreement: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Update the rejectAgreement method to cancel reminders
    private void rejectAgreement() {
        if (pendingAgreement == null) {
            Toast.makeText(this, "No pending agreement to reject", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRejectAgreement.setEnabled(false);
        btnRejectAgreement.setText("Rejecting...");

        // CANCEL ANY EXISTING REMINDERS
        cancelAgreementReminders(pendingAgreement.getAgreementId());

        agreementRepository.deleteAgreement(pendingAgreement.getAgreementId(), new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(AgreementPanelActivity.this, "Agreement rejected", Toast.LENGTH_SHORT).show();
                    sendAgreementNotification("rejected");
                    loadAgreements();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnRejectAgreement.setEnabled(true);
                    btnRejectAgreement.setText("Reject");
                    Toast.makeText(AgreementPanelActivity.this, "Failed to reject agreement: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void sendAgreementNotification(String action) {
        String messageText;
        String sender = userEmail.split("@")[0];

        switch (action) {
            case "proposed":
                messageText = "🤝 " + sender + " has proposed a new trip agreement! Tap the handshake icon to review.";
                break;
            case "updated":
                messageText = "📝 " + sender + " has updated the trip agreement! Check the agreement panel for changes.";
                break;
            case "accepted":
                messageText = "✅ Trip agreement confirmed! The carpool arrangement is now active and locked in. 🎉";
                break;
            case "rejected":
                messageText = "❌ " + sender + " has declined the trip agreement proposal.";
                break;
            default:
                messageText = "🤝 Agreement activity from " + sender;
        }

        userRepository.sendMessage(matchId, userEmail, otherUserEmail, messageText,
                new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("AgreementPanel", "Agreement notification sent successfully");
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("AgreementPanel", "Failed to send agreement notification: " + error);
                    }
                });
    }

    // AgreementReminderReceiver
    private void scheduleAgreementReminders(Agreement agreement) {
        AgreementReminderReceiver.scheduleAgreementReminder(this, agreement);
        Log.d("AgreementPanel", "Scheduled reminders for agreement: " + agreement.getAgreementId());
    }

    // AgreementReminderReceiver
    private void cancelAgreementReminders(String agreementId) {
        AgreementReminderReceiver.cancelAgreementReminders(this, agreementId);
        Log.d("AgreementPanel", "Cancelled reminders for agreement: " + agreementId);
    }

    // send manual reminders
    private void sendManualReminder() {
        if (activeAgreement == null) {
            Toast.makeText(this, "No active agreement to remind about", Toast.LENGTH_SHORT).show();
            return;
        }

        agreementRepository.sendAgreementReminder(matchId, userEmail, otherUserEmail, "manual_reminder",
                new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(AgreementPanelActivity.this, "Reminder sent! ✅", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("AgreementPanel", "Failed to send reminder: " + error);
                    }
                });
    }

    // history method
    private void showAgreementHistory() {
        // This would open a new activity showing all past agreements for this match
        Toast.makeText(this, "Agreement history coming soon!", Toast.LENGTH_SHORT).show();

        // For now, we can show a simple dialog with agreement history
        agreementRepository.getAgreementsByMatch(matchId, new AgreementRepository.AgreementsCallback() {
            @Override
            public void onSuccess(List<Agreement> agreements) {
                runOnUiThread(() -> showHistoryDialog(agreements));
            }

            @Override
            public void onError(String error) {
                Log.e("AgreementPanel", "Error loading history: " + error);
            }
        });
    }

    private void showHistoryDialog(List<Agreement> agreements) {
        StringBuilder historyText = new StringBuilder();
        historyText.append("Agreement History:\n\n");

        for (Agreement agreement : agreements) {
            String statusIcon = "";
            switch (agreement.getStatus()) {
                case "active": statusIcon = "✅"; break;
                case "proposed": statusIcon = "⏳"; break;
                case "rejected": statusIcon = "❌"; break;
                case "expired": statusIcon = "📅"; break;
            }

            historyText.append(statusIcon)
                    .append(" ")
                    .append(agreement.getProposedBy().equals(userEmail) ? "You" : otherUserName)
                    .append(" - ")
                    .append(agreement.getStatus())
                    .append("\n");

            if (agreement.getCreatedAt() != null) {
                // Format date nicely
                String date = agreement.getCreatedAt().split("T")[0];
                historyText.append("   Date: ").append(date).append("\n");
            }

            historyText.append("\n");
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Agreement History")
                .setMessage(historyText.toString())
                .setPositiveButton("Close", null)
                .show();
    }
}