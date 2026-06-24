# Create background cards
@"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#4A4A4A" />
</shape>
"@ | Set-Content "app\src\main\res\drawable\bg_button_disabled.xml"

@"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="line">
    <stroke
        android:width="2dp"
        android:color="@color/color_text_primary"
        android:dashWidth="6dp"
        android:dashGap="4dp" />
</shape>
"@ | Set-Content "app\src\main\res\drawable\bg_divider_horizontal_dashed.xml"

@"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/color_text_primary" />
</shape>
"@ | Set-Content "app\src\main\res\drawable\bg_divider_vertical_dashed.xml"

# Vector icons
@"
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000" android:pathData="M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z"/>
</vector>
"@ | Set-Content "app\src\main\res\drawable\ic_shape_star.xml"

@"
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000" android:pathData="M12,2 L22,12 L12,22 L2,12 Z"/>
</vector>
"@ | Set-Content "app\src\main\res\drawable\ic_shape_diamond.xml"

@"
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000" android:pathData="M3,3 h18 v18 h-18 z"/>
</vector>
"@ | Set-Content "app\src\main\res\drawable\ic_shape_square.xml"

@"
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000" android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z"/>
</vector>
"@ | Set-Content "app\src\main\res\drawable\ic_shape_circle.xml"

@"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#F5C518" />
    <stroke android:width="2dp" android:color="#000000" />
</shape>
"@ | Set-Content "app\src\main\res\drawable\bg_card_yellow_bordered.xml"

@"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#00C9A7" />
    <stroke android:width="2dp" android:color="#000000" />
</shape>
"@ | Set-Content "app\src\main\res\drawable\bg_card_teal_bordered.xml"

@"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#42A5F5" />
    <stroke android:width="2dp" android:color="#000000" />
</shape>
"@ | Set-Content "app\src\main\res\drawable\bg_card_blue_bordered.xml"

@"
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#E53935" />
    <stroke android:width="2dp" android:color="#000000" />
</shape>
"@ | Set-Content "app\src\main\res\drawable\bg_card_red_bordered.xml"
