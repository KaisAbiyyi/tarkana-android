package com.kaisabiyyistudio.tarkana_android;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SessionActivity extends AppCompatActivity {

    private View cardOptionA, cardOptionB, cardOptionC, cardOptionD;
    private Button btnSubmit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        cardOptionA = findViewById(R.id.card_option_a);
        cardOptionB = findViewById(R.id.card_option_b);
        cardOptionC = findViewById(R.id.card_option_c);
        cardOptionD = findViewById(R.id.card_option_d);
        btnSubmit = findViewById(R.id.btn_submit);

        View.OnClickListener optionListener = v -> {
            // Reset all
            cardOptionA.setBackgroundResource(R.drawable.bg_card_white);
            cardOptionB.setBackgroundResource(R.drawable.bg_card_white);
            cardOptionC.setBackgroundResource(R.drawable.bg_card_white);
            cardOptionD.setBackgroundResource(R.drawable.bg_card_white);

            // Select
            v.setBackgroundResource(R.drawable.bg_card_yellow);

            // Enable submit
            btnSubmit.setEnabled(true);
            btnSubmit.setBackgroundResource(R.drawable.bg_button_primary);
            btnSubmit.setTextColor(Color.BLACK);
        };

        cardOptionA.setOnClickListener(optionListener);
        cardOptionB.setOnClickListener(optionListener);
        cardOptionC.setOnClickListener(optionListener);
        cardOptionD.setOnClickListener(optionListener);

        btnSubmit.setOnClickListener(v -> finish());
    }
}
