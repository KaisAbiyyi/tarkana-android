package com.kaisabiyyistudio.tarkana_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

final class AuthSession {
    private static final String TAG = "AuthSession";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 20000;

    private AuthSession() {}

    static SharedPreferences prefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context, "auth_prefs", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    static String accessToken(Context context) {
        try {
            return prefs(context).getString("access_token", null);
        } catch (Exception e) {
            Log.e(TAG, "accessToken", e);
            return null;
        }
    }

	static boolean save(Context context, String accessToken, String refreshToken) {
		try {
			return prefs(context)
					.edit()
					.putString("access_token", accessToken)
					.putString("refresh_token", refreshToken)
					.commit();
		} catch (Exception e) {
			Log.e(TAG, "save", e);
			return false;
		}
	}

    static String refreshAccessToken(Context context) {
        try {
            SharedPreferences prefs = prefs(context);
            String refreshToken = prefs.getString("refresh_token", null);
            if (refreshToken == null || refreshToken.isEmpty()) return null;

            HttpURLConnection conn = (HttpURLConnection) new URL(
                    BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token"
            ).openConnection();
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("refresh_token", refreshToken);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes());
            }

            int code = conn.getResponseCode();
            String response = "";
            try (Scanner scanner = new Scanner(code < 400 ? conn.getInputStream() : conn.getErrorStream()).useDelimiter("\\A")) {
                response = scanner.hasNext() ? scanner.next() : "";
            }
            conn.disconnect();

            if (code != 200) {
                Log.e(TAG, "refresh failed " + code + ": " + response);
                return null;
            }

			JSONObject json = new JSONObject(response);
			String accessToken = json.getString("access_token");
			if (!save(context, accessToken, json.optString("refresh_token", refreshToken))) return null;
			return accessToken;
        } catch (Exception e) {
            Log.e(TAG, "refreshAccessToken", e);
            return null;
        }
    }

    static void clear(Context context) {
        try {
            prefs(context).edit().remove("access_token").remove("refresh_token").apply();
        } catch (Exception e) {
            Log.e(TAG, "clear", e);
        }
    }
}
