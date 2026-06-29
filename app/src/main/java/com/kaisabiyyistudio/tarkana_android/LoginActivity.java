package com.kaisabiyyistudio.tarkana_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private boolean passwordVisible = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        ImageButton btnToggle = findViewById(R.id.btn_toggle_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvPrompt = findViewById(R.id.tv_register_prompt);

        // Ponytail: Avoid heavy splash screens. Quick token check on main thread UI init.
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    this, "auth_prefs", masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            if (prefs.contains("access_token")) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }
        } catch (Exception e) {
            Log.e("LoginActivity", "Auth pref check failed", e);
        }

        btnToggle.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggle.setImageResource(R.drawable.ic_eye_off);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggle.setImageResource(R.drawable.ic_eye);
            }
            etPassword.setSelection(etPassword.length());
        });

        tvPrompt.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            btnLogin.setEnabled(false);
            btnLogin.setText(R.string.login_wait);
            
            authenticate("/auth/v1/token?grant_type=password", email, password, btnLogin);
        });

        findViewById(R.id.btn_google).setOnClickListener(v ->
                Toast.makeText(this, "Google login not implemented yet", Toast.LENGTH_SHORT).show());
    }

    private void authenticate(String path, String email, String password, Button btnLogin) {
        executor.execute(() -> {
            try {
                URL url = new URL(BuildConfig.SUPABASE_URL + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes());
                }

                int code = conn.getResponseCode();
                InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";

                if (code >= 400) {
                    JSONObject err = new JSONObject(response);
                    String msg = err.optString("error_description", err.optString("msg", "Error " + code));
                    handler.post(() -> {
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        resetButton(btnLogin);
                    });
				} else {
					JSONObject res = new JSONObject(response);
					String token = res.getString("access_token");
					String refreshToken = res.optString("refresh_token", "");
					if (!AuthSession.save(this, token, refreshToken)) {
						handler.post(() -> {
							Toast.makeText(this, "Failed to save session. Please try again.", Toast.LENGTH_LONG).show();
							resetButton(btnLogin);
						});
						return;
					}
					String savedToken = AuthSession.accessToken(this);
					Log.d("LoginActivity", "Session saved. accessTokenReadable=" + (savedToken != null) +
							", refreshTokenPresent=" + !refreshToken.isEmpty());
					handler.post(() -> {
						startActivity(new Intent(this, MainActivity.class));
						finish();
					});
                }
            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetButton(btnLogin);
                });
            }
        });
    }

    private void resetButton(Button btnLogin) {
        btnLogin.setEnabled(true);
        btnLogin.setText(R.string.login_button);
    }
}
