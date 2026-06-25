package com.kaisabiyyistudio.tarkana_android;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class SessionActivity extends AppCompatActivity {

    private static class Question {
        String prompt;
        String[] options;
        int correctIndex;
        String explanation;

        Question(String prompt, String[] options, int correctIndex, String explanation) {
            this.prompt = prompt;
            this.options = options;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }
    }

    private final Question[] questions = {
        new Question("1, 11, 21, 31, 41, ?", new String[]{"42", "51", "61", "71"}, 1, "The sequence increases by 10 at each step: 41 + 10 = 51."),
        new Question("2, 4, 8, 16, 32, ?", new String[]{"48", "64", "128", "50"}, 1, "Each number is multiplied by 2: 32 * 2 = 64."),
        new Question("3, 6, 12, 24, 48, ?", new String[]{"96", "60", "72", "84"}, 0, "The sequence doubles at each step: 48 * 2 = 96."),
        new Question("5, 10, 15, 20, 25, ?", new String[]{"26", "30", "35", "40"}, 1, "The sequence increases by 5 at each step: 25 + 5 = 30."),
        new Question("1, 2, 4, 7, 11, ?", new String[]{"12", "15", "16", "22"}, 2, "The difference increases by 1 each time: +1, +2, +3, +4, +5. 11 + 5 = 16.")
    };

    private int currentQuestionIndex = 0;
    private int currentSelection = -1;
    private boolean isSubmitted = false;
    private int score = 0;
    private int streak = 0;

    private final int[] userAnswers = new int[5];
    private final int[] questionTimes = new int[5];

    private View layoutGameplay, layoutFinishing, layoutResultReview;
    private View btnBack;
    private TextView tvGameTimer;
    private TextView tvProgressText, tvStreakText, tvScoreText;
    private ProgressBar pbGameplaySteps;
    private TextView tvQuestionLabel, tvQuestionPrompt;
    private View cardOptionA, cardOptionB, cardOptionC, cardOptionD;
    private TextView tvOptionAVal, tvOptionBVal, tvOptionCVal, tvOptionDVal;
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

        // Gameplay views
        layoutGameplay = findViewById(R.id.layout_gameplay);
        layoutFinishing = findViewById(R.id.layout_finishing);
        layoutResultReview = findViewById(R.id.layout_result_review);

        btnBack = findViewById(R.id.btn_back);
        tvGameTimer = findViewById(R.id.tv_game_timer);
        tvProgressText = findViewById(R.id.tv_progress_text);
        tvStreakText = findViewById(R.id.tv_streak_text);
        tvScoreText = findViewById(R.id.tv_score_text);
        pbGameplaySteps = findViewById(R.id.pb_gameplay_steps);
        tvQuestionLabel = findViewById(R.id.tv_question_label);
        tvQuestionPrompt = findViewById(R.id.tv_question_prompt);

        cardOptionA = findViewById(R.id.card_option_a);
        cardOptionB = findViewById(R.id.card_option_b);
        cardOptionC = findViewById(R.id.card_option_c);
        cardOptionD = findViewById(R.id.card_option_d);

        tvOptionAVal = findViewById(R.id.tv_option_a_val);
        tvOptionBVal = findViewById(R.id.tv_option_b_val);
        tvOptionCVal = findViewById(R.id.tv_option_c_val);
        tvOptionDVal = findViewById(R.id.tv_option_d_val);

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
        tvQuestionPrompt.setText(q.prompt);
        tvOptionAVal.setText(q.options[0]);
        tvOptionBVal.setText(q.options[1]);
        tvOptionCVal.setText(q.options[2]);
        tvOptionDVal.setText(q.options[3]);

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

        // Simulate short submit network delay (State C: Submitting state)
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

        // Transition from finishing overlay to result review (State D: Finishing state)
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
            badgeMode.setText("Number Patterns");
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

            // Prompt text
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

            // Your Answer
            TextView tvUserAns = new TextView(this);
            String userLetter = userAns >= 0 ? String.valueOf((char)('A' + userAns)) : "-";
            String userVal = userAns >= 0 ? q.options[userAns] : "-";
            tvUserAns.setText("Your Answer: " + userLetter + " (" + userVal + ")");
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

            // Correct Answer
            TextView tvCorrectAns = new TextView(this);
            String correctLetter = String.valueOf((char)('A' + q.correctIndex));
            String correctVal = q.options[q.correctIndex];
            tvCorrectAns.setText("Correct Answer: " + correctLetter + " (" + correctVal + ")");
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

            // Metrics (Score and time spent)
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
