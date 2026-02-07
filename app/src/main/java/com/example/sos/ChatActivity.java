package com.example.sos;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class ChatActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private String alertId;
    private String responderId;
    private String responderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent
        alertId = getIntent().getStringExtra("alertId");
        responderId = getIntent().getStringExtra("responderId");
        responderName = getIntent().getStringExtra("responderName");

        // Initialize views
        topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setTitle("Chat with " + responderName);
            topAppBar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // TODO: Implement real-time chat functionality
        Toast.makeText(this, "Chat feature coming soon!", Toast.LENGTH_SHORT).show();
    }
}
