package com.kaisabiyyistudio.tarkana_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

public class ChallengeFragment extends Fragment {

    private int selectedSessionType = -1; // 0=quick, 1=standard, 2=long
    private int selectedMode = -1; // 0=mixed, 1=number, 2=symbol, 3=deduction, 4=memory

    private View cardQuick, cardStandard, cardExtended;
    private View cardMixed, cardNumber, cardSymbol, cardDeduction, cardMemory;
    private RadioButton radioQuick, radioStandard, radioLong;
    private RadioButton radioMixed, radioNumber, radioSymbol, radioDeduction, radioMemory;
    private AppCompatButton btnChooseConfig;
    private TextView tvSummaryTitle, tvSummarySubtitle;
    private TextView tvDurationValue, tvCountValue, tvStatusValue, tvReviewValue;
    private android.widget.ProgressBar pbSteps;
    private TextView tvStepsReady;
    private TextView tvStickySelection;
    private View summaryContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_challenge, container, false);

        // Session type cards
        cardQuick = view.findViewById(R.id.card_quick);
        cardStandard = view.findViewById(R.id.card_standard);
        cardExtended = view.findViewById(R.id.card_extended);

        // Mode cards
        cardMixed = view.findViewById(R.id.card_mixed);
        cardNumber = view.findViewById(R.id.card_number);
        cardSymbol = view.findViewById(R.id.card_symbol);
        cardDeduction = view.findViewById(R.id.card_deduction);
        cardMemory = view.findViewById(R.id.card_memory);

        // Radio indicators
        radioQuick = view.findViewById(R.id.rb_quick);
        radioStandard = view.findViewById(R.id.rb_standard);
        radioLong = view.findViewById(R.id.rb_extended);
        radioMixed = view.findViewById(R.id.rb_mixed);
        radioNumber = view.findViewById(R.id.rb_number);
        radioSymbol = view.findViewById(R.id.rb_symbol);
        radioDeduction = view.findViewById(R.id.rb_deduction);
        radioMemory = view.findViewById(R.id.rb_memory);

        // Summary panel
        tvSummaryTitle = view.findViewById(R.id.tv_summary_title);
        tvSummarySubtitle = view.findViewById(R.id.tv_summary_subtitle);
        tvDurationValue = view.findViewById(R.id.tv_summary_duration);
        tvCountValue = view.findViewById(R.id.tv_summary_count);
        tvStatusValue = view.findViewById(R.id.tv_summary_status);
        tvReviewValue = view.findViewById(R.id.tv_summary_review);
        btnChooseConfig = view.findViewById(R.id.btn_choose_config);
        
        // Sticky bottom selection info
        tvStickySelection = view.findViewById(R.id.tv_sticky_selection);
        
        // Summary container
        summaryContainer = view.findViewById(R.id.layout_summary_container);
        
        // Step progress
        pbSteps = view.findViewById(R.id.pb_steps);
        tvStepsReady = view.findViewById(R.id.tv_steps_ready);

        btnChooseConfig.setEnabled(false);

        // Session type click listeners
        cardQuick.setOnClickListener(v -> selectSessionType(0));
        cardStandard.setOnClickListener(v -> selectSessionType(1));
        cardExtended.setOnClickListener(v -> selectSessionType(2));

        // Mode click listeners
        cardMixed.setOnClickListener(v -> selectMode(0));
        cardNumber.setOnClickListener(v -> selectMode(1));
        cardSymbol.setOnClickListener(v -> selectMode(2));
        cardDeduction.setOnClickListener(v -> selectMode(3));
        cardMemory.setOnClickListener(v -> selectMode(4));

        btnChooseConfig.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SessionActivity.class);
            intent.putExtra("mode", selectedMode);
            String modeStr = "number_patterns";
            if (selectedMode == 0) modeStr = "mixed_mode";
            else if (selectedMode == 2) modeStr = "symbol_patterns";
            else if (selectedMode == 3) modeStr = "mini_deduction";
            else if (selectedMode == 4) modeStr = "pattern_memory";
            intent.putExtra("mode_str", modeStr);
            startActivity(intent);
        });

        // Initialize display
        updateSummary();

        return view;
    }

    private void selectSessionType(int type) {
        selectedSessionType = type;

        // Reset all session radios
        if (radioQuick != null) radioQuick.setChecked(false);
        if (radioStandard != null) radioStandard.setChecked(false);
        if (radioLong != null) radioLong.setChecked(false);

        // Reset card backgrounds
        cardQuick.setBackgroundResource(R.drawable.bg_card_white);
        cardStandard.setBackgroundResource(R.drawable.bg_card_white);
        cardExtended.setBackgroundResource(R.drawable.bg_card_white);

        // Set selected
        RadioButton selectedRadio = null;
        View selectedCard;
        switch (type) {
            case 0: selectedRadio = radioQuick; selectedCard = cardQuick; break;
            case 1: selectedRadio = radioStandard; selectedCard = cardStandard; break;
            default: selectedRadio = radioLong; selectedCard = cardExtended; break;
        }
        if (selectedRadio != null) selectedRadio.setChecked(true);
        selectedCard.setBackgroundResource(R.drawable.bg_card_teal);

        // Re-apply padding because setting background resets padding
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        cardQuick.setPadding(padding, padding, padding, padding);
        cardStandard.setPadding(padding, padding, padding, padding);
        cardExtended.setPadding(padding, padding, padding, padding);

        // Update summary
        updateSummary();
    }

    private void selectMode(int mode) {
        selectedMode = mode;

        // Reset all mode radios
        if (radioMixed != null) radioMixed.setChecked(false);
        if (radioNumber != null) radioNumber.setChecked(false);
        if (radioSymbol != null) radioSymbol.setChecked(false);
        if (radioDeduction != null) radioDeduction.setChecked(false);
        if (radioMemory != null) radioMemory.setChecked(false);

        // Reset card backgrounds
        cardMixed.setBackgroundResource(R.drawable.bg_card_white);
        cardNumber.setBackgroundResource(R.drawable.bg_card_white);
        cardSymbol.setBackgroundResource(R.drawable.bg_card_white);
        cardDeduction.setBackgroundResource(R.drawable.bg_card_white);
        cardMemory.setBackgroundResource(R.drawable.bg_card_white);

        // Set selected
        RadioButton selectedRadio = null;
        View selectedCard;
        switch (mode) {
            case 0: selectedRadio = radioMixed; selectedCard = cardMixed; break;
            case 1: selectedRadio = radioNumber; selectedCard = cardNumber; break;
            case 2: selectedRadio = radioSymbol; selectedCard = cardSymbol; break;
            case 3: selectedRadio = radioDeduction; selectedCard = cardDeduction; break;
            default: selectedRadio = radioMemory; selectedCard = cardMemory; break;
        }
        if (selectedRadio != null) selectedRadio.setChecked(true);
        selectedCard.setBackgroundResource(R.drawable.bg_card_teal);

        // Re-apply padding because setting background resets padding
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        cardMixed.setPadding(padding, padding, padding, padding);
        cardNumber.setPadding(padding, padding, padding, padding);
        cardSymbol.setPadding(padding, padding, padding, padding);
        cardDeduction.setPadding(padding, padding, padding, padding);
        cardMemory.setPadding(padding, padding, padding, padding);

        updateSummary();
    }

    private void updateSummary() {
        // Step progress
        int steps = 0;
        if (selectedSessionType >= 0) steps++;
        if (selectedMode >= 0) steps++;
        if (selectedSessionType >= 0 && selectedMode >= 0) steps = 3;

        if (pbSteps != null) {
            pbSteps.setProgress(steps);
        }
        if (tvStepsReady != null) {
            if (selectedSessionType >= 0 && selectedMode >= 0) {
                tvStepsReady.setText("Ready to start");
            } else if (selectedSessionType >= 0 || selectedMode >= 0) {
                tvStepsReady.setText("1/2 selected");
            } else {
                tvStepsReady.setText("0/2 selected");
            }
        }

        if (tvSummaryTitle != null) {
            tvSummaryTitle.setText("Selected Configuration");
        }
        if (tvSummarySubtitle != null) {
            tvSummarySubtitle.setText("Step " + (steps == 3 ? 3 : steps + 1) + " of 3");
        }

        // Duration/count based on session type
        if (selectedSessionType >= 0) {
            String[] durations = {"~1 min", "~2 min", "~4 min"};
            String[] counts = {"5", "10", "20"};
            if (tvDurationValue != null) tvDurationValue.setText(durations[selectedSessionType]);
            if (tvCountValue != null) tvCountValue.setText(counts[selectedSessionType]);
        } else {
            if (tvDurationValue != null) tvDurationValue.setText("-");
            if (tvCountValue != null) tvCountValue.setText("-");
        }

        // Mode status
        if (selectedMode >= 0) {
            String[] modes = {"Mixed", "Number", "Symbol", "Deduction", "Memory"};
            if (tvStatusValue != null) tvStatusValue.setText(modes[selectedMode] + " Mode");
        } else {
            if (tvStatusValue != null) tvStatusValue.setText("-");
        }

        // Review/Rank
        if (selectedSessionType >= 0 && selectedMode >= 0) {
            if (tvReviewValue != null) tvReviewValue.setText("Ranked Arena");
        } else {
            if (tvReviewValue != null) tvReviewValue.setText("-");
        }

        // Toggle visibility of summary container
        boolean ready = selectedSessionType >= 0 && selectedMode >= 0;
        if (summaryContainer != null) {
            summaryContainer.setVisibility(ready ? View.VISIBLE : View.GONE);
        }

        // Sticky bottom loadout text
        if (tvStickySelection != null) {
            if (ready) {
                String[] sessions = {"Quick", "Standard", "Long"};
                String[] modes = {"Mixed", "Number", "Symbol", "Deduction", "Memory"};
                tvStickySelection.setText(sessions[selectedSessionType] + " + " + modes[selectedMode]);
            } else {
                tvStickySelection.setText("Choose 2 options");
            }
        }

        // Enable button when both selected
        if (btnChooseConfig != null) {
            btnChooseConfig.setEnabled(ready);
            if (ready) {
                btnChooseConfig.setBackgroundResource(R.drawable.bg_button_primary);
                if (selectedMode == 3) {
                    btnChooseConfig.setText("Start Mini Deduction");
                } else {
                    btnChooseConfig.setText("Start Challenge");
                }
                btnChooseConfig.setTextColor(getResources().getColor(R.color.color_text_primary));
            } else {
                btnChooseConfig.setBackgroundResource(R.drawable.bg_button_disabled);
                if (selectedSessionType >= 0 || selectedMode >= 0) {
                    btnChooseConfig.setText("Select 1 more option");
                } else {
                    btnChooseConfig.setText("Select session and mode");
                }
                btnChooseConfig.setTextColor(getResources().getColor(R.color.color_text_secondary));
            }
        }
    }
}
