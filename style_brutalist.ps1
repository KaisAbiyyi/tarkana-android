$cardWhite = @"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Shadow -->
    <item android:top="4dp" android:left="4dp">
        <shape android:shape="rectangle">
            <solid android:color="#000000" />
            <corners android:radius="0dp" />
        </shape>
    </item>
    <!-- Card -->
    <item android:bottom="4dp" android:right="4dp">
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <stroke android:width="2dp" android:color="#000000" />
            <corners android:radius="0dp" />
        </shape>
    </item>
</layer-list>
"@

$buttonPrimary = @"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:top="4dp" android:left="4dp">
        <shape android:shape="rectangle">
            <solid android:color="#000000" />
        </shape>
    </item>
    <item android:bottom="4dp" android:right="4dp">
        <shape android:shape="rectangle">
            <solid android:color="#F5C518" />
            <stroke android:width="2dp" android:color="#000000" />
        </shape>
    </item>
</layer-list>
"@

$badgeYellow = @"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:top="2dp" android:left="2dp">
        <shape android:shape="rectangle">
            <solid android:color="#000000" />
        </shape>
    </item>
    <item android:bottom="2dp" android:right="2dp">
        <shape android:shape="rectangle">
            <solid android:color="#F5C518" />
            <stroke android:width="2dp" android:color="#000000" />
        </shape>
    </item>
</layer-list>
"@

$badgeBronze = @"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:top="2dp" android:left="2dp">
        <shape android:shape="rectangle">
            <solid android:color="#000000" />
        </shape>
    </item>
    <item android:bottom="2dp" android:right="2dp">
        <shape android:shape="rectangle">
            <solid android:color="#CD7F32" />
            <stroke android:width="2dp" android:color="#000000" />
        </shape>
    </item>
</layer-list>
"@

$badgeSilver = @"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:top="2dp" android:left="2dp">
        <shape android:shape="rectangle">
            <solid android:color="#000000" />
        </shape>
    </item>
    <item android:bottom="2dp" android:right="2dp">
        <shape android:shape="rectangle">
            <solid android:color="#C0C0C0" />
            <stroke android:width="2dp" android:color="#000000" />
        </shape>
    </item>
</layer-list>
"@

$inputField = @"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:top="2dp" android:left="2dp">
        <shape android:shape="rectangle">
            <solid android:color="#000000" />
        </shape>
    </item>
    <item android:bottom="2dp" android:right="2dp">
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <stroke android:width="2dp" android:color="#000000" />
        </shape>
    </item>
</layer-list>
"@

$cardBlue = @"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:top="4dp" android:left="4dp">
        <shape android:shape="rectangle">
            <solid android:color="#000000" />
        </shape>
    </item>
    <item android:bottom="4dp" android:right="4dp">
        <shape android:shape="rectangle">
            <solid android:color="#4DD0E1" />
            <stroke android:width="2dp" android:color="#000000" />
        </shape>
    </item>
</layer-list>
"@

$cardGreen = @"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:top="4dp" android:left="4dp">
        <shape android:shape="rectangle">
            <solid android:color="#000000" />
        </shape>
    </item>
    <item android:bottom="4dp" android:right="4dp">
        <shape android:shape="rectangle">
            <solid android:color="#AED581" />
            <stroke android:width="2dp" android:color="#000000" />
        </shape>
    </item>
</layer-list>
"@

Set-Content "app/src/main/res/drawable/bg_card_white.xml" $cardWhite
Set-Content "app/src/main/res/drawable/bg_button_primary.xml" $buttonPrimary
Set-Content "app/src/main/res/drawable/bg_badge_yellow.xml" $badgeYellow
Set-Content "app/src/main/res/drawable/bg_badge_rank_bronze.xml" $badgeBronze
Set-Content "app/src/main/res/drawable/bg_badge_rank_silver.xml" $badgeSilver
Set-Content "app/src/main/res/drawable/bg_input.xml" $inputField
Set-Content "app/src/main/res/drawable/bg_card_blue_bordered.xml" $cardBlue
Set-Content "app/src/main/res/drawable/bg_card_green_bordered.xml" $cardGreen
