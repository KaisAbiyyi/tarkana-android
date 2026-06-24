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
import android.view.View;
import androidx.fragment.app.Fragment;

public class ChallengeFragment extends Fragment {

    private int selectedSessionType = -1; // 0=quick, 1=standard, 2=long
    private int selectedMode = -1; // 0=mixed, 1=number, 2=symbol, 3=deduction, 4=memory

    private View cardQuick, cardStandard, cardExtended;
    private View cardMixed, cardNumber, cardSymbol, cardDeduction, cardMemory;
    private RadioButton radioQuick, radioStandard, radioLong;
    private RadioButton radioMixed, radioNumber, radioSymbol, radioDeduction, radioMemory;
    private android.widget.Button btnChooseConfig;
    private TextView tvSummaryTitle, tvSummarySubtitle;
    private TextView tvDurationValue, tvCountValue, tvStatusValue, tvReviewValue;
    private android.widget.ProgressBar pbSteps;
    private TextView tvStepsReady;

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
            startActivity(intent);
        });

        return view;
    }

    private void selectSessionType(int type) {
        selectedSessionType = type;

        // Reset all session radios
        radioQuick.setChecked(false);
        radioStandard.setChecked(false);
        radioLong.setChecked(false);

        // Reset card backgrounds
        cardQuick.setBackgroundResource(R.drawable.bg_card_white);
        cardStandard.setBackgroundResource(R.drawable.bg_card_white);
        cardExtended.setBackgroundResource(R.drawable.bg_card_white);

        // Set selected
        RadioButton selectedRadio;
        View selectedCard;
        switch (type) {
            case 0: selectedRadio = radioQuick; selectedCard = cardQuick; break;
            case 1: selectedRadio = radioStandard; selectedCard = cardStandard; break;
            default: selectedRadio = radioLong; selectedCard = cardExtended; break;
        }
        selectedRadio.setChecked(true);
        selectedCard.setBackgroundResource(R.drawable.bg_card_yellow);

        // Re-apply padding because setting background resets padding
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_base);
        cardQuick.setPadding(padding, padding, padding, padding);
        cardStandard.setPadding(padding, padding, padding, padding);
        cardExtended.setPadding(padding, padding, padding, padding);

        // Update summary
        updateSummary();
    }

    private void selectMode(int mode) {
        selectedMode = mode;

        // Reset all mode radios
        radioMixed.setChecked(false);
        radioNumber.setChecked(false);
        radioSymbol.setChecked(false);
        radioDeduction.setChecked(false);
        radioMemory.setChecked(false);

        // Reset card backgrounds
        cardMixed.setBackgroundResource(R.drawable.bg_card_white);
        cardNumber.setBackgroundResource(R.drawable.bg_card_white);
        cardSymbol.setBackgroundResource(R.drawable.bg_card_white);
        cardDeduction.setBackgroundResource(R.drawable.bg_card_white);
        cardMemory.setBackgroundResource(R.drawable.bg_card_white);

        // Set selected
        RadioButton selectedRadio;
        View selectedCard;
        switch (mode) {
            case 0: selectedRadio = radioMixed; selectedCard = cardMixed; break;
            case 1: selectedRadio = radioNumber; selectedCard = cardNumber; break;
            case 2: selectedRadio = radioSymbol; selectedCard = cardSymbol; break;
            case 3: selectedRadio = radioDeduction; selectedCard = cardDeduction; break;
            default: selectedRadio = radioMemory; selectedCard = cardMemory; break;
        }
        selectedRadio.setChecked(true);
        selectedCard.setBackgroundResource(R.drawable.bg_card_yellow);

        // Re-apply padding because setting background resets padding
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_base);
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
            tvStepsReady.setText(getString(R.string.challenge_steps_ready, steps));
        }

        tvSummaryTitle.setText("Challenge Configuration");
        tvSummarySubtitle.setText("Step " + (steps == 3 ? 3 : steps + 1) + " of 3");

        // Duration/count based on session type
        if (selectedSessionType >= 0) {
            String[] durations = {"~3 min", "~7 min", "~15 min"};
            String[] counts = {"5", "10", "20"};
            tvDurationValue.setText(durations[selectedSessionType]);
            tvCountValue.setText(counts[selectedSessionType]);
        } else {
            tvDurationValue.setText("-");
            tvCountValue.setText("-");
        }

        // Mode status
        if (selectedMode >= 0) {
            String[] modes = {"Mixed", "Number", "Symbol", "Deduction", "Memory"};
            tvStatusValue.setText(modes[selectedMode] + " Mode");
        } else {
            tvStatusValue.setText("-");
        }

        // Enable button when both selected
        boolean ready = selectedSessionType >= 0 && selectedMode >= 0;
        btnChooseConfig.setEnabled(ready);
        if (ready) {
            btnChooseConfig.setBackgroundResource(R.drawable.bg_button_primary);
            btnChooseConfig.setText(R.string.start_challenge);
            btnChooseConfig.setTextColor(getResources().getColor(R.color.color_text_primary));
        } else {
            btnChooseConfig.setBackgroundResource(R.drawable.bg_button_disabled);
            btnChooseConfig.setText(R.string.challenge_choose_config);
            btnChooseConfig.setTextColor(getResources().getColor(R.color.color_text_secondary));
        }
    }
}
