package com.example.sos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class SosCall extends AppCompatActivity implements HelplineAdapter.OnHelplineActionListener {

    private static final int REQUEST_CALL_PHONE_PERMISSION = 1;
    MaterialToolbar appBar;
    RecyclerView recyclerView;
    HelplineAdapter adapter;
    DatabaseHelper db;
    ArrayList<HelplineModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soscall);

        appBar = findViewById(R.id.toolbar);
        setSupportActionBar(appBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.helplineRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = new DatabaseHelper(this);
        loadHelplines();

        com.google.android.material.floatingactionbutton.FloatingActionButton btnAdd = findViewById(
                R.id.btnAddHelpline);
        btnAdd.setOnClickListener(v -> showAddDialog());
    }

    private void loadHelplines() {
        list = db.fetchHelplineData();
        adapter = new HelplineAdapter(this, list, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onCall(String number) {
        performPhoneCall("tel:" + number);
    }

    @Override
    public void onEdit(HelplineModel model) {
        showEditDialog(model);
    }

    private void showEditDialog(HelplineModel model) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_helpline, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etNumber = dialogView.findViewById(R.id.etNumber);
        EditText etKeyword = dialogView.findViewById(R.id.etKeyword);
        EditText etCustomMessage = dialogView.findViewById(R.id.etCustomMessage);

        etName.setText(model.getName());
        etNumber.setText(model.getNumber());
        etKeyword.setText(model.getKeyword());
        etKeyword.setText(model.getKeyword());
        etCustomMessage.setText(model.getCustomMessage());

        new MaterialAlertDialogBuilder(this,
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle("Edit Helpline")
                .setView(dialogView)
                .setBackground(getDrawable(R.drawable.bg_dialog_rounded))
                .setPositiveButton("Update", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String number = etNumber.getText().toString().trim();
                    String keyword = etKeyword.getText().toString().trim();
                    String message = etCustomMessage.getText().toString().trim();

                    if (name.isEmpty() || number.isEmpty() || keyword.isEmpty()) {
                        Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (db.updateHelpline(model.getId(), name, number, keyword, message)) {
                        Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
                        loadHelplines(); // Refresh list
                    } else {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_helpline, null);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etNumber = dialogView.findViewById(R.id.etNumber);
        EditText etKeyword = dialogView.findViewById(R.id.etKeyword);
        EditText etCustomMessage = dialogView.findViewById(R.id.etCustomMessage);

        new MaterialAlertDialogBuilder(this,
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setTitle("Add Helpline")
                .setView(dialogView)
                .setBackground(getDrawable(R.drawable.bg_dialog_rounded)) // Force white rounded background
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String number = etNumber.getText().toString().trim();
                    String keyword = etKeyword.getText().toString().trim();
                    String message = etCustomMessage.getText().toString().trim();

                    if (name.isEmpty() || number.isEmpty() || keyword.isEmpty()) {
                        Toast.makeText(this, "Name, Number and Keyword are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (db.insertHelpline(name, number, keyword, message)) {
                        Toast.makeText(this, "Helpline added successfully", Toast.LENGTH_SHORT).show();
                        loadHelplines();
                    } else {
                        Toast.makeText(this, "Failed to add helpline", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performPhoneCall(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CALL_PHONE },
                    REQUEST_CALL_PHONE_PERMISSION);
        } else {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse(phoneNumber));
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                    new AlertDialog.Builder(this).setTitle("Permission required")
                            .setMessage("We need permission for calling")
                            .setPositiveButton("CONFIRM",
                                    (dialog, which) -> ActivityCompat.requestPermissions(SosCall.this,
                                            new String[] { Manifest.permission.CALL_PHONE },
                                            REQUEST_CALL_PHONE_PERMISSION))
                            .show();
                } else {
                    new AlertDialog.Builder(this).setTitle("Permission denied")
                            .setMessage("Please turn on Phone permission at [Setting] > [Permission]")
                            .setPositiveButton("PROCEED", (dialog, which) -> {
                                Intent intent = new Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }).setNegativeButton("CLOSE", (dialog, which) -> dialog.dismiss()).show();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}