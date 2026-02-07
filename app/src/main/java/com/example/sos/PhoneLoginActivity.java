package com.example.sos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    private TextInputEditText etPhone, etOtp;
    private TextInputLayout tilOtp;
    private MaterialButton btnSendOtp, btnVerify;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        mAuth = FirebaseAuth.getInstance();

        etPhone = findViewById(R.id.etPhone);
        etOtp = findViewById(R.id.etOtp);
        tilOtp = findViewById(R.id.tilOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerify = findViewById(R.id.btnVerify);
        progressBar = findViewById(R.id.progressBar);

        btnSendOtp.setOnClickListener(v -> {
            String mobile = etPhone.getText().toString().trim();
            if (mobile.isEmpty() || mobile.length() < 10) {
                etPhone.setError("Valid number required");
                etPhone.requestFocus();
                return;
            }
            // Add default country code if missing
            if (!mobile.startsWith("+")) {
                mobile = "+91" + mobile; // Default to India, change as needed
            }

            startPhoneNumberVerification(mobile);
        });

        btnVerify.setOnClickListener(v -> {
            String code = etOtp.getText().toString().trim();
            if (code.isEmpty() || code.length() < 6) {
                etOtp.setError("Valid code required");
                etOtp.requestFocus();
                return;
            }
            verifyPhoneNumberWithCode(mVerificationId, code);
        });
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        progressBar.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber) // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this) // Activity (for callback binding)
                .setCallbacks(mCallbacks) // OnVerificationStateChangedCallbacks
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            // verified without needing to send or enter an OTP.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            // detect the incoming verification SMS and perform verification without
            // user action.
            progressBar.setVisibility(View.GONE);
            signInWithPhoneAuthCredential(credential);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            progressBar.setVisibility(View.GONE);
            btnSendOtp.setEnabled(true);
            Toast.makeText(PhoneLoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(@NonNull String verificationId,
                @NonNull PhoneAuthProvider.ForceResendingToken token) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            progressBar.setVisibility(View.GONE);

            // Save verification ID and resending token so we can use them later
            mVerificationId = verificationId;
            mResendToken = token;

            // Update UI to show OTP field
            tilOtp.setVisibility(View.VISIBLE);
            btnVerify.setVisibility(View.VISIBLE);
            btnSendOtp.setVisibility(View.GONE);
            etPhone.setEnabled(false);

            Toast.makeText(PhoneLoginActivity.this, "OTP Sent!", Toast.LENGTH_SHORT).show();
        }
    };

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Update Device ID
                        String uid = mAuth.getCurrentUser().getUid();
                        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(),
                                android.provider.Settings.Secure.ANDROID_ID);
                        java.util.Map<String, Object> update = new java.util.HashMap<>();
                        update.put("currentDeviceId", deviceId);
                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                                .set(update, com.google.firebase.firestore.SetOptions.merge());

                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(PhoneLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(PhoneLoginActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Sign in failed, display a message and update the UI
                        if (task.getException() != null) {
                            Toast.makeText(PhoneLoginActivity.this, "Login Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
