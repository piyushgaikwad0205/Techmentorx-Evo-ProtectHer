//package com.example.sos;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import android.Manifest;
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.os.Bundle;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.LinearLayout;
//import android.widget.Toast;
//
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.button.MaterialButton;
//
//import java.util.ArrayList;
//
//public class ShowContact extends AppCompatActivity {
//    private static final int REQUEST_CALL_PHONE_PERMISSION = 1;
//    RecyclerView contactRecyclerView;
//    ArrayList<ContactModel> modelArrayList;
//    ContactRecyclerAdapter adapter;
//    DatabaseHelper helper;
//    MaterialToolbar appBar;
//    LinearLayout emptyStateLayout;
//    MaterialButton addFirstContactButton;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_show_contact);
//        appBar = findViewById(R.id.toolbar);
//        setSupportActionBar(appBar);
//        contactRecyclerView = findViewById(R.id.contactRecylerView);
//        contactRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//        helper = new DatabaseHelper(this);
//        modelArrayList = helper.fetchData();
//        adapter = new ContactRecyclerAdapter(this, modelArrayList);
//        contactRecyclerView.setAdapter(adapter);
//        emptyStateLayout = findViewById(R.id.emptyStateLayout);
//        addFirstContactButton = findViewById(R.id.addFirstContactButton);
//
//        addFirstContactButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(ShowContact.this, RegisterNumberActivity.class);
//                startActivity(intent);
//                finish();
//            }
//        });
//
//        if(modelArrayList.isEmpty()){
//            emptyStateLayout.setVisibility(View.VISIBLE);
//        } else {
//            emptyStateLayout.setVisibility(View.GONE);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//                Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show();
//            } else {
//                // Permission denied, check if a rationale is needed
//                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
//
//                    new AlertDialog.Builder(this).setTitle("Permission required").setMessage("We need permission for calling")
//                            .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    ActivityCompat.requestPermissions(ShowContact.this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_PERMISSION);
//                                }
//                            }).show();
//                } else {
//
//                    new AlertDialog.Builder(this).setTitle("Permission denied").setMessage("If you reject permission, you can't use this call service\n\n" +
//                                    "Please turn on Phone permission at [Setting] > [Permission]")
//                            .setPositiveButton("PROCEED", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    openAppSettings();
//                                }
//                            }).setNegativeButton("CLOSE", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    Toast.makeText(ShowContact.this, "Call permission denied", Toast.LENGTH_SHORT).show();
//                                    dialog.dismiss();
//                                }
//                            }).show();
////
//                }
//            }
//        }
//    }
//
//    private void performPhoneCall(String phoneNumber) {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
//            // Permission is not granted, request it
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_PERMISSION);
//        } else {
//            // Permission is already granted, make the phone call
//            Intent intent = new Intent(Intent.ACTION_CALL);
//            intent.setData(Uri.parse(phoneNumber));
//            startActivity(intent);
//        }
//    }
//
//    private void openAppSettings() {
//        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//        intent.setData(Uri.parse("package:" + getPackageName()));
//        startActivity(intent);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            // Handle back button press
//            onBackPressed(); // or finish();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//}

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
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ShowContact extends AppCompatActivity implements ContactRecyclerAdapter.OnContactDeleteListener {
    private static final int REQUEST_CALL_PHONE_PERMISSION = 1;
    private static final int REQUEST_READ_CONTACTS_PERMISSION = 2;
    private static final int PICK_CONTACT_REQUEST = 3;
    RecyclerView contactRecyclerView;
    ArrayList<ContactModel> modelArrayList;
    ContactRecyclerAdapter adapter;
    DatabaseHelper helper;
    MaterialToolbar appBar;
    LinearLayout emptyStateLayout;
    MaterialButton addFirstContactButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_contact);

        // Custom header handles title, no toolbar needed
        // appBar = findViewById(R.id.toolbar);
        // setSupportActionBar(appBar);
        contactRecyclerView = findViewById(R.id.contactRecylerView);
        contactRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        helper = new DatabaseHelper(this);
        modelArrayList = helper.fetchData();

        // Pass the delete listener to the adapter
        adapter = new ContactRecyclerAdapter(this, modelArrayList, this);
        contactRecyclerView.setAdapter(adapter);

        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        // addFirstContactButton = findViewById(R.id.addFirstContactButton); // Removed
        // from layout

        // Wired up the large Import button
        // Wired up the large Import button
        View btnAddContactLog = findViewById(R.id.btnAddContactLog);
        btnAddContactLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(ShowContact.this,
                        Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ShowContact.this,
                            new String[] { Manifest.permission.READ_CONTACTS }, REQUEST_READ_CONTACTS_PERMISSION);
                } else {
                    openContactPicker();
                }
            }
        });

        // FAB removed in new design
        // FloatingActionButton fabAddContact = findViewById(R.id.fabAddContact); ...

        updateEmptyState();
    }

    // Method to update empty state visibility
    private void updateEmptyState() {
        if (modelArrayList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            contactRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            contactRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Implement the callback method from adapter
    @Override
    public void onContactDeleted() {
        updateEmptyState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data
        modelArrayList.clear();
        modelArrayList.addAll(helper.fetchData());
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri contactUri = data.getData();
                String[] projection = new String[] { android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME };
                android.database.Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int numberIndex = cursor
                            .getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int nameIndex = cursor
                            .getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                    String number = cursor.getString(numberIndex);
                    String name = cursor.getString(nameIndex);

                    // Save to DB
                    // Generate default keyword from name (First Name)
                    String keyword = "";
                    if (name != null && !name.isEmpty()) {
                        keyword = name.split(" ")[0];
                    } else {
                        keyword = "Contact";
                    }

                    if (helper.insertDataFunc(name, number, keyword)) {
                        Toast.makeText(this, "Contact Imported: " + name, Toast.LENGTH_SHORT).show();
                        // Refresh list
                        modelArrayList.clear();
                        modelArrayList.addAll(helper.fetchData());
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    } else {
                        Toast.makeText(this, "Contact already exists or failed to save.", Toast.LENGTH_SHORT).show();
                    }
                    cursor.close();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, check if a rationale is needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
                    new AlertDialog.Builder(this).setTitle("Permission required")
                            .setMessage("We need permission for calling")
                            .setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(ShowContact.this,
                                            new String[] { Manifest.permission.CALL_PHONE },
                                            REQUEST_CALL_PHONE_PERMISSION);
                                }
                            }).show();
                } else {
                    new AlertDialog.Builder(this).setTitle("Permission denied")
                            .setMessage("If you reject permission, you can't use this call service\n\n" +
                                    "Please turn on Phone permission at [Setting] > [Permission]")
                            .setPositiveButton("PROCEED", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    openAppSettings();
                                }
                            }).setNegativeButton("CLOSE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(ShowContact.this, "Call permission denied", Toast.LENGTH_SHORT)
                                            .show();
                                    dialog.dismiss();
                                }
                            }).show();
                }

            }
        } else if (requestCode == REQUEST_READ_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactPicker();
            } else {
                Toast.makeText(this, "Permission Required to Import Contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performPhoneCall(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CALL_PHONE },
                    REQUEST_CALL_PHONE_PERMISSION);
        } else {
            // Permission is already granted, make the phone call
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse(phoneNumber));
            startActivity(intent);
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button press
            onBackPressed(); // or finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}