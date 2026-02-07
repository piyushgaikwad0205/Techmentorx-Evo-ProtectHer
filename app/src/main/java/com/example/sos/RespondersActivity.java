package com.example.sos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RespondersActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private RecyclerView recyclerViewResponders;
    private LinearLayout layoutEmpty;

    private String alertId;
    private Location currentLocation;

    private FirebaseAuth mAuth;
    private DatabaseReference responsesRef;
    private DatabaseReference usersRef;
    private ValueEventListener responsesListener;

    private List<ResponderModel> respondersList;
    private RespondersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_responders);

        // Get alert ID
        alertId = getIntent().getStringExtra("alertId");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        responsesRef = databaseRef.child("alert_responses");
        usersRef = databaseRef.child("user_locations");

        // Initialize views
        topAppBar = findViewById(R.id.topAppBar);
        recyclerViewResponders = findViewById(R.id.recyclerViewResponders);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        topAppBar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup RecyclerView
        respondersList = new ArrayList<>();
        adapter = new RespondersAdapter(respondersList, this::openChat);
        recyclerViewResponders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewResponders.setAdapter(adapter);

        // Load responders
        loadResponders();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadResponders() {
        if (alertId == null) {
            showEmptyState();
            return;
        }

        responsesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                respondersList.clear();

                for (DataSnapshot responseSnapshot : snapshot.getChildren()) {
                    String responderId = responseSnapshot.getKey();
                    String status = responseSnapshot.child("status").getValue(String.class);
                    Double lat = responseSnapshot.child("latitude").getValue(Double.class);
                    Double lng = responseSnapshot.child("longitude").getValue(Double.class);
                    Long timestamp = responseSnapshot.child("timestamp").getValue(Long.class);

                    if (responderId != null && status != null) {
                        ResponderModel responder = new ResponderModel();
                        responder.setUserId(responderId);
                        responder.setStatus(status);
                        responder.setLatitude(lat != null ? lat : 0.0);
                        responder.setLongitude(lng != null ? lng : 0.0);
                        responder.setTimestamp(timestamp != null ? timestamp : 0L);

                        // Calculate distance if location available
                        if (currentLocation != null && lat != null && lng != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(
                                    currentLocation.getLatitude(),
                                    currentLocation.getLongitude(),
                                    lat, lng, results);
                            responder.setDistance(results[0]);
                        }

                        respondersList.add(responder);
                    }
                }

                if (respondersList.isEmpty()) {
                    showEmptyState();
                } else {
                    showResponders();
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RespondersActivity.this,
                        "Error loading responders", Toast.LENGTH_SHORT).show();
            }
        };

        responsesRef.child(alertId).addValueEventListener(responsesListener);
    }

    private void showEmptyState() {
        recyclerViewResponders.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    private void showResponders() {
        recyclerViewResponders.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void openChat(ResponderModel responder) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("alertId", alertId);
        intent.putExtra("responderId", responder.getUserId());
        intent.putExtra("responderName", "Helper");
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (responsesListener != null && alertId != null) {
            responsesRef.child(alertId).removeEventListener(responsesListener);
        }
    }
}
