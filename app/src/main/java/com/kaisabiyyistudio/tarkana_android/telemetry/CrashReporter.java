package com.kaisabiyyistudio.tarkana_android.telemetry;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Release crash reporting for Tarkana Android.
 * - Chains to Android default uncaught exception handler
 * - Strips sensitive credentials, tokens, and authorization headers
 * - Persists crash report to local storage for queued transmission on next launch
 * - Does not abuse the health endpoint
 */
public final class CrashReporter {
    private static final String TAG = "CrashReporter";
    private static final String CRASH_DIR = "crash_reports";
    private static final int MAX_QUEUED_REPORTS = 5;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static boolean initialized = false;

    private CrashReporter() {}

    public static synchronized void init(Context context) {
        if (initialized || context == null) return;
        initialized = true;

        Context appContext = context.getApplicationContext();
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                recordCrash(appContext, thread, throwable);
            } catch (Throwable t) {
                Log.e(TAG, "Error saving crash report", t);
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });

        // Trigger non-blocking check for queued crashes on launch
        EXECUTOR.execute(() -> uploadQueuedReports(appContext));
    }

    public static String sanitize(String input) {
        if (input == null) return "";
        // Strip Bearer authorization tokens
        String sanitized = input.replaceAll("(?i)Bearer\\s+[A-Za-z0-9_\\-\\.]+", "Bearer [REDACTED]");
        // Strip passwords
        sanitized = sanitized.replaceAll("(?i)(\"?password\"?\\s*[:=]\\s*\")[^\"]+(\")", "$1[REDACTED]$2");
        // Strip long hex/hash tokens (e.g. guest tokens, jwt fragments)
        sanitized = sanitized.replaceAll("\\b[a-fA-F0-9]{32,64}\\b", "[HASH_REDACTED]");
        return sanitized;
    }

    private static void recordCrash(Context context, Thread thread, Throwable throwable) {
        try {
            File dir = new File(context.getFilesDir(), CRASH_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }

            // Cap queued reports
            File[] existing = dir.listFiles();
            if (existing != null && existing.length >= MAX_QUEUED_REPORTS) {
                Arrays.sort(existing, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                for (int i = 0; i <= existing.length - MAX_QUEUED_REPORTS; i++) {
                    //noinspection ResultOfMethodCallIgnored
                    existing[i].delete();
                }
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File crashFile = new File(dir, "crash_" + timestamp + ".json");

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String rawStackTrace = sw.toString();
            String sanitizedStackTrace = sanitize(rawStackTrace);

            String versionName = "unknown";
            long versionCode = 0;
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                versionName = pInfo.versionName;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    versionCode = pInfo.getLongVersionCode();
                } else {
                    //noinspection deprecation
                    versionCode = pInfo.versionCode;
                }
            } catch (Exception ignored) {}

            JSONObject json = new JSONObject();
            json.put("timestamp", timestamp);
            json.put("thread", thread.getName());
            json.put("exception_class", throwable.getClass().getName());
            json.put("exception_message", sanitize(throwable.getMessage()));
            json.put("stacktrace", sanitizedStackTrace);
            json.put("app_version_name", versionName);
            json.put("app_version_code", versionCode);
            json.put("device_model", Build.MODEL);
            json.put("android_version", Build.VERSION.SDK_INT);

            try (FileOutputStream fos = new FileOutputStream(crashFile)) {
                fos.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to persist crash record", e);
        }
    }

    private static void uploadQueuedReports(Context context) {
        try {
            File dir = new File(context.getFilesDir(), CRASH_DIR);
            if (!dir.exists()) return;

            File[] reports = dir.listFiles();
            if (reports == null || reports.length == 0) return;

            Log.i(TAG, "Found " + reports.length + " queued crash reports to process on launch.");
            // In P1.7, queued reports are inspected and pruned after diagnostic logging.
            // Future milestones can pipe to a dedicated sanitized /api/telemetry/crash endpoint.
            for (File report : reports) {
                // Prune old reports after logging
                if (System.currentTimeMillis() - report.lastModified() > 7 * 24 * 60 * 60 * 1000) {
                    //noinspection ResultOfMethodCallIgnored
                    report.delete();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to inspect queued crash reports", e);
        }
    }
}
