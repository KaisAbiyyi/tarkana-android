package com.kaisabiyyistudio.tarkana_android;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionActivity extends AppCompatActivity {

    private static final String TAG = "SessionActivity";
    static final String PREF_PENDING_SESSION_ID = "pending_challenge_session_id";

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

    // Per-question tracking. Correct answers are intentionally not carried in gameplay DTOs.
    private String[] userAnswers;
    private String[] resultCorrectAnswers;
    private String[] resultExplanations;
    private String[] questionTypes;
    private boolean[] wasCorrect;
    private int[] scoreEarned;
    private int[] timeSpent;
    private JSONArray[] memorizeSeqs; // for memory q review

    private int currentSelection = -1;
    private boolean isSubmitted = false;
    private int localScore = 0;
    private int localStreak = 0;

    // Views â€“ gameplay
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
    private LinearLayout layoutMemorizeSequence;
    private ImageView ivMem1, ivMem2, ivMem3, ivMem4, ivMem5;

    private View cardQuestion, cardOptionA, cardOptionB, cardOptionC, cardOptionD;
    private TextView tvOptionAVal, tvOptionBVal, tvOptionCVal, tvOptionDVal;
    private ImageView ivOptionAVal, ivOptionBVal, ivOptionCVal, ivOptionDVal;
    private AppCompatButton btnSubmit;
    private TextView tvHelperText;
    private View tvQMarkBox;

    // Views â€“ memory phase
    private View cardPatternMemorize, cardPatternHidden;
    private TextView tvMemorizeCountdown;

    // Views â€“ feedback
    private View layoutFeedbackCard;
    private TextView tvFeedbackTitle, tvFeedbackSubtitle, tvFeedbackPoints;

    // Views â€“ result
    private TextView tvResultScore, tvResultAccuracy, tvResultAvgTime;
    private TextView tvResultRatingChange, tvResultMasteryText;
    private TextView tvResultCorrectBadge, tvResultWrongBadge;
    private TextView tvResultRank, tvResultRankProgress, tvResultRankProgressPct;
    private ProgressBar pbResultRankProgress;
    private LinearLayout layoutReviewCardsContainer;
    private AppCompatButton btnRetry, btnFinish, btnResultRetry, btnResultLeaderboard;

    private CountDownTimer timer;
    private CountDownTimer memorizeTimer;
    private int secondsRemaining = 30;
    private static final long FEEDBACK_DELAY_MS = 850L;

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
            }
        });

        if (btnRetry != null) btnRetry.setOnClickListener(v -> restartSession());
        if (btnFinish != null) btnFinish.setOnClickListener(v -> finish());
        if (btnResultRetry != null) btnResultRetry.setOnClickListener(v -> restartSession());
        if (btnResultLeaderboard != null) btnResultLeaderboard.setOnClickListener(v -> finish());

        if (getIntent().getBooleanExtra("resumeSession", false)) {
            resumeExistingChallenge();
        } else {
            callStartChallenge();
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ View Binding â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        cardQuestion    = findViewById(R.id.card_question);

        layoutSymbolSequence = findViewById(R.id.layout_symbol_sequence);
        ivSeq1 = findViewById(R.id.iv_seq_1);
        ivSeq2 = findViewById(R.id.iv_seq_2);
        ivSeq3 = findViewById(R.id.iv_seq_3);
        ivSeq4 = findViewById(R.id.iv_seq_4);
        ivSeq5 = findViewById(R.id.iv_seq_5);
        layoutMemorizeSequence = findViewById(R.id.layout_memorize_sequence);
        ivMem1 = findViewById(R.id.iv_mem_1);
        ivMem2 = findViewById(R.id.iv_mem_2);
        ivMem3 = findViewById(R.id.iv_mem_3);
        ivMem4 = findViewById(R.id.iv_mem_4);
        ivMem5 = findViewById(R.id.iv_mem_5);

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

    private void resumeExistingChallenge() {
        sessionId = getIntent().getStringExtra("sessionId");
        boolean resumeComplete = getIntent().getBooleanExtra("resumeComplete", false);

        if (sessionId == null || sessionId.isEmpty()) {
            finish();
            return;
        }

        if (resumeComplete) {
            callFinishChallenge();
            return;
        }

        String questionJson = getIntent().getStringExtra("currentQuestion");
        if (questionJson == null || questionJson.isEmpty()) {
            finish();
            return;
        }

        try {
            currentApiQuestion = new JSONObject(questionJson);
            totalQuestions = Math.max(
                    getIntent().getIntExtra("totalQuestions", 0),
                    currentApiQuestion.optInt("orderIndex", 0) + 1
            );
            currentQuestionIndex = currentApiQuestion.optInt("orderIndex", 0);
            selectedMode = currentApiQuestion.optString("questionType", selectedMode);
            allocateSessionArrays();
            setTitle();

            layoutGameplay.setVisibility(View.VISIBLE);
            layoutFinishing.setVisibility(View.GONE);
            layoutResultReview.setVisibility(View.GONE);
            renderQuestion();
        } catch (Exception e) {
            Log.e(TAG, "resumeExistingChallenge", e);
            finish();
        }
    }

    private void allocateSessionArrays() {
        userAnswers  = new String[totalQuestions];
        resultCorrectAnswers = new String[totalQuestions];
        resultExplanations = new String[totalQuestions];
        questionTypes= new String[totalQuestions];
        wasCorrect   = new boolean[totalQuestions];
        scoreEarned  = new int[totalQuestions];
        timeSpent    = new int[totalQuestions];
        memorizeSeqs = new JSONArray[totalQuestions];
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ API: start-challenge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void callStartChallenge() {
        // Show loading state
        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.GONE);
        layoutResultReview.setVisibility(View.GONE);
        btnSubmit.setEnabled(false);
        btnSubmit.setBackgroundResource(R.drawable.bg_button_disabled);
        btnSubmit.setTextColor(Color.GRAY);
        btnSubmit.setText("Loadingâ€¦");

        executor.execute(() -> {
            try {
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

                ApiClient.ApiResponse response = ApiClient.postFunction(this, "/start-challenge", body);
                int code = response.code;
                String resp = response.body;

                if (code == 200) {
                    JSONObject json = response.json();
                    sessionId      = json.getString("sessionId");
                    rememberPendingSession(sessionId);
                    totalQuestions = json.getInt("totalQuestions");
                    JSONObject q   = json.getJSONObject("currentQuestion");

                    allocateSessionArrays();

                    currentApiQuestion = q;
                    currentQuestionIndex = 0;
                    handler.post(() -> {
                        layoutGameplay.setVisibility(View.VISIBLE);
                        renderQuestion();
                    });
                } else if (code == 409) {
                    JSONObject activeChallenge = fetchActiveChallenge(false);
                    if (activeChallenge != null) {
                        handler.post(() -> resumeFromActiveChallenge(activeChallenge));
                    } else {
                        handler.post(() -> {
                            Toast.makeText(this, "Ada session aktif, tapi gagal dimuat. Coba buka tab Challenge lagi.", Toast.LENGTH_LONG).show();
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
            } catch (ApiClient.AuthException e) {
                Log.e(TAG, "callStartChallenge auth", e);
                handler.post(() -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "callStartChallenge", e);
                handler.post(this::finish);
            }
        });
    }

    private JSONObject fetchActiveChallenge(boolean isRetry) {
        try {
            ApiClient.ApiResponse response = ApiClient.getFunction(this, "/get-active-challenge");
            int code = response.code;
            String resp = response.body;

            if (code != 200) {
                Log.e(TAG, "fetchActiveChallenge failed " + code + ": " + resp);
                return null;
            }

            JSONObject json = response.json();
            return json.optBoolean("hasActive", false) ? json : null;
        } catch (Exception e) {
            Log.e(TAG, "fetchActiveChallenge", e);
            return null;
        }
    }

    private void resumeFromActiveChallenge(JSONObject activeChallenge) {
        boolean isComplete = activeChallenge.optBoolean("isComplete", false);
        sessionId = activeChallenge.optString("sessionId", sessionId);
        rememberPendingSession(sessionId);

        if (isComplete) {
            callFinishChallenge();
            return;
        }

        JSONObject currentQuestion = activeChallenge.optJSONObject("currentQuestion");
        if (currentQuestion == null) {
            finish();
            return;
        }

        currentApiQuestion = currentQuestion;
        totalQuestions = Math.max(
                activeChallenge.optInt("totalQuestions", 0),
                currentApiQuestion.optInt("orderIndex", 0) + 1
        );
        currentQuestionIndex = currentApiQuestion.optInt("orderIndex", 0);
        selectedMode = currentApiQuestion.optString("questionType", selectedMode);
        allocateSessionArrays();
        setTitle();
        layoutGameplay.setVisibility(View.VISIBLE);
        layoutFinishing.setVisibility(View.GONE);
        layoutResultReview.setVisibility(View.GONE);
        renderQuestion();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Render Current Question â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void renderQuestion() {
        isSubmitted = false;
        currentSelection = -1;
        btnSubmit.setVisibility(View.VISIBLE);
        resetOptionBackgrounds();
        if (layoutFeedbackCard != null) layoutFeedbackCard.setVisibility(View.GONE);
        if (cardQuestion != null) cardQuestion.setVisibility(View.VISIBLE);
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
        if (metadata == null) {
            metadata = new JSONObject();
        }

        boolean isSymbol = questionType.equals("symbol_pattern");
        boolean isMemory = questionType.equals("memory_pattern");

        if (isMemory) {
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
        if (cardQuestion != null) cardQuestion.setVisibility(View.GONE);
        if (cardPatternHidden != null) cardPatternHidden.setVisibility(View.GONE);
        if (cardPatternMemorize != null) cardPatternMemorize.setVisibility(View.VISIBLE);

        tvQuestionPrompt.setText("Memorize the sequence.");
        layoutSymbolSequence.setVisibility(View.GONE);

        JSONArray memorize = extractMemorySequence(metadata);
        int revealSecs = metadata.optInt("revealSeconds", 4);

        memorizeSeqs[currentQuestionIndex] = memorize;

        ImageView[] seqViews = {ivMem1, ivMem2, ivMem3, ivMem4, ivMem5};
        boolean hasMemorize = memorize != null && memorize.length() > 0;
        if (layoutMemorizeSequence != null) {
            layoutMemorizeSequence.setVisibility(hasMemorize ? View.VISIBLE : View.GONE);
        }
        if (hasMemorize) {
            for (int i = 0; i < seqViews.length; i++) {
                if (i < memorize.length()) {
                    setSymbolImage(seqViews[i], memorize.optString(i));
                    seqViews[i].setVisibility(View.VISIBLE);
                } else {
                    seqViews[i].setVisibility(View.GONE);
                }
            }
        } else {
            for (ImageView seqView : seqViews) {
                seqView.setImageDrawable(null);
                seqView.setVisibility(View.GONE);
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

        setBtnDisabled("Memorizing...");

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
        if (cardQuestion != null) cardQuestion.setVisibility(View.VISIBLE);
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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ API: submit-answer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void callSubmitAnswer() {
        if (isSubmitted) return;
        isSubmitted = true;
        cancelTimers();

        if (timer != null) timer.cancel();

        setBtnDisabled("Submittingâ€¦");

        String selectedAnswer = getChoiceValue(currentSelection);
        String questionId = currentApiQuestion.optString("id");

        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("sessionId", sessionId);
                body.put("sessionQuestionId", questionId);
                body.put("selectedAnswer", selectedAnswer);

                ApiClient.ApiResponse response = ApiClient.postFunction(this, "/submit-answer", body);
                int code = response.code;
                String resp = response.body;

                if (code == 200) {
                    JSONObject json = response.json();
                    boolean isCorrect    = json.optBoolean("isCorrect", false);
                    int earned           = json.optInt("scoreEarned", 0);
                    boolean isComplete   = json.optBoolean("isComplete", false);
                    JSONObject nextQ     = json.optJSONObject("nextQuestion");

                    // Store only client-safe gameplay state. The server returns answer keys in
                    // final result review, never during active challenge play.
                    JSONArray choices  = currentApiQuestion.optJSONArray("choices");
                    String qType       = currentApiQuestion.optString("questionType", "");

                    userAnswers[currentQuestionIndex]   = selectedAnswer;
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

        // Do not reveal the answer key during gameplay. The backend is the source of truth.
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
                tvFeedbackSubtitle.setText("Review the full explanation after the round.");
                tvFeedbackPoints.setVisibility(View.GONE);
            }
        }

        tvScoreText.setText(String.valueOf(localScore));
        tvStreakText.setText(String.valueOf(localStreak));

        btnSubmit.setVisibility(View.GONE);
        setBtnDisabled(isComplete ? "Finishing Round" : "Next Question");
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (isComplete) {
                callFinishChallenge();
            } else {
                currentApiQuestion = nextQuestion;
                currentQuestionIndex++;
                renderQuestion();
            }
        }, FEEDBACK_DELAY_MS);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ API: finish-challenge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void callFinishChallenge() {
        cancelTimers();
        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("sessionId", sessionId);
                body.put("tabSwitchCount", 0);
                body.put("requestAnomalyFlags", new JSONArray());

                ApiClient.ApiResponse response = ApiClient.postFunction(this, "/finish-challenge", body);
                int code = response.code;
                String resp = response.body;

                if (code == 200) {
                    sessionResult = response.json();
                    clearPendingSession();
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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Result Screen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
            hydrateResultReviewFromServer();
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

    private void hydrateResultReviewFromServer() {
        if (sessionResult == null) return;
        JSONArray review = sessionResult.optJSONArray("review");
        if (review == null) return;

        for (int i = 0; i < review.length() && i < totalQuestions; i++) {
            JSONObject item = review.optJSONObject(i);
            if (item == null) continue;

            int orderIndex = item.optInt("orderIndex", i);
            if (orderIndex < 0 || orderIndex >= totalQuestions) continue;

            if (resultCorrectAnswers != null) {
                resultCorrectAnswers[orderIndex] = item.optString("correctAnswer", "-");
            }
            if (resultExplanations != null) {
                resultExplanations[orderIndex] = item.optString("explanation", "");
            }
            if (userAnswers != null && userAnswers[orderIndex] == null) {
                userAnswers[orderIndex] = item.optString("selectedAnswer", "-");
            }
            if (questionTypes != null && questionTypes[orderIndex] == null) {
                questionTypes[orderIndex] = item.optString("questionType", "");
            }
            if (wasCorrect != null) {
                wasCorrect[orderIndex] = item.optBoolean("isCorrect", false);
            }
            if (scoreEarned != null) {
                scoreEarned[orderIndex] = item.optInt("scoreEarned", 0);
            }
            if (timeSpent != null) {
                timeSpent[orderIndex] = item.optInt("timeSpentSeconds", 0);
            }
        }
    }

    private void buildReviewCard(int i) {
        boolean correct = wasCorrect[i];
        String qType    = questionTypes[i] != null ? questionTypes[i] : "";
        String userAns  = userAnswers[i]   != null ? userAnswers[i]   : "-";
        String corrAns  = resultCorrectAnswers != null && resultCorrectAnswers[i] != null
                ? resultCorrectAnswers[i] : "-";
        String expl     = resultExplanations != null && resultExplanations[i] != null
                ? resultExplanations[i] : "";

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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Timer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Abandon â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void abandonAndFinish() {
        cancelTimers();
        if (sessionId == null) { finish(); return; }
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("sessionId", sessionId);
                ApiClient.ApiResponse response = ApiClient.postFunction(this, "/abandon-challenge", body);
                response.requireSuccess();
                clearPendingSession();
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
        resultCorrectAnswers = null;
        resultExplanations = null;
        layoutGameplay.setVisibility(View.GONE);
        layoutFinishing.setVisibility(View.GONE);
        layoutResultReview.setVisibility(View.GONE);
        tvScoreText.setText("0");
        tvStreakText.setText("0");
        callStartChallenge();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Option Selection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Symbol Mapping â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    private JSONArray extractMemorySequence(JSONObject metadata) {
        if (metadata == null) return null;

        JSONArray memorize = metadata.optJSONArray("memorize");
        if (memorize != null && memorize.length() > 0) return memorize;

        JSONArray sequence = metadata.optJSONArray("sequence");
        if (sequence != null && sequence.length() > 0) return sequence;

        JSONArray pattern = metadata.optJSONArray("pattern");
        if (pattern != null && pattern.length() > 0) return pattern;

        return null;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void rememberPendingSession(String id) {
        try {
            AuthSession.prefs(this).edit().putString(PREF_PENDING_SESSION_ID, id).apply();
        } catch (Exception e) {
            Log.e(TAG, "rememberPendingSession", e);
        }
    }

    private void clearPendingSession() {
        try {
            AuthSession.prefs(this).edit().remove(PREF_PENDING_SESSION_ID).apply();
        } catch (Exception e) {
            Log.e(TAG, "clearPendingSession", e);
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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    protected void onDestroy() {
        cancelTimers();
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }
}
