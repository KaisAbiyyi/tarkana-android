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
import java.util.Random;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

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
        String modeTag; // "number_patterns", "symbol_patterns", "mini_deduction"

        // Number mode constructor
        Question(String prompt, String[] options, int correctIndex, String explanation) {
            this(prompt, options, correctIndex, explanation, "number_patterns");
        }

        // Custom mode constructor (e.g. Mini Deduction)
        Question(String prompt, String[] options, int correctIndex, String explanation, String modeTag) {
            this.isSymbol = false;
            this.prompt = prompt;
            this.options = options;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
            this.modeTag = modeTag;
        }

        // Symbol mode constructor with explicit modeTag
        Question(String prompt, int[] seqDrawables, int[] seqBackgrounds, int[] optionDrawables, int[] optionBackgrounds, int correctIndex, String explanation, String modeTag) {
            this.isSymbol = true;
            this.prompt = prompt;
            this.seqDrawables = seqDrawables;
            this.seqBackgrounds = seqBackgrounds;
            this.optionDrawables = optionDrawables;
            this.optionBackgrounds = optionBackgrounds;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
            this.modeTag = modeTag;
        }

        // Symbol mode constructor (defaults to symbol_patterns)
        Question(String prompt, int[] seqDrawables, int[] seqBackgrounds, int[] optionDrawables, int[] optionBackgrounds, int correctIndex, String explanation) {
            this(prompt, seqDrawables, seqBackgrounds, optionDrawables, optionBackgrounds, correctIndex, explanation, "symbol_patterns");
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

    private final Question[] deductionQuestions = {
        new Question("Fara solved more puzzles than Ari. Ari solved more than Bima. Who solved the most?",
            new String[]{"Bima", "Fara", "Deni", "Ari"}, 1, "Fara > Ari > Bima. Therefore, Fara solved the most.", "mini_deduction"),
        new Question("If all cats are animals, and some animals are fluffy, is a cat guaranteed to be fluffy?",
            new String[]{"Yes, always", "No, not necessarily", "Only if it is a pet", "Depends on the breed"}, 1, "Some animals are fluffy, but not all animals or cats. Therefore, it is not guaranteed.", "mini_deduction"),
        new Question("X is taller than Y. Y is taller than Z. Who is the shortest?",
            new String[]{"X", "Y", "Z", "Cannot be determined"}, 2, "The order of height is X > Y > Z. Therefore, Z is the shortest.", "mini_deduction"),
        new Question("A red house is next to a blue house. A yellow house is next to the blue house but not next to the red house. What is the order of the houses?",
            new String[]{"Red, Blue, Yellow", "Red, Yellow, Blue", "Yellow, Red, Blue", "Blue, Red, Yellow"}, 0, "Since the blue house is next to the red house and yellow house, it must be in the middle: Red - Blue - Yellow.", "mini_deduction"),
        new Question("If a triangle is larger than a square, and the square is larger than a circle, which shape is the smallest?",
            new String[]{"Triangle", "Square", "Circle", "They are all equal"}, 2, "Triangle > Square > Circle. Therefore, the circle is the smallest shape.", "mini_deduction")
    };

    private final Question[] memoryQuestions = {
        new Question(
            "Memorize the sequence. What was symbol 4?",
            new int[]{COLORED_CIRCLE, COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{COLORED_CIRCLE, COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            3,
            "The sequence was Circle, Star, Diamond, Square, Circle. The 4th symbol was Square.",
            "pattern_memory"
        ),
        new Question(
            "Memorize the sequence. What was symbol 2?",
            new int[]{COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE, COLORED_STAR},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            1,
            "The sequence was Star, Diamond, Square, Circle, Star. The 2nd symbol was Diamond.",
            "pattern_memory"
        ),
        new Question(
            "Memorize the sequence. What was symbol 5?",
            new int[]{COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE, COLORED_STAR, COLORED_DIAMOND},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            1,
            "The sequence was Diamond, Square, Circle, Star, Diamond. The 5th symbol was Diamond.",
            "pattern_memory"
        ),
        new Question(
            "Memorize the sequence. What was symbol 1?",
            new int[]{COLORED_SQUARE, COLORED_CIRCLE, COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            2,
            "The sequence was Square, Circle, Star, Diamond, Square. The 1st symbol was Square.",
            "pattern_memory"
        ),
        new Question(
            "Memorize the sequence. What was symbol 3?",
            new int[]{COLORED_CIRCLE, COLORED_DIAMOND, COLORED_STAR, COLORED_SQUARE, COLORED_STAR},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            new int[]{COLORED_STAR, COLORED_DIAMOND, COLORED_SQUARE, COLORED_CIRCLE},
            new int[]{BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD, BG_WHITE_CARD},
            0,
            "The sequence was Circle, Diamond, Star, Square, Star. The 3rd symbol was Star.",
            "pattern_memory"
        )
    };

    private Question[] questions;
    private String selectedMode;
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

    private TextView tvResultScore, tvResultCorrectWrong, tvResultAccuracy, tvResultAvgTime, tvResultRatingChange, tvResultMasteryText;
    private TextView tvResultCorrectBadge, tvResultWrongBadge, tvResultRank, tvResultRankProgress, tvResultRankProgressPct;
    private ProgressBar pbResultRankProgress;
    private LinearLayout layoutReviewCardsContainer;
    private AppCompatButton btnRetry, btnFinish, btnResultRetry, btnResultLeaderboard;

    private View layoutFeedbackCard;
    private TextView tvFeedbackTitle, tvFeedbackSubtitle, tvFeedbackPoints;

    private View cardPatternMemorize, cardPatternHidden;
    private TextView tvMemorizeCountdown;

    private CountDownTimer timer;
    private CountDownTimer memorizeTimer;
    private CountDownTimer autoAdvanceTimer;
    private int secondsRemaining = 30;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        // Get Mode from Intent
        selectedMode = getIntent().getStringExtra("mode_str");
        if (selectedMode == null) {
            int m = getIntent().getIntExtra("mode", 1);
            if (m == 0) selectedMode = "mixed_mode";
            else if (m == 2) selectedMode = "symbol_patterns";
            else if (m == 3) selectedMode = "mini_deduction";
            else if (m == 4) selectedMode = "pattern_memory";
            else selectedMode = "number_patterns";
        }

        initializeQuestions();

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
        tvResultCorrectWrong = null;
        tvResultAccuracy = findViewById(R.id.tv_result_accuracy);
        tvResultAvgTime = findViewById(R.id.tv_result_avg_time);
        tvResultRatingChange = findViewById(R.id.tv_result_rating_change);
        tvResultMasteryText = findViewById(R.id.tv_result_mastery_text);
        layoutReviewCardsContainer = findViewById(R.id.layout_review_cards_container);
        btnRetry = findViewById(R.id.btn_retry);
        btnFinish = findViewById(R.id.btn_finish);

        tvResultCorrectBadge = findViewById(R.id.tv_result_correct_badge);
        tvResultWrongBadge = findViewById(R.id.tv_result_wrong_badge);
        tvResultRank = findViewById(R.id.tv_result_rank);
        tvResultRankProgress = findViewById(R.id.tv_result_rank_progress);
        tvResultRankProgressPct = findViewById(R.id.tv_result_rank_progress_pct);
        pbResultRankProgress = findViewById(R.id.pb_result_rank_progress);
        btnResultRetry = findViewById(R.id.btn_result_retry);
        btnResultLeaderboard = findViewById(R.id.btn_result_leaderboard);

        // Feedback views
        layoutFeedbackCard = findViewById(R.id.layout_feedback_card);
        tvFeedbackTitle = findViewById(R.id.tv_feedback_title);
        tvFeedbackSubtitle = findViewById(R.id.tv_feedback_subtitle);
        tvFeedbackPoints = findViewById(R.id.tv_feedback_points);

        cardPatternMemorize = findViewById(R.id.card_pattern_memorize);
        cardPatternHidden = findViewById(R.id.card_pattern_hidden);
        tvMemorizeCountdown = findViewById(R.id.tv_memorize_countdown);

        // Set Title text
        if ("symbol_patterns".equals(selectedMode)) {
            tvGameplayTitle.setText("Symbol Patterns");
        } else if ("mini_deduction".equals(selectedMode)) {
            tvGameplayTitle.setText("Mini Deduction");
        } else if ("mixed_mode".equals(selectedMode)) {
            tvGameplayTitle.setText("Mixed Mode");
        } else if ("pattern_memory".equals(selectedMode)) {
            tvGameplayTitle.setText("Pattern Memory");
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
            if (text.startsWith("Submit Answer")) {
                submitAnswer();
            } else if (text.startsWith("Next Question")) {
                if (autoAdvanceTimer != null) {
                    autoAdvanceTimer.cancel();
                }
                currentQuestionIndex++;
                setupQuestion();
            } else if (text.startsWith("Finish Round")) {
                if (autoAdvanceTimer != null) {
                    autoAdvanceTimer.cancel();
                }
                finishRound();
            }
        });

        // Result footer button click listeners
        btnRetry.setOnClickListener(v -> restartRound());
        btnFinish.setOnClickListener(v -> finish());

        if (btnResultRetry != null) {
            btnResultRetry.setOnClickListener(v -> restartRound());
        }
        if (btnResultLeaderboard != null) {
            btnResultLeaderboard.setOnClickListener(v -> finish());
        }

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
        if (layoutFeedbackCard != null) {
            layoutFeedbackCard.setVisibility(View.GONE);
        }

        // Cancel any active timers
        if (timer != null) {
            timer.cancel();
        }
        if (memorizeTimer != null) {
            memorizeTimer.cancel();
        }
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.cancel();
        }

        // Update progress and status cards
        tvProgressText.setText("Question " + (currentQuestionIndex + 1) + " of 5");
        pbGameplaySteps.setProgress(currentQuestionIndex + 1);
        tvQuestionLabel.setText("QUESTION " + (currentQuestionIndex + 1));

        Question q = questions[currentQuestionIndex];

        if ("pattern_memory".equals(selectedMode)) {
            // Memorization Phase
            tvQuestionPrompt.setText("Memorize the sequence.");
            layoutSymbolSequence.setVisibility(View.VISIBLE);

            // Populate sequence shapes
            setShapeImageAndBackground(ivSeq1, q.seqDrawables[0], q.seqBackgrounds[0], 8, 8, 12, 12);
            setShapeImageAndBackground(ivSeq2, q.seqDrawables[1], q.seqBackgrounds[1], 8, 8, 12, 12);
            setShapeImageAndBackground(ivSeq3, q.seqDrawables[2], q.seqBackgrounds[2], 8, 8, 12, 12);
            setShapeImageAndBackground(ivSeq4, q.seqDrawables[3], q.seqBackgrounds[3], 8, 8, 12, 12);
            setShapeImageAndBackground(ivSeq5, q.seqDrawables[4], q.seqBackgrounds[4], 8, 8, 12, 12);

            // Hide the "?" mark box in memory mode sequence reveal
            View tvQMarkBox = findViewById(R.id.tv_question_mark_box);
            if (tvQMarkBox != null) {
                tvQMarkBox.setVisibility(View.GONE);
            }

            if (cardPatternMemorize != null) {
                cardPatternMemorize.setVisibility(View.VISIBLE);
            }
            if (cardPatternHidden != null) {
                cardPatternHidden.setVisibility(View.GONE);
            }

            // Hide option cards during memorization phase
            cardOptionA.setVisibility(View.GONE);
            cardOptionB.setVisibility(View.GONE);
            cardOptionC.setVisibility(View.GONE);
            cardOptionD.setVisibility(View.GONE);

            // Set up option contents beforehand so they are ready for the answer phase
            tvOptionAVal.setVisibility(View.VISIBLE);
            ivOptionAVal.setVisibility(View.VISIBLE);
            tvOptionAVal.setText(getShapeName(q.optionDrawables[0]));
            setShapeImageAndBackground(ivOptionAVal, q.optionDrawables[0], q.optionBackgrounds[0], 6, 6, 10, 10);

            tvOptionBVal.setVisibility(View.VISIBLE);
            ivOptionBVal.setVisibility(View.VISIBLE);
            tvOptionBVal.setText(getShapeName(q.optionDrawables[1]));
            setShapeImageAndBackground(ivOptionBVal, q.optionDrawables[1], q.optionBackgrounds[1], 6, 6, 10, 10);

            tvOptionCVal.setVisibility(View.VISIBLE);
            ivOptionCVal.setVisibility(View.VISIBLE);
            tvOptionCVal.setText(getShapeName(q.optionDrawables[2]));
            setShapeImageAndBackground(ivOptionCVal, q.optionDrawables[2], q.optionBackgrounds[2], 6, 6, 10, 10);

            tvOptionDVal.setVisibility(View.VISIBLE);
            ivOptionDVal.setVisibility(View.VISIBLE);
            tvOptionDVal.setText(getShapeName(q.optionDrawables[3]));
            setShapeImageAndBackground(ivOptionDVal, q.optionDrawables[3], q.optionBackgrounds[3], 6, 6, 10, 10);

            // Bottom submit button disabled and labeled "Memorize now"
            btnSubmit.setEnabled(false);
            btnSubmit.setBackgroundResource(R.drawable.bg_button_disabled);
            btnSubmit.setTextColor(Color.GRAY);
            btnSubmit.setText("Memorize now");

            // Start 4-second memorization timer
            if (tvMemorizeCountdown != null) {
                tvMemorizeCountdown.setText("Pattern visible: 4s remaining");
            }
            memorizeTimer = new CountDownTimer(4000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int sec = (int) Math.ceil(millisUntilFinished / 1000.0);
                    if (tvMemorizeCountdown != null) {
                        tvMemorizeCountdown.setText("Pattern visible: " + sec + "s remaining");
                    }
                }

                @Override
                public void onFinish() {
                    enterAnswerPhase();
                }
            }.start();

        } else {
            // Ensure "?" mark box is visible in regular Symbol Mode sequence
            View tvQMarkBox = findViewById(R.id.tv_question_mark_box);
            if (tvQMarkBox != null) {
                tvQMarkBox.setVisibility(q.isSymbol ? View.VISIBLE : View.GONE);
            }

            if (cardPatternMemorize != null) {
                cardPatternMemorize.setVisibility(View.GONE);
            }
            if (cardPatternHidden != null) {
                cardPatternHidden.setVisibility(View.GONE);
            }

            cardOptionA.setVisibility(View.VISIBLE);
            cardOptionB.setVisibility(View.VISIBLE);
            cardOptionC.setVisibility(View.VISIBLE);
            cardOptionD.setVisibility(View.VISIBLE);

            if (q.isSymbol) {
                tvQuestionPrompt.setText("Find the next symbol in sequence:");
                layoutSymbolSequence.setVisibility(View.VISIBLE);

                // Populate sequence shapes
                setShapeImageAndBackground(ivSeq1, q.seqDrawables[0], q.seqBackgrounds[0], 8, 8, 12, 12);
                setShapeImageAndBackground(ivSeq2, q.seqDrawables[1], q.seqBackgrounds[1], 8, 8, 12, 12);
                setShapeImageAndBackground(ivSeq3, q.seqDrawables[2], q.seqBackgrounds[2], 8, 8, 12, 12);
                setShapeImageAndBackground(ivSeq4, q.seqDrawables[3], q.seqBackgrounds[3], 8, 8, 12, 12);
                setShapeImageAndBackground(ivSeq5, q.seqDrawables[4], q.seqBackgrounds[4], 8, 8, 12, 12);

                // Show ImageViews, hide TextViews for options
                tvOptionAVal.setVisibility(View.GONE);
                ivOptionAVal.setVisibility(View.VISIBLE);
                setShapeImageAndBackground(ivOptionAVal, q.optionDrawables[0], q.optionBackgrounds[0], 6, 6, 10, 10);

                tvOptionBVal.setVisibility(View.GONE);
                ivOptionBVal.setVisibility(View.VISIBLE);
                setShapeImageAndBackground(ivOptionBVal, q.optionDrawables[1], q.optionBackgrounds[1], 6, 6, 10, 10);

                tvOptionCVal.setVisibility(View.GONE);
                ivOptionCVal.setVisibility(View.VISIBLE);
                setShapeImageAndBackground(ivOptionCVal, q.optionDrawables[2], q.optionBackgrounds[2], 6, 6, 10, 10);

                tvOptionDVal.setVisibility(View.GONE);
                ivOptionDVal.setVisibility(View.VISIBLE);
                setShapeImageAndBackground(ivOptionDVal, q.optionDrawables[3], q.optionBackgrounds[3], 6, 6, 10, 10);
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
    }

    private void enterAnswerPhase() {
        Question q = questions[currentQuestionIndex];

        // 1. Hide the sequence
        layoutSymbolSequence.setVisibility(View.GONE);

        // 2. Hide memorize card, show hidden notice card
        if (cardPatternMemorize != null) {
            cardPatternMemorize.setVisibility(View.GONE);
        }
        if (cardPatternHidden != null) {
            cardPatternHidden.setVisibility(View.VISIBLE);
        }

        // 3. Set the prompt to the actual question prompt
        tvQuestionPrompt.setText(q.prompt);

        // 4. Show the option cards
        cardOptionA.setVisibility(View.VISIBLE);
        cardOptionB.setVisibility(View.VISIBLE);
        cardOptionC.setVisibility(View.VISIBLE);
        cardOptionD.setVisibility(View.VISIBLE);

        // 5. Submit button starts disabled
        btnSubmit.setEnabled(false);
        btnSubmit.setBackgroundResource(R.drawable.bg_button_disabled);
        btnSubmit.setTextColor(Color.GRAY);
        btnSubmit.setText("Choose an answer");

        // 6. Start the 30-second gameplay answer countdown timer
        startTimer();
    }

    private void setShapeImageAndBackground(ImageView iv, int imageRes, int bgRes, int padLeftDp, int padTopDp, int padRightDp, int padBottomDp) {
        iv.setImageResource(imageRes);
        iv.setBackgroundResource(bgRes);
        int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padLeftDp, getResources().getDisplayMetrics());
        int top = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padTopDp, getResources().getDisplayMetrics());
        int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padRightDp, getResources().getDisplayMetrics());
        int bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padBottomDp, getResources().getDisplayMetrics());
        iv.setPadding(left, top, right, bottom);
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
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.cancel();
        }

        int timeSpent = 30 - secondsRemaining;
        questionTimes[currentQuestionIndex] = timeSpent;

        // Simulate short submit network delay
        new Handler().postDelayed(() -> {
            Question q = questions[currentQuestionIndex];
            int correctIndex = q.correctIndex;
            userAnswers[currentQuestionIndex] = currentSelection;

            btnSubmit.setEnabled(true);
            btnSubmit.setBackgroundResource(R.drawable.bg_button_primary);
            btnSubmit.setTextColor(Color.BLACK);

            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());

            // Green correct answer option card
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
                // Red wrong answer option card
                View wrongCard = getOptionCard(currentSelection);
                if (wrongCard != null) {
                    wrongCard.setBackgroundResource(R.drawable.bg_card_red_bordered);
                    wrongCard.setPadding(padding, padding, padding, padding);
                }
            }

            // Determine correct answer name text
            String correctText = "";
            if (q.isSymbol) {
                correctText = getShapeName(q.optionDrawables[correctIndex]);
            } else {
                correctText = q.options[correctIndex];
            }

            if (layoutFeedbackCard != null) {
                layoutFeedbackCard.setVisibility(View.VISIBLE);
                if (currentSelection == correctIndex) {
                    layoutFeedbackCard.setBackgroundResource(R.drawable.bg_card_green_bordered);
                    tvFeedbackTitle.setText("Correct!");
                    if ("mini_deduction".equals(selectedMode)) {
                        tvFeedbackSubtitle.setText("Sharp reasoning. Keep the streak alive.");
                        tvFeedbackPoints.setText("+120 Reasoning Score");
                    } else {
                        tvFeedbackSubtitle.setText("Great job! Keep the streak alive.");
                        tvFeedbackPoints.setText("+120 Score");
                    }
                    tvFeedbackPoints.setVisibility(View.VISIBLE);
                } else {
                    layoutFeedbackCard.setBackgroundResource(R.drawable.bg_card_red_bordered);
                    tvFeedbackTitle.setText("Incorrect!");
                    String feedbackHtml = "The correct answer was <font color='#4CAF50'><b>" + correctText + "</b></font>.";
                    tvFeedbackSubtitle.setText(android.text.Html.fromHtml(feedbackHtml));
                    tvFeedbackPoints.setVisibility(View.GONE);
                }
            }

            tvScoreText.setText(String.valueOf(score));
            tvStreakText.setText(String.valueOf(streak));

            // Start 5-second auto-advance countdown timer
            final String baseBtnText = (currentQuestionIndex == 4) ? "Finish Round" : "Next Question";
            btnSubmit.setText(baseBtnText + " (5s)");

            autoAdvanceTimer = new CountDownTimer(5000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                    btnSubmit.setText(baseBtnText + " (" + secLeft + "s)");
                }

                @Override
                public void onFinish() {
                    btnSubmit.setText(baseBtnText);
                    if (currentQuestionIndex == 4) {
                        finishRound();
                    } else {
                        currentQuestionIndex++;
                        setupQuestion();
                    }
                }
            }.start();

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

        if (tvResultCorrectWrong != null) {
            tvResultCorrectWrong.setText(correctCount + " / " + wrongCount);
        }
        tvResultAccuracy.setText(accuracy + "%");
        tvResultAvgTime.setText(String.format("%.1fs", avgTime));
        tvResultRatingChange.setText("+" + (correctCount * 20));

        if (tvResultCorrectBadge != null) {
            tvResultCorrectBadge.setText(correctCount + " CORRECT");
        }
        if (tvResultWrongBadge != null) {
            tvResultWrongBadge.setText(wrongCount + " WRONG");
        }
        if (tvResultRank != null) {
            tvResultRank.setText("BRONZE MIND");
        }
        if (tvResultRankProgressPct != null) {
            int progress = 20 + correctCount * 15;
            tvResultRankProgressPct.setText(progress + "%");
            if (pbResultRankProgress != null) {
                pbResultRankProgress.setProgress(progress);
            }
            if (tvResultRankProgress != null) {
                int pointsNeeded = 1000 - score;
                if (pointsNeeded < 0) pointsNeeded = 0;
                tvResultRankProgress.setText(pointsNeeded + " points to Silver Solver");
            }
        }

        if (tvResultMasteryText != null) {
            String masteryText = "Number Patterns: Proficient (+15 XP)";
            if ("symbol_patterns".equals(selectedMode)) {
                masteryText = "Symbol Patterns: Proficient (+15 XP)";
            } else if ("mini_deduction".equals(selectedMode)) {
                masteryText = "Mini Deduction: Proficient (+15 XP)";
            } else if ("pattern_memory".equals(selectedMode)) {
                masteryText = "Pattern Memory: Proficient (+15 XP)";
            } else if ("mixed_mode".equals(selectedMode)) {
                masteryText = "Mixed Mode: Proficient (+15 XP)";
            }
            tvResultMasteryText.setText(masteryText);
        }

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
            String modeName = "Number Patterns";
            if ("symbol_patterns".equals(q.modeTag)) {
                modeName = "Symbol Patterns";
            } else if ("mini_deduction".equals(q.modeTag)) {
                modeName = "Mini Deduction";
            } else if ("pattern_memory".equals(q.modeTag)) {
                modeName = "Pattern Memory";
            }
            badgeMode.setText(modeName);
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

            // Spacer to push tvQNum to the right
            View spacer = new View(this);
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1.0f);
            spacer.setLayoutParams(spacerParams);
            header.addView(spacer);

            TextView tvQNum = new TextView(this);
            tvQNum.setText("Question " + (i + 1));
            tvQNum.setTextColor(Color.parseColor("#777777"));
            tvQNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvQNum.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams qNumParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            tvQNum.setLayoutParams(qNumParams);
            header.addView(tvQNum);

            card.addView(header);

            // Prompt / Sequence display
            if (q.isSymbol && !"pattern_memory".equals(q.modeTag)) {
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
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()),
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics())
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
                tvQuestionMark.setPadding(
                    0,
                    0,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()),
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics())
                );
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

            // Get string values for stats
            String userAnsVal = "-";
            String correctAnsVal = "";
            if (q.isSymbol) {
                userAnsVal = userAns >= 0 ? getShapeName(q.optionDrawables[userAns]) : "-";
                correctAnsVal = getShapeName(q.optionDrawables[q.correctIndex]);
            } else {
                userAnsVal = userAns >= 0 ? q.options[userAns] : "-";
                correctAnsVal = q.options[q.correctIndex];
            }

            // Build the 3-column stats row
            LinearLayout statsContainer = new LinearLayout(this);
            statsContainer.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams statsContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            statsContainerParams.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics()), 0, 0);
            statsContainer.setLayoutParams(statsContainerParams);
            statsContainer.setWeightSum(3.0f);

            // Column 1: YOUR ANSWER
            LinearLayout col1 = new LinearLayout(this);
            col1.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams col1Params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            col1.setLayoutParams(col1Params);

            TextView tvLabel1 = new TextView(this);
            tvLabel1.setText("YOUR ANSWER");
            tvLabel1.setTextColor(Color.parseColor("#777777"));
            tvLabel1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            tvLabel1.setTypeface(null, Typeface.BOLD);
            col1.addView(tvLabel1);

            TextView tvVal1 = new TextView(this);
            tvVal1.setText(userAnsVal);
            tvVal1.setTextColor(isCorrect ? Color.parseColor("#4CAF50") : Color.parseColor("#E53935"));
            tvVal1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvVal1.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams val1Params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            val1Params.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()), 0, 0);
            tvVal1.setLayoutParams(val1Params);
            col1.addView(tvVal1);

            statsContainer.addView(col1);

            // Column 2: CORRECT ANSWER
            LinearLayout col2 = new LinearLayout(this);
            col2.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams col2Params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            col2.setLayoutParams(col2Params);

            TextView tvLabel2 = new TextView(this);
            tvLabel2.setText("CORRECT ANSWER");
            tvLabel2.setTextColor(Color.parseColor("#777777"));
            tvLabel2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            tvLabel2.setTypeface(null, Typeface.BOLD);
            col2.addView(tvLabel2);

            TextView tvVal2 = new TextView(this);
            tvVal2.setText(correctAnsVal);
            tvVal2.setTextColor(Color.BLACK);
            tvVal2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvVal2.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams val2Params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            val2Params.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()), 0, 0);
            tvVal2.setLayoutParams(val2Params);
            col2.addView(tvVal2);

            statsContainer.addView(col2);

            // Column 3: SCORE / TIME
            LinearLayout col3 = new LinearLayout(this);
            col3.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams col3Params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            col3.setLayoutParams(col3Params);

            TextView tvLabel3 = new TextView(this);
            tvLabel3.setText("SCORE / TIME");
            tvLabel3.setTextColor(Color.parseColor("#777777"));
            tvLabel3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            tvLabel3.setTypeface(null, Typeface.BOLD);
            col3.addView(tvLabel3);

            TextView tvVal3 = new TextView(this);
            String pts = isCorrect ? "120 pts" : "0 pts";
            tvVal3.setText(pts + ", " + questionTimes[i] + "s");
            tvVal3.setTextColor(Color.BLACK);
            tvVal3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvVal3.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams val3Params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            val3Params.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()), 0, 0);
            tvVal3.setLayoutParams(val3Params);
            col3.addView(tvVal3);

            statsContainer.addView(col3);

            card.addView(statsContainer);

            // Explanation box
            LinearLayout explBox = new LinearLayout(this);
            explBox.setOrientation(LinearLayout.VERTICAL);
            explBox.setBackgroundResource(R.drawable.bg_border_black_cream);
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

        initializeQuestions();
        setupQuestion();
    }

    private void initializeQuestions() {
        if ("symbol_patterns".equals(selectedMode)) {
            questions = symbolQuestions;
        } else if ("mini_deduction".equals(selectedMode)) {
            questions = deductionQuestions;
        } else if ("pattern_memory".equals(selectedMode)) {
            questions = memoryQuestions;
        } else if ("mixed_mode".equals(selectedMode)) {
            Random rand = new Random();
            Question[] pool = new Question[5];
            
            // Randomly select 1 question from each of the 4 modes
            int numIdx = rand.nextInt(numberQuestions.length);
            int symIdx = rand.nextInt(symbolQuestions.length);
            int dedIdx = rand.nextInt(deductionQuestions.length);
            int memIdx = rand.nextInt(memoryQuestions.length);
            
            pool[0] = numberQuestions[numIdx];
            pool[1] = symbolQuestions[symIdx];
            pool[2] = deductionQuestions[dedIdx];
            pool[3] = memoryQuestions[memIdx];
            
            // Select a 5th question from a random category (0 to 3)
            int extraCat = rand.nextInt(4);
            int extraIdx = 0;
            switch (extraCat) {
                case 0:
                    extraIdx = rand.nextInt(numberQuestions.length - 1);
                    if (extraIdx >= numIdx) {
                        extraIdx++;
                    }
                    pool[4] = numberQuestions[extraIdx];
                    break;
                case 1:
                    extraIdx = rand.nextInt(symbolQuestions.length - 1);
                    if (extraIdx >= symIdx) {
                        extraIdx++;
                    }
                    pool[4] = symbolQuestions[extraIdx];
                    break;
                case 2:
                    extraIdx = rand.nextInt(deductionQuestions.length - 1);
                    if (extraIdx >= dedIdx) {
                        extraIdx++;
                    }
                    pool[4] = deductionQuestions[extraIdx];
                    break;
                case 3:
                    extraIdx = rand.nextInt(memoryQuestions.length - 1);
                    if (extraIdx >= memIdx) {
                        extraIdx++;
                    }
                    pool[4] = memoryQuestions[extraIdx];
                    break;
            }
            
            // Shuffle the pool
            List<Question> list = Arrays.asList(pool);
            Collections.shuffle(list, rand);
            questions = pool;
        } else {
            questions = numberQuestions;
        }
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        if (memorizeTimer != null) {
            memorizeTimer.cancel();
        }
        super.onDestroy();
    }
}
