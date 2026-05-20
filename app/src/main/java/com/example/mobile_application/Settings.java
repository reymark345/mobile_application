package com.example.mobile_application;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class Settings extends AppCompatActivity {
    private ImageDbHelper dbHelper;
    private EditText baseUrlEditText;
    private TextInputLayout baseUrlInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new ImageDbHelper(this);
        baseUrlInputLayout = findViewById(R.id.cc_CurrentPass);
        baseUrlEditText = findViewById(R.id.edt_base_url);
        Button updateButton = findViewById(R.id.updateChangePassword);

        String savedBaseUrl = normalizeBaseUrl(dbHelper.getBaseUrl());
        baseUrlEditText.setText(savedBaseUrl != null ? savedBaseUrl : ImageDbHelper.DEFAULT_BASE_URL);
        updateButton.setOnClickListener(view -> saveBaseUrl());
    }

    private void saveBaseUrl() {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrlEditText.getText().toString());

        if (normalizedBaseUrl == null) {
            baseUrlInputLayout.setError("Enter a valid URL, eg. http://192.31.246.38");
            return;
        }

        baseUrlInputLayout.setError(null);

        if (dbHelper.saveBaseUrl(normalizedBaseUrl)) {
            baseUrlEditText.setText(normalizedBaseUrl);
            Toast.makeText(this, "Base URL updated. Port 5000 will be used.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update Base URL.", Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizeBaseUrl(String input) {
        if (input == null) {
            return null;
        }

        String baseUrl = input.trim();
        Uri parsedUrl = Uri.parse(baseUrl);
        String scheme = parsedUrl.getScheme();
        String host = parsedUrl.getHost();

        if (scheme == null
                || host == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return null;
        }

        return new Uri.Builder()
                .scheme(scheme.toLowerCase(Locale.US))
                .encodedAuthority(formatHostForAuthority(host))
                .build()
                .toString();
    }

    private String formatHostForAuthority(String host) {
        if (host.contains(":") && !host.startsWith("[")) {
            return "[" + host + "]";
        }

        return host;
    }
}
