package com.kaisabiyyistudio.tarkana_android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText etDisplayName = findViewById(R.id.et_display_name);
        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        ImageButton btnToggle = findViewById(R.id.btn_toggle_password);
        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvPrompt = findViewById(R.id.tv_login_prompt);

        btnToggle.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggle.setImageResource(R.drawable.ic_eye);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggle.setImageResource(R.drawable.ic_eye_off);
            }
            etPassword.setSelection(etPassword.length());
        });

        tvPrompt.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String displayName = etDisplayName.getText().toString().trim();
            
            if (email.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            btnRegister.setEnabled(false);
            btnRegister.setText(R.string.login_wait);
            
            authenticate(email, password, displayName, btnRegister);
        });

        findViewById(R.id.btn_google).setOnClickListener(v ->
                Toast.makeText(this, "Google login not implemented yet", Toast.LENGTH_SHORT).show());
    }

    private void authenticate(String email, String password, String displayName, Button btnRegister) {
        executor.execute(() -> {
            try {
                URL url = new URL(BuildConfig.SUPABASE_URL + "/auth/v1/signup");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                
                JSONObject options = new JSONObject();
                JSONObject data = new JSONObject();
                data.put("display_name", displayName);
                options.put("data", data);
                body.put("options", options);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes());
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    handler.post(() -> {
                        Toast.makeText(this, R.string.register_success_msg, Toast.LENGTH_LONG).show();
                        finish(); // Go back to login
                    });
                } else {
                    java.io.InputStream es = conn.getErrorStream();
                    String errorMsg = "Error: " + responseCode;
                    if (es != null) {
                        java.util.Scanner scanner = new java.util.Scanner(es).useDelimiter("\\A");
                        String response = scanner.hasNext() ? scanner.next() : "";
                        Log.e("RegisterActivity", "Signup failed: " + response);
                        try {
                            JSONObject errObj = new JSONObject(response);
                            errorMsg = errObj.optString("msg", errObj.optString("error_description", errorMsg));
                        } catch (Exception ignored) {}
                    }
                    String finalErrorMsg = errorMsg;
                    handler.post(() -> {
                        Toast.makeText(this, finalErrorMsg, Toast.LENGTH_LONG).show();
                        btnRegister.setEnabled(true);
                        btnRegister.setText(R.string.register_button);
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                    btnRegister.setEnabled(true);
                    btnRegister.setText(R.string.register_button);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
