package com.example.sos;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class HistoryActivity extends AppCompatActivity {

    RecyclerView recyclerHistory;
    HistoryAdapter adapter;
    DatabaseHelper db;
    LinearLayout emptyState;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Set light theme
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(android.R.color.holo_red_light));
            getWindow().getDecorView().setSystemUiVisibility(0); // Dark icons
        }

        btnBack = findViewById(R.id.btnBack);
        recyclerHistory = findViewById(R.id.recyclerHistory);
        emptyState = findViewById(R.id.emptyState);

        btnBack.setOnClickListener(v -> onBackPressed());

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        db = new DatabaseHelper(this);
        loadHistory();
    }

    private void loadHistory() {
        ArrayList<HashMap<String, String>> list = db.fetchHistory();
        if (list.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerHistory.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerHistory.setVisibility(View.VISIBLE);
            adapter = new HistoryAdapter(this, list);
            recyclerHistory.setAdapter(adapter);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
