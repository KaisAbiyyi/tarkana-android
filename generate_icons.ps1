$icons = @(
    "ic_logo_t", "ic_dashboard", "ic_challenge", "ic_history", "ic_leaderboard",
    "ic_profile", "ic_check", "ic_bolt", "ic_chart", "ic_timer", "ic_mixed",
    "ic_number", "ic_symbol", "ic_deduction", "ic_memory", "ic_eye", "ic_eye_off",
    "ic_star", "ic_arrow_right", "ic_question_mark", "ic_verified", "ic_language",
    "ic_radio_unchecked", "ic_radio_checked"
)

$xmlContent = @"
<vector xmlns:android=`"http://schemas.android.com/apk/res/android`"
    android:width=`"24dp`"
    android:height=`"24dp`"
    android:viewportWidth=`"24.0`"
    android:viewportHeight=`"24.0`">
    <path
        android:fillColor=`"#FF000000`"
        android:pathData=`"M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z`"/>
</vector>
"@

foreach ($icon in $icons) {
    $filePath = "app\src\main\res\drawable\$icon.xml"
    if (!(Test-Path $filePath)) {
        Set-Content -Path $filePath -Value $xmlContent
        Write-Host "Created $filePath"
    }
}
