package com.kaisabiyyistudio.tarkana_android;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    private boolean passwordVisible = false;

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

        EditText etPassword = findViewById(R.id.et_password);
        ImageButton btnToggle = findViewById(R.id.btn_toggle_password);

        // Password visibility toggle
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

        // MASUK → navigate to MainActivity
        findViewById(R.id.btn_login).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });

        // Google login
        findViewById(R.id.btn_google).setOnClickListener(v ->
                Toast.makeText(this, "Google login clicked", Toast.LENGTH_SHORT).show());

        // Register link
        findViewById(R.id.tv_register_prompt).setOnClickListener(v ->
                Toast.makeText(this, "Register clicked", Toast.LENGTH_SHORT).show());
    }
}
