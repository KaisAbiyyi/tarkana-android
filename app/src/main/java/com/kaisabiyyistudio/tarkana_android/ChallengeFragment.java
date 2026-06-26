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
    
    private View ivCheckQuick, ivCheckStandard, ivCheckExtended;
    private View ivCheckMixed, ivCheckNumber, ivCheckSymbol, ivCheckDeduction, ivCheckMemory;
    
    private TextView tvLoadoutPreview;
    
    private AppCompatButton btnChooseConfig;
    private android.widget.ProgressBar pbSteps;
    private TextView tvStepsReady;
    private TextView tvStickySelection;

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

        // Check icons
        ivCheckQuick = view.findViewById(R.id.iv_check_quick);
        ivCheckStandard = view.findViewById(R.id.iv_check_standard);
        ivCheckExtended = view.findViewById(R.id.iv_check_extended);
        ivCheckMixed = view.findViewById(R.id.iv_check_mixed);
        ivCheckNumber = view.findViewById(R.id.iv_check_number);
        ivCheckSymbol = view.findViewById(R.id.iv_check_symbol);
        ivCheckDeduction = view.findViewById(R.id.iv_check_deduction);
        ivCheckMemory = view.findViewById(R.id.iv_check_memory);

        // Loadout preview text
        tvLoadoutPreview = view.findViewById(R.id.tv_loadout_preview);

        btnChooseConfig = view.findViewById(R.id.btn_choose_config);
        
        // Sticky bottom selection info
        tvStickySelection = view.findViewById(R.id.tv_sticky_selection);
        
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
            
            String cType = "standard";
            if (selectedSessionType == 0) cType = "quick";
            else if (selectedSessionType == 2) cType = "long";

            String qType = "mixed_mode";
            if (selectedMode == 1) qType = "number_patterns";
            else if (selectedMode == 2) qType = "symbol_patterns";
            else if (selectedMode == 3) qType = "mini_deduction";
            else if (selectedMode == 4) qType = "pattern_memory";

            intent.putExtra("challengeType", cType);
            intent.putExtra("selectedMode", qType);
            startActivity(intent);
        });

        // Set safe padding listener on bottom bar to respect mobile safe area
        View bottomStickyBar = view.findViewById(R.id.bottom_sticky_bar);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomStickyBar, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                v.getPaddingLeft(),
                v.getPaddingTop(),
                v.getPaddingRight(),
                systemBars.bottom + getResources().getDimensionPixelSize(R.dimen.spacing_sm)
            );
            return insets;
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

        // Reset check visibilities to INVISIBLE (for layout stability)
        if (ivCheckQuick != null) ivCheckQuick.setVisibility(View.INVISIBLE);
        if (ivCheckStandard != null) ivCheckStandard.setVisibility(View.INVISIBLE);
        if (ivCheckExtended != null) ivCheckExtended.setVisibility(View.INVISIBLE);

        // Reset card backgrounds
        cardQuick.setBackgroundResource(R.drawable.bg_card_white);
        cardStandard.setBackgroundResource(R.drawable.bg_card_white);
        cardExtended.setBackgroundResource(R.drawable.bg_card_white);

        // Set selected
        RadioButton selectedRadio = null;
        View selectedCard;
        View selectedCheck;
        switch (type) {
            case 0: selectedRadio = radioQuick; selectedCard = cardQuick; selectedCheck = ivCheckQuick; break;
            case 1: selectedRadio = radioStandard; selectedCard = cardStandard; selectedCheck = ivCheckStandard; break;
            default: selectedRadio = radioLong; selectedCard = cardExtended; selectedCheck = ivCheckExtended; break;
        }
        if (selectedRadio != null) selectedRadio.setChecked(true);
        selectedCard.setBackgroundResource(R.drawable.bg_card_teal);
        if (selectedCheck != null) selectedCheck.setVisibility(View.VISIBLE);

        // Re-apply padding spacing_md (12dp) to match XML, preventing card resizing
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_md);
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

        // Reset check visibilities to INVISIBLE (for layout stability)
        if (ivCheckMixed != null) ivCheckMixed.setVisibility(View.INVISIBLE);
        if (ivCheckNumber != null) ivCheckNumber.setVisibility(View.INVISIBLE);
        if (ivCheckSymbol != null) ivCheckSymbol.setVisibility(View.INVISIBLE);
        if (ivCheckDeduction != null) ivCheckDeduction.setVisibility(View.INVISIBLE);
        if (ivCheckMemory != null) ivCheckMemory.setVisibility(View.INVISIBLE);

        // Reset card backgrounds
        cardMixed.setBackgroundResource(R.drawable.bg_card_white);
        cardNumber.setBackgroundResource(R.drawable.bg_card_white);
        cardSymbol.setBackgroundResource(R.drawable.bg_card_white);
        cardDeduction.setBackgroundResource(R.drawable.bg_card_white);
        cardMemory.setBackgroundResource(R.drawable.bg_card_white);

        // Set selected
        RadioButton selectedRadio = null;
        View selectedCard;
        View selectedCheck;
        switch (mode) {
            case 0: selectedRadio = radioMixed; selectedCard = cardMixed; selectedCheck = ivCheckMixed; break;
            case 1: selectedRadio = radioNumber; selectedCard = cardNumber; selectedCheck = ivCheckNumber; break;
            case 2: selectedRadio = radioSymbol; selectedCard = cardSymbol; selectedCheck = ivCheckSymbol; break;
            case 3: selectedRadio = radioDeduction; selectedCard = cardDeduction; selectedCheck = ivCheckDeduction; break;
            default: selectedRadio = radioMemory; selectedCard = cardMemory; selectedCheck = ivCheckMemory; break;
        }
        if (selectedRadio != null) selectedRadio.setChecked(true);
        selectedCard.setBackgroundResource(R.drawable.bg_card_teal);
        if (selectedCheck != null) selectedCheck.setVisibility(View.VISIBLE);

        // Re-apply padding spacing_md (12dp) to match XML, preventing card resizing
        int padding = getResources().getDimensionPixelSize(R.dimen.spacing_md);
        cardMixed.setPadding(padding, padding, padding, padding);
        cardNumber.setPadding(padding, padding, padding, padding);
        cardSymbol.setPadding(padding, padding, padding, padding);
        cardDeduction.setPadding(padding, padding, padding, padding);
        cardMemory.setPadding(padding, padding, padding, padding);

        updateSummary();
    }

    private void updateSummary() {
        // Step progress
        int progress = 0;
        if (selectedSessionType >= 0) progress += 50;
        if (selectedMode >= 0) progress += 50;
        
        if (pbSteps != null) {
            pbSteps.setProgress(progress);
        }
        
        boolean hasSession = selectedSessionType >= 0;
        boolean hasMode = selectedMode >= 0;
        boolean eitherSelected = hasSession || hasMode;
        boolean ready = hasSession && hasMode;

        if (tvStepsReady != null) {
            if (ready) {
                tvStepsReady.setText("Ready");
            } else if (eitherSelected) {
                tvStepsReady.setText("1/2 selected");
            } else {
                tvStepsReady.setText("0/2 selected");
            }
        }

        // Selected Loadout Preview TextView - Always visible, stable size
        if (tvLoadoutPreview != null) {
            if (ready) {
                String[] sessions = {"Quick", "Standard", "Long"};
                String[] modes = {"Mixed Mode", "Number", "Symbol", "Deduction", "Memory"};
                String[] info = {"5Q · ~1 min", "10Q · ~2 min", "20Q · ~4 min"};
                tvLoadoutPreview.setText("Ready: " + sessions[selectedSessionType] + " · " + modes[selectedMode] + " · " + info[selectedSessionType]);
            } else if (hasSession) {
                String[] sessions = {"Quick", "Standard", "Long"};
                tvLoadoutPreview.setText("Selected: " + sessions[selectedSessionType]);
            } else if (hasMode) {
                String[] modes = {"Mixed Mode", "Number", "Symbol", "Deduction", "Memory"};
                tvLoadoutPreview.setText("Selected: " + modes[selectedMode]);
            } else {
                tvLoadoutPreview.setText("Choose a session and mode");
            }
        }

        // Sticky bottom loadout text
        if (tvStickySelection != null) {
            if (ready) {
                String[] sessions = {"Quick", "Standard", "Long"};
                String[] modes = {"Mixed Mode", "Number", "Symbol", "Deduction", "Memory"};
                tvStickySelection.setText(sessions[selectedSessionType] + " · " + modes[selectedMode]);
            } else if (eitherSelected) {
                tvStickySelection.setText("Choose 1 more option");
            } else {
                tvStickySelection.setText("Choose 2 options");
            }
        }

        // Enable button when both selected and set appropriate button text
        if (btnChooseConfig != null) {
            btnChooseConfig.setEnabled(ready);
            if (ready) {
                btnChooseConfig.setBackgroundResource(R.drawable.bg_button_primary);
                String btnText = "Start Challenge";
                switch (selectedMode) {
                    case 0: btnText = "Start Mixed Mode"; break;
                    case 1: btnText = "Start Number"; break;
                    case 2: btnText = "Start Symbol"; break;
                    case 3: btnText = "Start Deduction"; break;
                    case 4: btnText = "Start Memory"; break;
                }
                btnChooseConfig.setText(btnText);
                btnChooseConfig.setTextColor(getResources().getColor(R.color.color_text_primary));
            } else {
                btnChooseConfig.setBackgroundResource(R.drawable.bg_button_disabled);
                if (eitherSelected) {
                    btnChooseConfig.setText("Choose 1 more option");
                } else {
                    btnChooseConfig.setText("Choose session and mode");
                }
                btnChooseConfig.setTextColor(getResources().getColor(R.color.color_text_secondary));
            }
        }
    }
}
