package com.kaisabiyyistudio.tarkana_android;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class SessionActivity extends AppCompatActivity {

    private static class Question {
        boolean isSymbol;
        String prompt;
        // For Number Mode
        String[] options;
        // For Symbol Mode
        int[] seqDrawables;
        int[] seqBackgrounds;
        int[] optionDrawables;
        int[] optionBackgrounds;

        int correctIndex;
        String explanation;

        // Number mode constructor
        Question(String prompt, String[] options, int correctIndex, String explanation) {
            this.isSymbol = false;
            this.prompt = prompt;
            this.options = options;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }

        // Symbol mode constructor
        Question(String prompt, int[] seqDrawables, int[] seqBackgrounds, int[] optionDrawables, int[] optionBackgrounds, int correctIndex, String explanation) {
            this.isSymbol = true;
            this.prompt = prompt;
            this.seqDrawables = seqDrawables;
            this.seqBackgrounds = seqBackgrounds;
            this.optionDrawables = optionDrawables;
            this.optionBackgrounds = optionBackgrounds;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }
    }

    // Colored Shapes
    private static final int COLORED_CIRCLE = R.drawable.ic_shape_circle_colored;
    private static final int COLORED_STAR = R.drawable.ic_shape_star_colored;
    private static final int COLORED_DIAMOND = R.drawable.ic_shape_diamond_colored;
    private static final int COLORED_SQUARE = R.drawable.ic_shape_square_colored;

    // Arrow/Rotation shapes
    private static final int ARROW_RIGHT = R.drawable.ic_shape_arrow_right;
    private static final int ARROW_DOWN = R.drawable.ic_shape_arrow_down;
    private static final int ARROW_LEFT = R.drawable.ic_shape_arrow_left;
    private static final int ARROW_UP = R.drawable.ic_shape_arrow_up;

    // Backgrounds
    private static final int BG_WHITE_CARD = R.drawable.bg_card_white;

    private final Question[] numberQuestions = {
        new Question("1, 11, 21, 31, 41, ?", new String[]{"42", "51", "61", "71"}, 1, "The sequence increases by 10 at each step: 41 + 10 = 51."),
        new Question("2, 4, 8, 16, 32, ?", new String[]{"48", "64", "128", "50"}, 1, "Each number is multiplied by 2: 32 * 2 = 64."),
        new Question("3, 6, 12, 24, 48, ?", new String[]{"96", "60", "72", "84"}, 0, "The sequence doubles at each step: 48 * 2 = 96."),
        new Question("5, 10, 15, 20, 25, ?", new String[]{"26", "30", "35", "40"}, 1, "The sequence increases by 5 at each step: 25 + 5 = 30."),
        new Question("1, 2, 4, 7, 11, ?", new String[]{"12", "15", "16", "22"}, 2, "The difference increases by 1 each time: +1, +2, +3, +4, +5. 11 + 5 = 16.")
    };

    private final Question[] symbolQuestions = {
        new Question("Find the next symbol in the sequence:", 
            new int[]{ARROW_RIGHT, ARROW_DOWN, ARROW_LEFT, ARROW_UP, ARROW_RIGHT}, 
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            1, "The triangle rotates 90 degrees clockwise at each step: Right, Down, Left, Up. After Right, the next shape must point Down."),
        new Question("Find the next symbol in the sequence:", 
            new int[]{COLORED_CIRCLE, COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE}, 
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            0, "The sequence repeats four shapes: Circle, Star, Diamond, Square. After Circle, the next shape is the yellow Star."),
        new Question("Find the next symbol in the sequence:", 
            new int[]{ARROW_LEFT, ARROW_UP, ARROW_RIGHT, ARROW_DOWN, ARROW_LEFT}, 
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{ARROW_RIGHT, ARROW_UP, ARROW_LEFT, ARROW_DOWN},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            1, "The triangle rotates 90 degrees clockwise at each step: Left, Up, Right, Down. After Left, the next shape must point Up."),
        new Question("Find the next symbol in the sequence:", 
            new int[]{COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE, COLORED_STAR, COLORED_DIAMOND}, 
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{COLORED_SQUARE, COLORED_CIRCLE, COLORED_STAR, COLORED_DIAMOND},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            0, "The sequence repeats: Diamond, Square, Circle, Star. After Diamond, the next shape is the blue Square."),
        new Question("Find the next symbol in the sequence:", 
            new int[]{ARROW_UP, ARROW_LEFT, ARROW_DOWN, ARROW_RIGHT, ARROW_UP}, 
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{ARROW_LEFT, ARROW_RIGHT, ARROW_UP, ARROW_DOWN},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            0, "The triangle rotates 90 degrees counter-clockwise at each step: Up, Left, Down, Right. After Up, the next shape must point Left.")
    };

    private Question[] questions;
    private int currentQuestionIndex = 0;
    private int currentSelection = -1;
    private boolean isSubmitted = false;
    private int score = 0;
    private int streak = 0;

    private final int[] userAnswers = new int[5];
    private final int[] questionTimes = new int[5];

    private View layoutGameplay, layoutFinishing, layoutResultReview;
    private View btnBack;
    private TextView tvGameplayTitle;
    private TextView tvGameTimer;
    private TextView tvProgressText, tvStreakText, tvScoreText;
    private ProgressBar pbGameplaySteps;
    private TextView tvQuestionLabel, tvQuestionPrompt;
    private LinearLayout layoutSymbolSequence;
    private ImageView ivSeq1, ivSeq2, ivSeq3, ivSeq4, ivSeq5;

    private View cardOptionA, cardOptionB, cardOptionC, cardOptionD;
    private TextView tvOptionAVal, tvOptionBVal, tvOptionCVal, tvOptionDVal;
    private ImageView ivOptionAVal, ivOptionBVal, ivOptionCVal, ivOptionDVal;
    private AppCompatButton btnSubmit;
    private TextView tvHelperText;

    private TextView tvResultScore, tvResultCorrectWrong, tvResultAccuracy, tvResultAvgTime, tvResultRatingChange;
    private LinearLayout layoutReviewCardsContainer;
    private AppCompatButton btnRetry, btnFinish;

    private CountDownTimer timer;
    private int secondsRemaining = 30;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        // Get Mode from Intent
        int mode = getIntent().getIntExtra("mode", 1); // 1 = number, 2 = symbol, 0 = mixed

        if (mode == 2) {
            questions = symbolQuestions;
        } else if (mode == 0) {
            // Mixed Mode: Alternate
            questions = new Question[]{
                numberQuestions[0],
                symbolQuestions[0],
                numberQuestions[1],
                symbolQuestions[1],
                numberQuestions[2]
            };
        } else {
            questions = numberQuestions;
        }

        // Gameplay views
        layoutGameplay = findViewById(R.id.layout_gameplay);
        layoutFinishing = findViewById(R.id.layout_finishing);
        layoutResultReview = findViewById(R.id.layout_result_review);

        btnBack = findViewById(R.id.btn_back);
        tvGameplayTitle = findViewById(R.id.tv_gameplay_title);
        tvGameTimer = findViewById(R.id.tv_game_timer);
        tvProgressText = findViewById(R.id.tv_progress_text);
        tvStreakText = findViewById(R.id.tv_streak_text);
        tvScoreText = findViewById(R.id.tv_score_text);
        pbGameplaySteps = findViewById(R.id.pb_gameplay_steps);
        tvQuestionLabel = findViewById(R.id.tv_question_label);
        tvQuestionPrompt = findViewById(R.id.tv_question_prompt);

        layoutSymbolSequence = findViewById(R.id.layout_symbol_sequence);
        ivSeq1 = findViewById(R.id.iv_seq_1);
        ivSeq2 = findViewById(R.id.iv_seq_2);
        ivSeq3 = findViewById(R.id.iv_seq_3);
        ivSeq4 = findViewById(R.id.iv_seq_4);
        ivSeq5 = findViewById(R.id.iv_seq_5);

        cardOptionA = findViewById(R.id.card_option_a);
        cardOptionB = findViewById(R.id.card_option_b);
        cardOptionC = findViewById(R.id.card_option_c);
        cardOptionD = findViewById(R.id.card_option_d);

        tvOptionAVal = findViewById(R.id.tv_option_a_val);
        tvOptionBVal = findViewById(R.id.tv_option_b_val);
        tvOptionCVal = findViewById(R.id.tv_option_c_val);
        tvOptionDVal = findViewById(R.id.tv_option_d_val);

        ivOptionAVal = findViewById(R.id.iv_option_a_val);
        ivOptionBVal = findViewById(R.id.iv_option_b_val);
        ivOptionCVal = findViewById(R.id.iv_option_c_val);
        ivOptionDVal = findViewById(R.id.iv_option_d_val);

        btnSubmit = findViewById(R.id.btn_submit);
        tvHelperText = findViewById(R.id.tv_helper_text);

        // Result views
        tvResultScore = findViewById(R.id.tv_result_score);
        tvResultCorrectWrong = findViewById(R.id.tv_result_correct_wrong);
        tvResultAccuracy = findViewById(R.id.tv_result_accuracy);
        tvResultAvgTime = findViewById(R.id.tv_result_avg_time);
        tvResultRatingChange = findViewById(R.id.tv_result_rating_change);
        layoutReviewCardsContainer = findViewById(R.id.layout_review_cards_container);
        btnRetry = findViewById(R.id.btn_retry);
        btnFinish = findViewById(R.id.btn_finish);

        // Set Title text
        if (mode == 2) {
            tvGameplayTitle.setText("Symbol Patterns");
        } else if (mode == 0) {
            tvGameplayTitle.setText("Mixed Mode");
        } else {
            tvGameplayTitle.setText("Number Patterns");
        }

        // Close button
        btnBack.setOnClickListener(v -> finish());

        // Option click listeners
        cardOptionA.setOnClickListener(v -> selectOption(0));
        cardOptionB.setOnClickListener(v -> selectOption(1));
        cardOptionC.setOnClickListener(v -> selectOption(2));
        cardOptionD.setOnClickListener(v -> selectOption(3));

        // Submit action
        btnSubmit.setOnClickListener(v -> {
            String text = btnSubmit.getText().toString();
            if ("Submit Answer".equals(text)) {
                submitAnswer();
            } else if ("Next Question".equals(text)) {
                currentQuestionIndex++;
                setupQuestion();
            } else if ("Finish Round".equals(text)) {
                finishRound();
            }
        });

        // Result footer button click listeners
        btnRetry.setOnClickListener(v -> restartRound());
        btnFinish.setOnClickListener(v -> finish());

        // Initialize variables
        for (int i = 0; i < 5; i++) {
            userAnswers[i] = -1;
            questionTimes[i] = 0;
        }

        // Start gameplay
        setupQuestion();
    }

    private void setupQuestion() {
        isSubmitted = false;
        currentSelection = -1;
        resetOptionBackgrounds();

        // Update progress and status cards
        tvProgressText.setText("Question " + (currentQuestionIndex + 1) + " of 5");
        pbGameplaySteps.setProgress(currentQuestionIndex + 1);
        tvQuestionLabel.setText("QUESTION " + (currentQuestionIndex + 1));

        Question q = questions[currentQuestionIndex];

        if (q.isSymbol) {
            tvQuestionPrompt.setText("Find the next symbol in sequence:");
            layoutSymbolSequence.setVisibility(View.VISIBLE);

            // Populate sequence shapes
            ivSeq1.setImageResource(q.seqDrawables[0]);
            ivSeq1.setBackgroundResource(q.seqBackgrounds[0]);

            ivSeq2.setImageResource(q.seqDrawables[1]);
            ivSeq2.setBackgroundResource(q.seqBackgrounds[1]);

            ivSeq3.setImageResource(q.seqDrawables[2]);
            ivSeq3.setBackgroundResource(q.seqBackgrounds[2]);

            ivSeq4.setImageResource(q.seqDrawables[3]);
            ivSeq4.setBackgroundResource(q.seqBackgrounds[3]);

            ivSeq5.setImageResource(q.seqDrawables[4]);
            ivSeq5.setBackgroundResource(q.seqBackgrounds[4]);

            // Show ImageViews, hide TextViews for options
            tvOptionAVal.setVisibility(View.GONE);
            ivOptionAVal.setVisibility(View.VISIBLE);
            ivOptionAVal.setImageResource(q.optionDrawables[0]);
            ivOptionAVal.setBackgroundResource(q.optionBackgrounds[0]);

            tvOptionBVal.setVisibility(View.GONE);
            ivOptionBVal.setVisibility(View.VISIBLE);
            ivOptionBVal.setImageResource(q.optionDrawables[1]);
            ivOptionBVal.setBackgroundResource(q.optionBackgrounds[1]);

            tvOptionCVal.setVisibility(View.GONE);
            ivOptionCVal.setVisibility(View.VISIBLE);
            ivOptionCVal.setImageResource(q.optionDrawables[2]);
            ivOptionCVal.setBackgroundResource(q.optionBackgrounds[2]);

            tvOptionDVal.setVisibility(View.GONE);
            ivOptionDVal.setVisibility(View.VISIBLE);
            ivOptionDVal.setImageResource(q.optionDrawables[3]);
            ivOptionDVal.setBackgroundResource(q.optionBackgrounds[3]);
        } else {
            tvQuestionPrompt.setText(q.prompt);
            layoutSymbolSequence.setVisibility(View.GONE);

            // Show TextViews, hide ImageViews for options
            tvOptionAVal.setVisibility(View.VISIBLE);
            ivOptionAVal.setVisibility(View.GONE);
            tvOptionAVal.setText(q.options[0]);

            tvOptionBVal.setVisibility(View.VISIBLE);
            ivOptionBVal.setVisibility(View.GONE);
            tvOptionBVal.setText(q.options[1]);

            tvOptionCVal.setVisibility(View.VISIBLE);
            ivOptionCVal.setVisibility(View.GONE);
            tvOptionCVal.setText(q.options[2]);

            tvOptionDVal.setVisibility(View.VISIBLE);
            ivOptionDVal.setVisibility(View.GONE);
            tvOptionDVal.setText(q.options[3]);
        }

        // Submit button starts disabled
        btnSubmit.setEnabled(false);
        btnSubmit.setBackgroundResource(R.drawable.bg_button_disabled);
        btnSubmit.setTextColor(Color.GRAY);
        btnSubmit.setText("Choose an answer");

        // Start countdown
        startTimer();
    }

    private void selectOption(int index) {
        if (isSubmitted) return;
        currentSelection = index;

        resetOptionBackgrounds();

        View selectedView = getOptionCard(index);
        if (selectedView != null) {
            selectedView.setBackgroundResource(R.drawable.bg_card_teal);
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
            selectedView.setPadding(padding, padding, padding, padding);
        }

        btnSubmit.setEnabled(true);
        btnSubmit.setBackgroundResource(R.drawable.bg_button_primary);
        btnSubmit.setTextColor(Color.BLACK);
        btnSubmit.setText("Submit Answer");
    }

    private void resetOptionBackgrounds() {
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
        
        cardOptionA.setBackgroundResource(R.drawable.bg_card_white);
        cardOptionA.setPadding(padding, padding, padding, padding);
        
        cardOptionB.setBackgroundResource(R.drawable.bg_card_white);
        cardOptionB.setPadding(padding, padding, padding, padding);
        
        cardOptionC.setBackgroundResource(R.drawable.bg_card_white);
        cardOptionC.setPadding(padding, padding, padding, padding);
        
        cardOptionD.setBackgroundResource(R.drawable.bg_card_white);
        cardOptionD.setPadding(padding, padding, padding, padding);
    }

    private View getOptionCard(int index) {
        switch (index) {
            case 0: return cardOptionA;
            case 1: return cardOptionB;
            case 2: return cardOptionC;
            case 3: return cardOptionD;
            default: return null;
        }
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        secondsRemaining = 30;
        tvGameTimer.setText("30s");
        timer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining = (int) (millisUntilFinished / 1000);
                tvGameTimer.setText(secondsRemaining + "s");
            }

            @Override
            public void onFinish() {
                secondsRemaining = 0;
                tvGameTimer.setText("0s");
                if (currentSelection == -1) {
                    currentSelection = 0; // Auto-select A on timeout
                }
                submitAnswer();
            }
        }.start();
    }

    private void submitAnswer() {
        isSubmitted = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setBackgroundResource(R.drawable.bg_button_disabled);
        btnSubmit.setTextColor(Color.GRAY);
        btnSubmit.setText("Submitting...");

        if (timer != null) {
            timer.cancel();
        }

        int timeSpent = 30 - secondsRemaining;
        questionTimes[currentQuestionIndex] = timeSpent;

        // Simulate short submit network delay
        new Handler().postDelayed(() -> {
            int correctIndex = questions[currentQuestionIndex].correctIndex;
            userAnswers[currentQuestionIndex] = currentSelection;

            btnSubmit.setEnabled(true);
            btnSubmit.setBackgroundResource(R.drawable.bg_button_primary);
            btnSubmit.setTextColor(Color.BLACK);

            if (currentQuestionIndex == 4) {
                btnSubmit.setText("Finish Round");
            } else {
                btnSubmit.setText("Next Question");
            }

            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());

            // Green correct answer
            View correctCard = getOptionCard(correctIndex);
            if (correctCard != null) {
                correctCard.setBackgroundResource(R.drawable.bg_card_green_bordered);
                correctCard.setPadding(padding, padding, padding, padding);
            }

            if (currentSelection == correctIndex) {
                streak++;
                score += 120;
            } else {
                streak = 0;
                View wrongCard = getOptionCard(currentSelection);
                if (wrongCard != null) {
                    wrongCard.setBackgroundResource(R.drawable.bg_card_red_bordered);
                    wrongCard.setPadding(padding, padding, padding, padding);
                }
            }

            tvScoreText.setText(String.valueOf(score));
            tvStreakText.setText(String.valueOf(streak));

        }, 600);
    }

    private void finishRound() {
        if (timer != null) {
            timer.cancel();
        }

        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.VISIBLE);

        // Transition from finishing overlay to result review
        new Handler().postDelayed(this::showResultReview, 1500);
    }

    private void showResultReview() {
        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.GONE);
        layoutResultReview.setVisibility(View.VISIBLE);

        // Update score
        tvResultScore.setText(String.format("%,d", score));

        // Calculate statistics
        int correctCount = 0;
        int totalTime = 0;
        for (int i = 0; i < 5; i++) {
            if (userAnswers[i] == questions[i].correctIndex) {
                correctCount++;
            }
            totalTime += questionTimes[i];
        }
        int wrongCount = 5 - correctCount;
        int accuracy = (correctCount * 100) / 5;
        double avgTime = totalTime / 5.0;

        tvResultCorrectWrong.setText(correctCount + " / " + wrongCount);
        tvResultAccuracy.setText(accuracy + "%");
        tvResultAvgTime.setText(String.format("%.1fs", avgTime));
        tvResultRatingChange.setText("+" + (correctCount * 20));

        // Build question review items dynamically
        layoutReviewCardsContainer.removeAllViews();
        for (int i = 0; i < 5; i++) {
            Question q = questions[i];
            int userAns = userAnswers[i];
            boolean isCorrect = userAns == q.correctIndex;

            // Outer Card Container
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card_white);
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
            card.setPadding(padding, padding, padding, padding);
            
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()));
            card.setLayoutParams(cardParams);

            // Header Row (Badges + Question ID)
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(android.view.Gravity.CENTER_VERTICAL);
            
            TextView badgeStatus = new TextView(this);
            badgeStatus.setText(isCorrect ? "CORRECT" : "WRONG");
            badgeStatus.setTextColor(Color.WHITE);
            badgeStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            badgeStatus.setTypeface(null, Typeface.BOLD);
            badgeStatus.setPadding(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics())
            );
            badgeStatus.setBackgroundResource(isCorrect ? R.drawable.bg_badge_green : R.drawable.bg_chip_red);
            header.addView(badgeStatus);

            TextView badgeMode = new TextView(this);
            badgeMode.setText(q.isSymbol ? "Symbol Patterns" : "Number Patterns");
            badgeMode.setTextColor(Color.BLACK);
            badgeMode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            badgeMode.setTypeface(null, Typeface.BOLD);
            badgeMode.setPadding(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics())
            );
            badgeMode.setBackgroundResource(R.drawable.bg_badge_teal);
            LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            modeParams.setMarginStart((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
            badgeMode.setLayoutParams(modeParams);
            header.addView(badgeMode);

            TextView tvQNum = new TextView(this);
            tvQNum.setText("Q" + (i + 1));
            tvQNum.setTextColor(Color.parseColor("#777777"));
            tvQNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvQNum.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams qNumParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            qNumParams.setMarginStart((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
            tvQNum.setLayoutParams(qNumParams);
            header.addView(tvQNum);

            card.addView(header);

            // Prompt / Sequence display
            if (q.isSymbol) {
                LinearLayout seqContainer = new LinearLayout(this);
                seqContainer.setOrientation(LinearLayout.HORIZONTAL);
                seqContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams seqParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                seqParams.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()), 0, 0);
                seqContainer.setLayoutParams(seqParams);

                for (int j = 0; j < 5; j++) {
                    ImageView iv = new ImageView(this);
                    iv.setImageResource(q.seqDrawables[j]);
                    iv.setBackgroundResource(q.seqBackgrounds[j]);
                    int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, getResources().getDisplayMetrics());
                    LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(size, size);
                    if (j > 0) ivParams.setMarginStart((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
                    iv.setLayoutParams(ivParams);
                    iv.setPadding(
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics())
                    );
                    seqContainer.addView(iv);
                }

                // Question mark box
                TextView tvQuestionMark = new TextView(this);
                tvQuestionMark.setText("?");
                tvQuestionMark.setGravity(android.view.Gravity.CENTER);
                tvQuestionMark.setTextColor(Color.BLACK);
                tvQuestionMark.setTypeface(null, Typeface.BOLD);
                tvQuestionMark.setBackgroundResource(R.drawable.bg_card_blue_bordered);
                int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, getResources().getDisplayMetrics());
                LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(size, size);
                tvParams.setMarginStart((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
                tvQuestionMark.setLayoutParams(tvParams);
                seqContainer.addView(tvQuestionMark);

                card.addView(seqContainer);
            } else {
                TextView tvPrompt = new TextView(this);
                tvPrompt.setText(q.prompt);
                tvPrompt.setTextColor(Color.BLACK);
                tvPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tvPrompt.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams promptParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                promptParams.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()), 0, 0);
                tvPrompt.setLayoutParams(promptParams);
                card.addView(tvPrompt);
            }

            // User / Correct Answer Displays
            TextView tvUserAns = new TextView(this);
            TextView tvCorrectAns = new TextView(this);

            if (q.isSymbol) {
                String userLetter = userAns >= 0 ? String.valueOf((char)('A' + userAns)) : "-";
                String userShapeName = userAns >= 0 ? getShapeName(q.optionDrawables[userAns]) : "-";
                tvUserAns.setText("Your Answer: " + userLetter + " (" + userShapeName + ")");

                String correctLetter = String.valueOf((char)('A' + q.correctIndex));
                String correctShapeName = getShapeName(q.optionDrawables[q.correctIndex]);
                tvCorrectAns.setText("Correct Answer: " + correctLetter + " (" + correctShapeName + ")");
            } else {
                String userLetter = userAns >= 0 ? String.valueOf((char)('A' + userAns)) : "-";
                String userVal = userAns >= 0 ? q.options[userAns] : "-";
                tvUserAns.setText("Your Answer: " + userLetter + " (" + userVal + ")");

                String correctLetter = String.valueOf((char)('A' + q.correctIndex));
                String correctVal = q.options[q.correctIndex];
                tvCorrectAns.setText("Correct Answer: " + correctLetter + " (" + correctVal + ")");
            }

            tvUserAns.setTextColor(isCorrect ? Color.parseColor("#4CAF50") : Color.parseColor("#E53935"));
            tvUserAns.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvUserAns.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams ansParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            ansParams.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()), 0, 0);
            tvUserAns.setLayoutParams(ansParams);
            card.addView(tvUserAns);

            tvCorrectAns.setTextColor(Color.BLACK);
            tvCorrectAns.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvCorrectAns.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams correctParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            correctParams.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()), 0, 0);
            tvCorrectAns.setLayoutParams(correctParams);
            card.addView(tvCorrectAns);

            // Metrics
            TextView tvMetrics = new TextView(this);
            tvMetrics.setText("Score: " + (isCorrect ? "+120" : "0") + "  |  Time: " + questionTimes[i] + "s");
            tvMetrics.setTextColor(Color.parseColor("#555555"));
            tvMetrics.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvMetrics.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams metricsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            metricsParams.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()), 0, 0);
            tvMetrics.setLayoutParams(metricsParams);
            card.addView(tvMetrics);

            // Explanation box
            LinearLayout explBox = new LinearLayout(this);
            explBox.setOrientation(LinearLayout.VERTICAL);
            explBox.setBackgroundResource(R.drawable.bg_dashed_border);
            explBox.setPadding(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics())
            );
            LinearLayout.LayoutParams explParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            explParams.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()), 0, 0);
            explBox.setLayoutParams(explParams);

            TextView tvExplTitle = new TextView(this);
            tvExplTitle.setText("EXPLANATION");
            tvExplTitle.setTextColor(Color.parseColor("#555555"));
            tvExplTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvExplTitle.setTypeface(null, Typeface.BOLD);
            explBox.addView(tvExplTitle);

            TextView tvExplContent = new TextView(this);
            tvExplContent.setText(q.explanation);
            tvExplContent.setTextColor(Color.BLACK);
            tvExplContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            LinearLayout.LayoutParams explContentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            explContentParams.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()), 0, 0);
            tvExplContent.setLayoutParams(explContentParams);
            explBox.addView(tvExplContent);

            card.addView(explBox);

            layoutReviewCardsContainer.addView(card);
        }
    }

    private String getShapeName(int drawableId) {
        if (drawableId == COLORED_CIRCLE) return "Circle";
        if (drawableId == COLORED_STAR) return "Star";
        if (drawableId == COLORED_DIAMOND) return "Diamond";
        if (drawableId == COLORED_SQUARE) return "Square";
        if (drawableId == ARROW_RIGHT) return "Arrow Right";
        if (drawableId == ARROW_DOWN) return "Arrow Down";
        if (drawableId == ARROW_LEFT) return "Arrow Left";
        if (drawableId == ARROW_UP) return "Arrow Up";
        return "Unknown";
    }

    private void restartRound() {
        currentQuestionIndex = 0;
        score = 0;
        streak = 0;
        for (int i = 0; i < 5; i++) {
            userAnswers[i] = -1;
            questionTimes[i] = 0;
        }

        tvScoreText.setText("0");
        tvStreakText.setText("0");

        layoutGameplay.setVisibility(View.VISIBLE);
        layoutFinishing.setVisibility(View.GONE);
        layoutResultReview.setVisibility(View.GONE);

        setupQuestion();
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }
}
