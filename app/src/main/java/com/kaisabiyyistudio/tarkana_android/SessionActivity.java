package com.kaisabiyyistudio.tarkana_android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionActivity extends AppCompatActivity {

    private static final String TAG = "SessionActivity";

    // Symbol drawable mapping
    private static final int COLORED_CIRCLE   = R.drawable.ic_shape_circle_colored;
    private static final int COLORED_STAR     = R.drawable.ic_shape_star_colored;
    private static final int COLORED_DIAMOND  = R.drawable.ic_shape_diamond_colored;
    private static final int COLORED_SQUARE   = R.drawable.ic_shape_square_colored;
    private static final int ARROW_RIGHT = R.drawable.ic_shape_arrow_right;
    private static final int ARROW_DOWN  = R.drawable.ic_shape_arrow_down;
    private static final int ARROW_LEFT  = R.drawable.ic_shape_arrow_left;
    private static final int ARROW_UP    = R.drawable.ic_shape_arrow_up;
    private static final int BG_WHITE_CARD = R.drawable.bg_card_white;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Session state
    private String challengeType;
    private String selectedMode; // null = mixed
    private String sessionId;
    private int totalQuestions = 0;
    private int currentQuestionIndex = 0;    // local 0-based tracker
    private JSONObject currentApiQuestion;    // full JSON from API

    // Per-question tracking (review list)
    private String[] userAnswers;
    private String[] correctAnswers;
    private String[] explanations;
    private String[] questionTypes;
    private boolean[] wasCorrect;
    private int[] scoreEarned;
    private int[] timeSpent;
    private JSONArray[] memorizeSeqs; // for memory q review

    private int currentSelection = -1;
    private boolean isSubmitted = false;
    private int localScore = 0;
    private int localStreak = 0;

    // Views – gameplay
    private View layoutGameplay, layoutFinishing, layoutResultReview;
    private View layoutLoading;
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
    private View tvQMarkBox;

    // Views – memory phase
    private View cardPatternMemorize, cardPatternHidden;
    private TextView tvMemorizeCountdown;

    // Views – feedback
    private View layoutFeedbackCard;
    private TextView tvFeedbackTitle, tvFeedbackSubtitle, tvFeedbackPoints;

    // Views – result
    private TextView tvResultScore, tvResultAccuracy, tvResultAvgTime;
    private TextView tvResultRatingChange, tvResultMasteryText;
    private TextView tvResultCorrectBadge, tvResultWrongBadge;
    private TextView tvResultRank, tvResultRankProgress, tvResultRankProgressPct;
    private ProgressBar pbResultRankProgress;
    private LinearLayout layoutReviewCardsContainer;
    private AppCompatButton btnRetry, btnFinish, btnResultRetry, btnResultLeaderboard;

    private CountDownTimer timer;
    private CountDownTimer memorizeTimer;
    private CountDownTimer autoAdvanceTimer;
    private int secondsRemaining = 30;

    // Result data from finish-challenge
    private JSONObject sessionResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        challengeType = getIntent().getStringExtra("challengeType");
        selectedMode  = getIntent().getStringExtra("selectedMode");
        if (challengeType == null) challengeType = "standard";

        bindViews();
        setTitle();

        btnBack.setOnClickListener(v -> abandonAndFinish());

        cardOptionA.setOnClickListener(v -> selectOption(0));
        cardOptionB.setOnClickListener(v -> selectOption(1));
        cardOptionC.setOnClickListener(v -> selectOption(2));
        cardOptionD.setOnClickListener(v -> selectOption(3));

        btnSubmit.setOnClickListener(v -> {
            String text = btnSubmit.getText().toString();
            if (text.startsWith("Submit Answer")) {
                callSubmitAnswer();
            } else if (text.startsWith("Next Question")) {
                if (autoAdvanceTimer != null) autoAdvanceTimer.cancel();
                showNextQuestion();
            } else if (text.startsWith("Finish Round")) {
                if (autoAdvanceTimer != null) autoAdvanceTimer.cancel();
                callFinishChallenge();
            }
        });

        if (btnRetry != null) btnRetry.setOnClickListener(v -> restartSession());
        if (btnFinish != null) btnFinish.setOnClickListener(v -> finish());
        if (btnResultRetry != null) btnResultRetry.setOnClickListener(v -> restartSession());
        if (btnResultLeaderboard != null) btnResultLeaderboard.setOnClickListener(v -> finish());

        callStartChallenge();
    }

    // ─────────────── View Binding ───────────────

    private void bindViews() {
        layoutLoading     = null; // optional overlay, handle gracefully
        layoutGameplay    = findViewById(R.id.layout_gameplay);
        layoutFinishing   = findViewById(R.id.layout_finishing);
        layoutResultReview = findViewById(R.id.layout_result_review);

        btnBack         = findViewById(R.id.btn_back);
        tvGameplayTitle = findViewById(R.id.tv_gameplay_title);
        tvGameTimer     = findViewById(R.id.tv_game_timer);
        tvProgressText  = findViewById(R.id.tv_progress_text);
        tvStreakText    = findViewById(R.id.tv_streak_text);
        tvScoreText     = findViewById(R.id.tv_score_text);
        pbGameplaySteps = findViewById(R.id.pb_gameplay_steps);
        tvQuestionLabel = findViewById(R.id.tv_question_label);
        tvQuestionPrompt= findViewById(R.id.tv_question_prompt);
        tvQMarkBox      = findViewById(R.id.tv_question_mark_box);

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

        btnSubmit   = findViewById(R.id.btn_submit);
        tvHelperText= findViewById(R.id.tv_helper_text);

        layoutFeedbackCard  = findViewById(R.id.layout_feedback_card);
        tvFeedbackTitle     = findViewById(R.id.tv_feedback_title);
        tvFeedbackSubtitle  = findViewById(R.id.tv_feedback_subtitle);
        tvFeedbackPoints    = findViewById(R.id.tv_feedback_points);

        cardPatternMemorize = findViewById(R.id.card_pattern_memorize);
        cardPatternHidden   = findViewById(R.id.card_pattern_hidden);
        tvMemorizeCountdown = findViewById(R.id.tv_memorize_countdown);

        tvResultScore       = findViewById(R.id.tv_result_score);
        tvResultAccuracy    = findViewById(R.id.tv_result_accuracy);
        tvResultAvgTime     = findViewById(R.id.tv_result_avg_time);
        tvResultRatingChange= findViewById(R.id.tv_result_rating_change);
        tvResultMasteryText = findViewById(R.id.tv_result_mastery_text);
        tvResultCorrectBadge= findViewById(R.id.tv_result_correct_badge);
        tvResultWrongBadge  = findViewById(R.id.tv_result_wrong_badge);
        tvResultRank        = findViewById(R.id.tv_result_rank);
        tvResultRankProgress= findViewById(R.id.tv_result_rank_progress);
        tvResultRankProgressPct = findViewById(R.id.tv_result_rank_progress_pct);
        pbResultRankProgress= findViewById(R.id.pb_result_rank_progress);
        layoutReviewCardsContainer = findViewById(R.id.layout_review_cards_container);

        btnRetry            = findViewById(R.id.btn_retry);
        btnFinish           = findViewById(R.id.btn_finish);
        btnResultRetry      = findViewById(R.id.btn_result_retry);
        btnResultLeaderboard= findViewById(R.id.btn_result_leaderboard);
    }

    private void setTitle() {
        if (tvGameplayTitle == null) return;
        if (selectedMode == null || "mixed_mode".equals(selectedMode)) {
            tvGameplayTitle.setText(challengeType.substring(0,1).toUpperCase()
                    + challengeType.substring(1) + " Mode");
            return;
        }
        switch (selectedMode) {
            case "symbol_patterns":
            case "symbol_pattern":
                tvGameplayTitle.setText("Symbol Patterns");
                break;
            case "mini_deduction":
                tvGameplayTitle.setText("Mini Deduction");
                break;
            case "pattern_memory":
            case "memory_pattern":
                tvGameplayTitle.setText("Pattern Memory");
                break;
            default:
                tvGameplayTitle.setText("Number Patterns");
                break;
        }
    }

    // ─────────────── API: start-challenge ───────────────

    private void callStartChallenge() {
        // Show loading state
        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.GONE);
        layoutResultReview.setVisibility(View.GONE);
        btnSubmit.setEnabled(false);
        btnSubmit.setBackgroundResource(R.drawable.bg_button_disabled);
        btnSubmit.setTextColor(Color.GRAY);
        btnSubmit.setText("Loading…");

        executor.execute(() -> {
            try {
                String token = getToken();
                if (token == null) { handler.post(this::finish); return; }

                JSONObject body = new JSONObject();
                body.put("challengeType", challengeType);
                if (selectedMode != null) {
                    String apiMode = selectedMode;
                    if ("number_patterns".equals(selectedMode)) apiMode = "number_sequence";
                    else if ("symbol_patterns".equals(selectedMode)) apiMode = "symbol_pattern";
                    else if ("pattern_memory".equals(selectedMode)) apiMode = "memory_pattern";
                    else if ("mixed_mode".equals(selectedMode)) apiMode = null;
                    
                    if (apiMode != null) {
                        body.put("selectedMode", apiMode);
                    }
                }

                URL url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/start-challenge");
                HttpURLConnection conn = openPostConn(url, token);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes());
                    os.flush();
                }

                int code = conn.getResponseCode();
                String resp = readStream(code == 200 ? conn.getInputStream() : conn.getErrorStream());
                conn.disconnect();

                if (code == 200) {
                    JSONObject json = new JSONObject(resp);
                    sessionId      = json.getString("sessionId");
                    totalQuestions = json.getInt("totalQuestions");
                    JSONObject q   = json.getJSONObject("currentQuestion");

                    // Allocate tracking arrays
                    userAnswers  = new String[totalQuestions];
                    correctAnswers = new String[totalQuestions];
                    explanations = new String[totalQuestions];
                    questionTypes= new String[totalQuestions];
                    wasCorrect   = new boolean[totalQuestions];
                    scoreEarned  = new int[totalQuestions];
                    timeSpent    = new int[totalQuestions];
                    memorizeSeqs = new JSONArray[totalQuestions];

                    currentApiQuestion = q;
                    currentQuestionIndex = 0;
                    handler.post(() -> {
                        layoutGameplay.setVisibility(View.VISIBLE);
                        renderQuestion();
                    });
                } else if (code == 401) {
                    // Try refresh first
                    MasterKey mk2 = new MasterKey.Builder(this)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
                    SharedPreferences prefs2 = EncryptedSharedPreferences.create(
                            this, "auth_prefs", mk2,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                    String newToken = refreshToken(prefs2);
                    if (newToken != null) {
                        // Token refreshed — retry once
                        handler.post(this::callStartChallenge);
                    } else {
                        // No refresh token available — session may be expired, show error but stay on app
                        Log.e(TAG, "start-challenge 401, no refresh token available. resp=" + resp);
                        handler.post(() -> {
                            Toast.makeText(this, "Session expired. Please log out and log in again.", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                } else {
                    Log.e(TAG, "start-challenge error " + code + ": " + resp);
                    handler.post(() -> {
                        Toast.makeText(this, "Failed to start session (" + code + "): " + resp, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "callStartChallenge", e);
                handler.post(this::finish);
            }
        });
    }

    // ─────────────── Render Current Question ───────────────

    private void renderQuestion() {
        isSubmitted = false;
        currentSelection = -1;
        resetOptionBackgrounds();
        if (layoutFeedbackCard != null) layoutFeedbackCard.setVisibility(View.GONE);
        cancelTimers();

        String questionType = currentApiQuestion.optString("questionType", "number_sequence");
        int qIndex = currentApiQuestion.optInt("orderIndex", currentQuestionIndex);

        tvProgressText.setText("Question " + (qIndex + 1) + " of " + totalQuestions);
        pbGameplaySteps.setMax(totalQuestions);
        pbGameplaySteps.setProgress(qIndex + 1);
        tvQuestionLabel.setText("QUESTION " + (qIndex + 1));

        int timeLimitSecs = currentApiQuestion.optInt("timeLimitSeconds", 30);
        JSONArray choices  = currentApiQuestion.optJSONArray("choices");
        String prompt      = currentApiQuestion.optString("prompt", "");
        JSONObject metadata= currentApiQuestion.optJSONObject("metadata");

        boolean isSymbol = questionType.equals("symbol_pattern");
        boolean isMemory = questionType.equals("memory_pattern");

        if (isMemory && metadata != null) {
            renderMemoryQuestion(prompt, choices, metadata, timeLimitSecs);
        } else if (isSymbol) {
            renderSymbolQuestion(prompt, choices, timeLimitSecs);
        } else {
            renderTextQuestion(prompt, choices, timeLimitSecs);
        }
    }

    private void renderTextQuestion(String prompt, JSONArray choices, int timeLimitSecs) {
        if (tvQMarkBox != null) tvQMarkBox.setVisibility(View.GONE);
        if (cardPatternMemorize != null) cardPatternMemorize.setVisibility(View.GONE);
        if (cardPatternHidden != null)   cardPatternHidden.setVisibility(View.GONE);

        tvQuestionPrompt.setText(prompt);
        layoutSymbolSequence.setVisibility(View.GONE);

        cardOptionA.setVisibility(View.VISIBLE);
        cardOptionB.setVisibility(View.VISIBLE);
        cardOptionC.setVisibility(View.VISIBLE);
        cardOptionD.setVisibility(View.VISIBLE);

        // Text options
        tvOptionAVal.setVisibility(View.VISIBLE); ivOptionAVal.setVisibility(View.GONE);
        tvOptionBVal.setVisibility(View.VISIBLE); ivOptionBVal.setVisibility(View.GONE);
        tvOptionCVal.setVisibility(View.VISIBLE); ivOptionCVal.setVisibility(View.GONE);
        tvOptionDVal.setVisibility(View.VISIBLE); ivOptionDVal.setVisibility(View.GONE);

        if (choices != null && choices.length() >= 4) {
            tvOptionAVal.setText(choices.optString(0));
            tvOptionBVal.setText(choices.optString(1));
            tvOptionCVal.setText(choices.optString(2));
            tvOptionDVal.setText(choices.optString(3));
        }

        setBtnDisabled("Choose an answer");
        startTimer(timeLimitSecs);
    }

    private void renderSymbolQuestion(String prompt, JSONArray choices, int timeLimitSecs) {
        if (tvQMarkBox != null) tvQMarkBox.setVisibility(View.VISIBLE);
        if (cardPatternMemorize != null) cardPatternMemorize.setVisibility(View.GONE);
        if (cardPatternHidden != null)   cardPatternHidden.setVisibility(View.GONE);

        tvQuestionPrompt.setText("Find the next symbol in the sequence:");
        layoutSymbolSequence.setVisibility(View.VISIBLE);

        // Parse sequence from prompt: "A | B | C | D | E | ?"
        String[] seqTokens = prompt.replace(" | ?", "").split(" \\| ");
        ImageView[] seqViews = {ivSeq1, ivSeq2, ivSeq3, ivSeq4, ivSeq5};
        for (int i = 0; i < seqViews.length; i++) {
            String token = (i < seqTokens.length) ? seqTokens[i].trim() : "";
            setSymbolImage(seqViews[i], token);
            seqViews[i].setVisibility(View.VISIBLE);
        }

        cardOptionA.setVisibility(View.VISIBLE);
        cardOptionB.setVisibility(View.VISIBLE);
        cardOptionC.setVisibility(View.VISIBLE);
        cardOptionD.setVisibility(View.VISIBLE);

        tvOptionAVal.setVisibility(View.GONE); ivOptionAVal.setVisibility(View.VISIBLE);
        tvOptionBVal.setVisibility(View.GONE); ivOptionBVal.setVisibility(View.VISIBLE);
        tvOptionCVal.setVisibility(View.GONE); ivOptionCVal.setVisibility(View.VISIBLE);
        tvOptionDVal.setVisibility(View.GONE); ivOptionDVal.setVisibility(View.VISIBLE);

        if (choices != null && choices.length() >= 4) {
            setSymbolImage(ivOptionAVal, choices.optString(0));
            setSymbolImage(ivOptionBVal, choices.optString(1));
            setSymbolImage(ivOptionCVal, choices.optString(2));
            setSymbolImage(ivOptionDVal, choices.optString(3));
        }

        setBtnDisabled("Choose an answer");
        startTimer(timeLimitSecs);
    }

    private void renderMemoryQuestion(String prompt, JSONArray choices, JSONObject metadata, int timeLimitSecs) {
        if (tvQMarkBox != null) tvQMarkBox.setVisibility(View.GONE);
        if (cardPatternHidden != null) cardPatternHidden.setVisibility(View.GONE);
        if (cardPatternMemorize != null) cardPatternMemorize.setVisibility(View.VISIBLE);

        tvQuestionPrompt.setText("Memorize the sequence.");
        layoutSymbolSequence.setVisibility(View.VISIBLE);

        JSONArray memorize = metadata.optJSONArray("memorize");
        int revealSecs = metadata.optInt("revealSeconds", 4);

        // Store memorize sequence for this question's review later
        memorizeSeqs[currentQuestionIndex] = memorize;

        // Populate sequence images
        ImageView[] seqViews = {ivSeq1, ivSeq2, ivSeq3, ivSeq4, ivSeq5};
        if (memorize != null) {
            for (int i = 0; i < seqViews.length; i++) {
                if (i < memorize.length()) {
                    setSymbolImage(seqViews[i], memorize.optString(i));
                    seqViews[i].setVisibility(View.VISIBLE);
                } else {
                    seqViews[i].setVisibility(View.GONE);
                }
            }
        }

        // Prepare choices but hide option cards during memorize phase
        cardOptionA.setVisibility(View.GONE);
        cardOptionB.setVisibility(View.GONE);
        cardOptionC.setVisibility(View.GONE);
        cardOptionD.setVisibility(View.GONE);

        boolean isSymbolChoice = choices != null && choices.length() > 0
                && isSymbolToken(choices.optString(0));

        if (isSymbolChoice) {
            tvOptionAVal.setVisibility(View.VISIBLE); ivOptionAVal.setVisibility(View.VISIBLE);
            tvOptionBVal.setVisibility(View.VISIBLE); ivOptionBVal.setVisibility(View.VISIBLE);
            tvOptionCVal.setVisibility(View.VISIBLE); ivOptionCVal.setVisibility(View.VISIBLE);
            tvOptionDVal.setVisibility(View.VISIBLE); ivOptionDVal.setVisibility(View.VISIBLE);
            if (choices != null && choices.length() >= 4) {
                tvOptionAVal.setText(capitalize(choices.optString(0)));
                setSymbolImage(ivOptionAVal, choices.optString(0));
                tvOptionBVal.setText(capitalize(choices.optString(1)));
                setSymbolImage(ivOptionBVal, choices.optString(1));
                tvOptionCVal.setText(capitalize(choices.optString(2)));
                setSymbolImage(ivOptionCVal, choices.optString(2));
                tvOptionDVal.setText(capitalize(choices.optString(3)));
                setSymbolImage(ivOptionDVal, choices.optString(3));
            }
        } else {
            tvOptionAVal.setVisibility(View.VISIBLE); ivOptionAVal.setVisibility(View.GONE);
            tvOptionBVal.setVisibility(View.VISIBLE); ivOptionBVal.setVisibility(View.GONE);
            tvOptionCVal.setVisibility(View.VISIBLE); ivOptionCVal.setVisibility(View.GONE);
            tvOptionDVal.setVisibility(View.VISIBLE); ivOptionDVal.setVisibility(View.GONE);
            if (choices != null && choices.length() >= 4) {
                tvOptionAVal.setText(choices.optString(0));
                tvOptionBVal.setText(choices.optString(1));
                tvOptionCVal.setText(choices.optString(2));
                tvOptionDVal.setText(choices.optString(3));
            }
        }

        setBtnDisabled("Memorizing…");

        if (tvMemorizeCountdown != null) {
            tvMemorizeCountdown.setText("Pattern visible: " + revealSecs + "s remaining");
        }
        memorizeTimer = new CountDownTimer(revealSecs * 1000L, 1000) {
            @Override public void onTick(long ms) {
                int sec = (int) Math.ceil(ms / 1000.0);
                if (tvMemorizeCountdown != null)
                    tvMemorizeCountdown.setText("Pattern visible: " + sec + "s remaining");
            }
            @Override public void onFinish() { enterMemoryAnswerPhase(prompt, timeLimitSecs); }
        }.start();
    }

    private void enterMemoryAnswerPhase(String prompt, int timeLimitSecs) {
        layoutSymbolSequence.setVisibility(View.GONE);
        if (cardPatternMemorize != null) cardPatternMemorize.setVisibility(View.GONE);
        if (cardPatternHidden != null)   cardPatternHidden.setVisibility(View.VISIBLE);

        tvQuestionPrompt.setText(prompt);

        cardOptionA.setVisibility(View.VISIBLE);
        cardOptionB.setVisibility(View.VISIBLE);
        cardOptionC.setVisibility(View.VISIBLE);
        cardOptionD.setVisibility(View.VISIBLE);

        setBtnDisabled("Choose an answer");
        startTimer(timeLimitSecs);
    }

    // ─────────────── API: submit-answer ───────────────

    private void callSubmitAnswer() {
        if (isSubmitted) return;
        isSubmitted = true;
        cancelTimers();

        if (timer != null) timer.cancel();

        setBtnDisabled("Submitting…");

        String selectedAnswer = getChoiceValue(currentSelection);
        String questionId = currentApiQuestion.optString("id");

        executor.execute(() -> {
            try {
                String token = getToken();
                if (token == null) return;

                JSONObject body = new JSONObject();
                body.put("sessionId", sessionId);
                body.put("sessionQuestionId", questionId);
                body.put("selectedAnswer", selectedAnswer);

                URL url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/submit-answer");
                HttpURLConnection conn = openPostConn(url, token);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes());
                    os.flush();
                }

                int code = conn.getResponseCode();
                String resp = readStream(code == 200 ? conn.getInputStream() : conn.getErrorStream());
                conn.disconnect();

                if (code == 200) {
                    JSONObject json = new JSONObject(resp);
                    boolean isCorrect    = json.optBoolean("isCorrect", false);
                    int earned           = json.optInt("scoreEarned", 0);
                    boolean isComplete   = json.optBoolean("isComplete", false);
                    JSONObject nextQ     = json.optJSONObject("nextQuestion");

                    // Store result for review
                    String correctAns = currentApiQuestion.optString("correctAnswer", "");
                    JSONArray choices  = currentApiQuestion.optJSONArray("choices");
                    String expl        = currentApiQuestion.optString("explanation", "");
                    String qType       = currentApiQuestion.optString("questionType", "");

                    userAnswers[currentQuestionIndex]   = selectedAnswer;
                    correctAnswers[currentQuestionIndex]= correctAns;
                    explanations[currentQuestionIndex]  = expl;
                    questionTypes[currentQuestionIndex] = qType;
                    wasCorrect[currentQuestionIndex]    = isCorrect;
                    scoreEarned[currentQuestionIndex]   = earned;
                    timeSpent[currentQuestionIndex]     = 0; // server-side; display placeholder

                    if (isCorrect) { localScore += earned; localStreak++; }
                    else           { localStreak = 0; }

                    final JSONObject nextQuestion = nextQ;
                    final boolean complete = isComplete;

                    handler.post(() -> showAnswerFeedback(
                            isCorrect, earned, complete, nextQuestion, choices));

                } else {
                    Log.e(TAG, "submit-answer error " + code + ": " + resp);
                    handler.post(() -> Toast.makeText(this,
                            "Submit error: " + resp, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "callSubmitAnswer", e);
            }
        });
    }

    private void showAnswerFeedback(boolean isCorrect, int earned, boolean isComplete,
                                    JSONObject nextQuestion, JSONArray choices) {
        int padding = dp(14);

        // Highlight the correct answer card
        String correctAnswer = currentApiQuestion.optString("correctAnswer", "");
        int correctIdx = findChoiceIndex(choices, correctAnswer);
        View correctCard = getOptionCard(correctIdx);
        if (correctCard != null) {
            correctCard.setBackgroundResource(R.drawable.bg_card_green_bordered);
            correctCard.setPadding(padding, padding, padding, padding);
        }
        if (!isCorrect) {
            View wrongCard = getOptionCard(currentSelection);
            if (wrongCard != null) {
                wrongCard.setBackgroundResource(R.drawable.bg_card_red_bordered);
                wrongCard.setPadding(padding, padding, padding, padding);
            }
        }

        // Feedback card
        if (layoutFeedbackCard != null) {
            layoutFeedbackCard.setVisibility(View.VISIBLE);
            layoutFeedbackCard.setBackgroundResource(
                    isCorrect ? R.drawable.bg_card_green_bordered : R.drawable.bg_card_red_bordered);
            tvFeedbackTitle.setText(isCorrect ? "Correct!" : "Incorrect!");
            if (isCorrect) {
                tvFeedbackSubtitle.setText("Great job! Keep the streak alive.");
                tvFeedbackPoints.setText("+" + earned + " Score");
                tvFeedbackPoints.setVisibility(View.VISIBLE);
            } else {
                tvFeedbackSubtitle.setText("The correct answer was: " + correctAnswer);
                tvFeedbackPoints.setVisibility(View.GONE);
            }
        }

        tvScoreText.setText(String.valueOf(localScore));
        tvStreakText.setText(String.valueOf(localStreak));

        // Auto-advance
        final String baseBtnText = isComplete ? "Finish Round" : "Next Question";
        setBtnEnabled(baseBtnText + " (5s)");

        autoAdvanceTimer = new CountDownTimer(5000, 1000) {
            @Override public void onTick(long ms) {
                int sec = (int) Math.ceil(ms / 1000.0);
                btnSubmit.setText(baseBtnText + " (" + sec + "s)");
            }
            @Override public void onFinish() {
                btnSubmit.setText(baseBtnText);
                if (isComplete) {
                    callFinishChallenge();
                } else {
                    currentApiQuestion = nextQuestion;
                    currentQuestionIndex++;
                    renderQuestion();
                }
            }
        }.start();

        // Wire manual btn press
        btnSubmit.setOnClickListener(v -> {
            if (autoAdvanceTimer != null) autoAdvanceTimer.cancel();
            btnSubmit.setText(baseBtnText);
            if (isComplete) {
                callFinishChallenge();
            } else {
                currentApiQuestion = nextQuestion;
                currentQuestionIndex++;
                renderQuestion();
                // Restore normal btn click listener
                btnSubmit.setOnClickListener(bv -> {
                    String t = btnSubmit.getText().toString();
                    if (t.startsWith("Submit Answer")) callSubmitAnswer();
                    else if (t.startsWith("Next Question")) { if (autoAdvanceTimer != null) autoAdvanceTimer.cancel(); showNextQuestion(); }
                    else if (t.startsWith("Finish Round"))  { if (autoAdvanceTimer != null) autoAdvanceTimer.cancel(); callFinishChallenge(); }
                });
            }
        });
    }

    private void showNextQuestion() {
        // This path is hit when user manually clicks 'Next Question'
        // nextQuestion is wired through the closure in showAnswerFeedback, so this is a fallback
    }

    // ─────────────── API: finish-challenge ───────────────

    private void callFinishChallenge() {
        cancelTimers();
        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                String token = getToken();
                if (token == null) return;

                JSONObject body = new JSONObject();
                body.put("sessionId", sessionId);
                body.put("tabSwitchCount", 0);
                body.put("requestAnomalyFlags", new JSONArray());

                URL url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/finish-challenge");
                HttpURLConnection conn = openPostConn(url, token);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes());
                    os.flush();
                }

                int code = conn.getResponseCode();
                String resp = readStream(code == 200 ? conn.getInputStream() : conn.getErrorStream());
                conn.disconnect();

                if (code == 200) {
                    sessionResult = new JSONObject(resp);
                    handler.postDelayed(this::showResultReview, 1500);
                } else {
                    Log.e(TAG, "finish-challenge error " + code + ": " + resp);
                    handler.postDelayed(this::showResultReview, 1500);
                }
            } catch (Exception e) {
                Log.e(TAG, "callFinishChallenge", e);
                handler.postDelayed(this::showResultReview, 1500);
            }
        });
    }

    // ─────────────── Result Screen ───────────────

    private void showResultReview() {
        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.GONE);
        layoutResultReview.setVisibility(View.VISIBLE);

        int totalScore = localScore;
        double accuracy = 0;
        int correctCount = 0;
        double avgTime = 0;
        int ratingDelta = 0;
        String rankAfter = "BRONZE MIND";

        if (sessionResult != null) {
            totalScore   = sessionResult.optInt("totalScore", localScore);
            accuracy     = sessionResult.optDouble("accuracy", 0.0);
            correctCount = sessionResult.optInt("correctAnswers", 0);
            int wrongCount = sessionResult.optInt("wrongAnswers", 0);
            avgTime      = sessionResult.optDouble("averageTimeSeconds", 0.0);
            ratingDelta  = sessionResult.optInt("ratingDelta", 0);
            rankAfter    = sessionResult.optString("rankAfter", "BRONZE MIND").toUpperCase().replace("_", " ");
            correctCount = sessionResult.optInt("correctAnswers", 0);
            int wrongCnt = totalQuestions - correctCount;

            if (tvResultCorrectBadge != null) tvResultCorrectBadge.setText(correctCount + " CORRECT");
            if (tvResultWrongBadge   != null) tvResultWrongBadge.setText(wrongCnt + " WRONG");
        } else {
            // Fallback local count
            for (boolean b : wasCorrect) { if (b) correctCount++; }
            int wrongCnt = totalQuestions - correctCount;
            accuracy = totalQuestions > 0 ? correctCount * 100.0 / totalQuestions : 0;
            if (tvResultCorrectBadge != null) tvResultCorrectBadge.setText(correctCount + " CORRECT");
            if (tvResultWrongBadge   != null) tvResultWrongBadge.setText(wrongCnt + " WRONG");
        }

        tvResultScore.setText(String.format("%,d", totalScore));
        tvResultAccuracy.setText(String.format("%.0f%%", accuracy));
        tvResultAvgTime.setText(String.format("%.1fs", avgTime));

        String ratingStr = ratingDelta >= 0 ? "+" + ratingDelta : String.valueOf(ratingDelta);
        tvResultRatingChange.setText(ratingStr);

        if (tvResultRank != null) tvResultRank.setText(rankAfter);

        if (sessionResult != null && tvResultRankProgressPct != null) {
            JSONObject progress = sessionResult.optJSONObject("rankProgress");
            if (progress != null) {
                int pct = (int) (progress.optDouble("progressPercent", 0) * 100);
                tvResultRankProgressPct.setText(pct + "%");
                if (pbResultRankProgress != null) pbResultRankProgress.setProgress(pct);
                int needed = progress.optInt("pointsToNextRank", 0);
                if (tvResultRankProgress != null && needed > 0) {
                    String nextRank = progress.optString("nextRank", "Silver Solver")
                            .toUpperCase().replace("_", " ");
                    tvResultRankProgress.setText(needed + " points to " + nextRank);
                }
            }
        }

        if (tvResultMasteryText != null) {
            String modeName = selectedMode != null ? capitalize(selectedMode.replace("_", " ")) : "Mixed Mode";
            tvResultMasteryText.setText(modeName + ": Proficient");
        }

        // Build review cards
        layoutReviewCardsContainer.removeAllViews();
        for (int i = 0; i < totalQuestions && i < userAnswers.length; i++) {
            if (userAnswers[i] == null) break;
            buildReviewCard(i);
        }
    }

    private void buildReviewCard(int i) {
        boolean correct = wasCorrect[i];
        String qType    = questionTypes[i] != null ? questionTypes[i] : "";
        String userAns  = userAnswers[i]   != null ? userAnswers[i]   : "-";
        String corrAns  = correctAnswers[i]!= null ? correctAnswers[i]: "-";
        String expl     = explanations[i]  != null ? explanations[i]  : "";

        int padding = dp(14);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card_white);
        card.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cp);

        // Header row
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView badgeStatus = makeBadge(correct ? "CORRECT" : "WRONG",
                correct ? R.drawable.bg_badge_green : R.drawable.bg_chip_red,
                correct ? Color.WHITE : Color.WHITE);
        header.addView(badgeStatus);

        String modeName = qTypeLabel(qType);
        TextView badgeMode = makeBadge(modeName, R.drawable.bg_badge_teal, Color.BLACK);
        LinearLayout.LayoutParams bmp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bmp.setMarginStart(dp(8));
        badgeMode.setLayoutParams(bmp);
        header.addView(badgeMode);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));
        header.addView(spacer);

        TextView tvQNum = new TextView(this);
        tvQNum.setText("Question " + (i + 1));
        tvQNum.setTextColor(Color.parseColor("#777777"));
        tvQNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvQNum.setTypeface(null, Typeface.BOLD);
        header.addView(tvQNum);

        card.addView(header);

        // Stats row
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMargins(0, dp(12), 0, 0);
        stats.setLayoutParams(sp);
        stats.setWeightSum(3f);

        stats.addView(makeStatCol("YOUR ANSWER", userAns,
                correct ? Color.parseColor("#4CAF50") : Color.parseColor("#E53935")));
        stats.addView(makeStatCol("CORRECT ANSWER", corrAns, Color.BLACK));
        stats.addView(makeStatCol("SCORE", "+" + scoreEarned[i], Color.BLACK));
        card.addView(stats);

        // Explanation
        if (!expl.isEmpty()) {
            LinearLayout explBox = new LinearLayout(this);
            explBox.setOrientation(LinearLayout.VERTICAL);
            explBox.setBackgroundResource(R.drawable.bg_border_black_cream);
            explBox.setPadding(dp(10), dp(8), dp(10), dp(8));
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ep.setMargins(0, dp(10), 0, 0);
            explBox.setLayoutParams(ep);

            TextView tvExplT = new TextView(this);
            tvExplT.setText("EXPLANATION");
            tvExplT.setTextColor(Color.parseColor("#555555"));
            tvExplT.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvExplT.setTypeface(null, Typeface.BOLD);
            explBox.addView(tvExplT);

            TextView tvExplC = new TextView(this);
            tvExplC.setText(expl);
            tvExplC.setTextColor(Color.BLACK);
            tvExplC.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            LinearLayout.LayoutParams ecp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ecp.setMargins(0, dp(2), 0, 0);
            tvExplC.setLayoutParams(ecp);
            explBox.addView(tvExplC);

            card.addView(explBox);
        }

        layoutReviewCardsContainer.addView(card);
    }

    // ─────────────── Timer ───────────────

    private void startTimer(int seconds) {
        cancelTimers();
        secondsRemaining = seconds;
        tvGameTimer.setText(seconds + "s");
        timer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override public void onTick(long ms) {
                secondsRemaining = (int) (ms / 1000);
                tvGameTimer.setText(secondsRemaining + "s");
            }
            @Override public void onFinish() {
                secondsRemaining = 0;
                tvGameTimer.setText("0s");
                if (currentSelection == -1) currentSelection = 0;
                callSubmitAnswer();
            }
        }.start();
    }

    private void cancelTimers() {
        if (timer != null)             { timer.cancel();            timer = null; }
        if (memorizeTimer != null)     { memorizeTimer.cancel();    memorizeTimer = null; }
        if (autoAdvanceTimer != null)  { autoAdvanceTimer.cancel(); autoAdvanceTimer = null; }
    }

    // ─────────────── Abandon ───────────────

    private void abandonAndFinish() {
        cancelTimers();
        if (sessionId == null) { finish(); return; }
        executor.execute(() -> {
            try {
                String token = getToken();
                if (token == null) return;
                JSONObject body = new JSONObject();
                body.put("sessionId", sessionId);
                URL url = new URL(BuildConfig.SUPABASE_URL + "/functions/v1/abandon-challenge");
                HttpURLConnection conn = openPostConn(url, token);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes()); os.flush();
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
            handler.post(this::finish);
        });
    }

    private void restartSession() {
        currentQuestionIndex = 0;
        localScore = 0;
        localStreak = 0;
        sessionResult = null;
        userAnswers = null;
        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.GONE);
        layoutResultReview.setVisibility(View.GONE);
        tvScoreText.setText("0");
        tvStreakText.setText("0");
        callStartChallenge();
    }

    // ─────────────── Option Selection ───────────────

    private void selectOption(int index) {
        if (isSubmitted) return;
        currentSelection = index;
        resetOptionBackgrounds();
        View v = getOptionCard(index);
        if (v != null) {
            v.setBackgroundResource(R.drawable.bg_card_teal);
            v.setPadding(dp(14), dp(14), dp(14), dp(14));
        }
        setBtnEnabled("Submit Answer");
    }

    private void resetOptionBackgrounds() {
        int p = dp(14);
        for (View card : new View[]{cardOptionA, cardOptionB, cardOptionC, cardOptionD}) {
            if (card != null) {
                card.setBackgroundResource(R.drawable.bg_card_white);
                card.setPadding(p, p, p, p);
            }
        }
    }

    private View getOptionCard(int i) {
        switch (i) {
            case 0: return cardOptionA;
            case 1: return cardOptionB;
            case 2: return cardOptionC;
            case 3: return cardOptionD;
            default: return null;
        }
    }

    /** Returns the string value of the selected choice (for submitting to API). */
    private String getChoiceValue(int index) {
        JSONArray choices = currentApiQuestion.optJSONArray("choices");
        if (choices == null || index < 0 || index >= choices.length()) return "";
        return choices.optString(index);
    }

    private int findChoiceIndex(JSONArray choices, String answer) {
        if (choices == null || answer == null) return -1;
        for (int i = 0; i < choices.length(); i++) {
            if (answer.equals(choices.optString(i))) return i;
        }
        return -1;
    }

    // ─────────────── Symbol Mapping ───────────────

    private void setSymbolImage(ImageView iv, String token) {
        if (iv == null || token == null) return;
        iv.setBackgroundResource(BG_WHITE_CARD);
        int pad = dp(6);
        iv.setPadding(pad, pad, pad, pad);
        switch (token.toLowerCase().trim()) {
            case "circle":         iv.setImageResource(COLORED_CIRCLE);  break;
            case "star":           iv.setImageResource(COLORED_STAR);    break;
            case "diamond":        iv.setImageResource(COLORED_DIAMOND); break;
            case "square":         iv.setImageResource(COLORED_SQUARE);  break;
            case "triangle-right":
            case "triangle_right": iv.setImageResource(ARROW_RIGHT);     break;
            case "triangle-down":
            case "triangle_down":  iv.setImageResource(ARROW_DOWN);      break;
            case "triangle-left":
            case "triangle_left":  iv.setImageResource(ARROW_LEFT);      break;
            case "triangle-up":
            case "triangle_up":    iv.setImageResource(ARROW_UP);        break;
            default:               iv.setImageResource(COLORED_CIRCLE);  break;
        }
    }

    private boolean isSymbolToken(String s) {
        if (s == null) return false;
        String l = s.toLowerCase();
        return l.equals("circle") || l.equals("star") || l.equals("diamond") || l.equals("square")
                || l.contains("triangle") || l.contains("hex") || l.contains("pent") || l.contains("oct");
    }

    // ─────────────── Helpers ───────────────

    private String getToken() {
        try {
            MasterKey mk = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    this, "auth_prefs", mk,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            String token = prefs.getString("access_token", null);
            if (token != null) return token;
            // Try refresh
            return refreshToken(prefs);
        } catch (Exception e) {
            Log.e(TAG, "getToken", e);
            return null;
        }
    }

    /** Calls /auth/v1/token?grant_type=refresh_token and updates stored tokens. */
    private String refreshToken(SharedPreferences prefs) {
        try {
            String rt = prefs.getString("refresh_token", null);
            if (rt == null || rt.isEmpty()) return null;

            URL url = new URL(BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            JSONObject body = new JSONObject();
            body.put("refresh_token", rt);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes()); os.flush();
            }
            int code = conn.getResponseCode();
            String resp = readStream(code < 400 ? conn.getInputStream() : conn.getErrorStream());
            conn.disconnect();
            if (code == 200) {
                JSONObject json = new JSONObject(resp);
                String newAccess  = json.getString("access_token");
                String newRefresh = json.optString("refresh_token", rt);
                prefs.edit()
                        .putString("access_token", newAccess)
                        .putString("refresh_token", newRefresh)
                        .apply();
                Log.d(TAG, "Token refreshed successfully");
                return newAccess;
            } else {
                Log.e(TAG, "Token refresh failed " + code + ": " + resp);
                // Clear stored creds so next launch goes to login
                prefs.edit().remove("access_token").remove("refresh_token").apply();
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "refreshToken", e);
            return null;
        }
    }

    private HttpURLConnection openPostConn(URL url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        return conn;
    }

    private String readStream(java.io.InputStream stream) {
        if (stream == null) return "";
        try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private void setBtnDisabled(String text) {
        btnSubmit.setEnabled(false);
        btnSubmit.setBackgroundResource(R.drawable.bg_button_disabled);
        btnSubmit.setTextColor(Color.GRAY);
        btnSubmit.setText(text);
    }

    private void setBtnEnabled(String text) {
        btnSubmit.setEnabled(true);
        btnSubmit.setBackgroundResource(R.drawable.bg_button_primary);
        btnSubmit.setTextColor(Color.BLACK);
        btnSubmit.setText(text);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String qTypeLabel(String qType) {
        switch (qType) {
            case "symbol_pattern": return "Symbol Patterns";
            case "mini_deduction": return "Mini Deduction";
            case "memory_pattern": return "Pattern Memory";
            case "number_sequence": return "Number Patterns";
            default: return "Mixed";
        }
    }

    private TextView makeBadge(String text, int bgRes, int textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(dp(6), dp(2), dp(6), dp(2));
        tv.setBackgroundResource(bgRes);
        return tv;
    }

    private LinearLayout makeStatCol(String label, String value, int valueColor) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvL = new TextView(this);
        tvL.setText(label);
        tvL.setTextColor(Color.parseColor("#777777"));
        tvL.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tvL.setTypeface(null, Typeface.BOLD);
        col.addView(tvL);

        TextView tvV = new TextView(this);
        tvV.setText(value);
        tvV.setTextColor(valueColor);
        tvV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvV.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vp.setMargins(0, dp(2), 0, 0);
        tvV.setLayoutParams(vp);
        col.addView(tvV);

        return col;
    }

    // ─────────────── Lifecycle ───────────────

    @Override
    protected void onDestroy() {
        cancelTimers();
        executor.shutdownNow();
        super.onDestroy();
    }
}
