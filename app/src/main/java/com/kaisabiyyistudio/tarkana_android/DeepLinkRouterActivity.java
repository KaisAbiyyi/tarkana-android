package com.kaisabiyyistudio.tarkana_android;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;

/**
 * Trampoline Activity handling incoming App Links.
 * In P1.7, unported native flows are dispatched to an explicit safe web fallback (Custom Tabs / browser)
 * so intercepted links never provide an inferior or broken user experience.
 */
public class DeepLinkRouterActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent != null ? intent.getData() : null;

        DeepLinkRouter.Resolution resolution = DeepLinkRouter.resolve(data, BuildConfig.APP_LINKS_HOST);

        switch (resolution.destination) {
            case SAFE_WEB_FALLBACK_SHARE:
            case SAFE_WEB_FALLBACK_DUEL:
                launchSafeWebFallback(resolution.canonicalWebUrl);
                break;

            case MAIN_APP:
                launchMainApp();
                break;

            case UNKNOWN:
            default:
                Toast.makeText(this, "Unrecognized or expired challenge link", Toast.LENGTH_SHORT).show();
                launchMainApp();
                break;
        }

        finish();
    }

    private void launchSafeWebFallback(String url) {
        if (url == null || url.isEmpty()) {
            launchMainApp();
            return;
        }

        Uri uri = Uri.parse(url);
        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
            customTabsIntent.launchUrl(this, uri);
        } catch (ActivityNotFoundException e) {
            // Fallback to standard browser intent if Custom Tabs is unavailable
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(browserIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open web link", Toast.LENGTH_SHORT).show();
                launchMainApp();
            }
        }
    }

    private void launchMainApp() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(mainIntent);
    }
}
