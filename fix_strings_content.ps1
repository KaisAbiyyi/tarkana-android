$xmlPath = "app\src\main\res\values\strings.xml"
$content = Get-Content $xmlPath -Raw

$replacements = @{
    ">login_hero_text<" = ">Welcome back!<"
    ">login_feature_1<" = ">Logic puzzles to stretch your mind<"
    ">login_feature_2<" = ">Track your accuracy and speed<"
    ">login_feature_3<" = ">Compete on the global leaderboard<"
    ">login_feature_4<" = ">Climb the ranks to Grandmaster<"
    ">login_card_title<" = ">LOGIN<"
    ">login_card_subtitle<" = ">Please sign in to continue<"
    ">login_email_hint<" = ">Email address<"
    ">login_password_hint<" = ">Password<"
    ">login_divider<" = ">OR<"
    ">login_google_button<" = ">Sign in with Google<"
    ">login_register_prompt<" = ">Don't have an account? Register<"
    ">dashboard_rating<" = ">RATING<"
    ">dashboard_rank<" = ">BRONZE MIND<"
    ">dashboard_sessions_label<" = ">SESSIONS<"
    ">dashboard_best_score_label<" = ">BEST SCORE<"
    ">dashboard_next_title<" = ">CHOOSE CONFIGURATION<"
    ">dashboard_next_detail<" = ">Choose a session type and mode to prepare the round.<"
    ">start_challenge<" = ">START CHALLENGE<"
    ">dashboard_avg_accuracy_label<" = ">AVG ACCURACY<"
    ">dashboard_avg_time_label<" = ">AVG TIME<"
    ">dashboard_rank_progress_title<" = ">YOUR PROGRESS<"
    ">dashboard_rank_subtitle<" = ">Improve your accuracy and speed to reach a higher rank.<"
    ">dashboard_rank_progress_label<" = ">TOWARD SILVER<"
    ">dashboard_rank_pct<" = ">21%<"
    ">dashboard_rank_remaining<" = ">395 MORE RATING NEEDED<"
    ">dashboard_mastery_title<" = ">Mastery<"
    ">dashboard_mastery_empty<" = ">Unlock mastery details by completing more sessions.<"
    ">dashboard_recent_title<" = ">Recent Sessions<"
    ">dashboard_view_history<" = ">VIEW FULL HISTORY<"
    ">dashboard_sessions_value<" = ">3<"
    ">dashboard_best_score_value<" = ">98<"
    ">dashboard_avg_accuracy_value<" = ">86.7%<"
    ">dashboard_avg_time_value<" = ">12.4s<"
}

foreach ($key in $replacements.Keys) {
    $content = $content -replace $key, $replacements[$key]
}

Set-Content $xmlPath $content
