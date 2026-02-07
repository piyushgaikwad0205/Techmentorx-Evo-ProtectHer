package com.example.sos;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class GeminiSafetyHelper {

    private static final String TAG = "GeminiSafetyHelper";
    // NOTE: In a real production app, this key should be stored in BuildConfig or a
    // secure backend.
    private static final String API_KEY = "AIzaSyD82LZe2qm_H_0aDbOoTRFGN7TouJNchh4";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";

    public interface GeminiService {
        @POST
        Call<JsonObject> generateContent(@retrofit2.http.Url String url, @Body JsonObject body);
    }

    private GeminiService service;
    private SafetyResultListener listener;

    public interface SafetyResultListener {
        void onSafetyResult(String level, String reason);
    }

    public GeminiSafetyHelper(SafetyResultListener listener) {
        this.listener = listener;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GeminiService.class);
    }

    public void assessSafety(String areaType, String timeOfDay, String sosActivity, String lighting, String distance) {
        // Construct Prompt
        String prompt = "Analyze the safety of this area based on these signals: \n" +
                "- Area Type: " + areaType + "\n" +
                "- Time of Day: " + timeOfDay + "\n" +
                "- Recent SOS Activity: " + sosActivity + "\n" +
                "- Street Lighting: " + lighting + "\n" +
                "- Distance to Emergency Services: " + distance + "\n\n" +
                "Task: Act as a safety intelligence assistant.\n" +
                "Output Format: Return EXACTLY and ONLY this format:\n" +
                "Safety Level: <Safe | Moderate | Risky>\n" +
                "Reason: <One very short line (max 8-10 words) with key risk terms in bold like **Night** or **Isolated**>";

        // Construct JSON Body
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        // Construct Full URL manually to avoid Retrofit colon encoding issues
        String fullUrl = BASE_URL + "v1beta/models/gemini-pro:generateContent?key=" + API_KEY;

        // Execute Call
        service.generateContent(fullUrl, requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Parse simple text response
                        JsonObject result = response.body();
                        JsonArray candidates = result.getAsJsonArray("candidates");
                        if (candidates != null && candidates.size() > 0) {
                            JsonObject candidate = candidates.get(0).getAsJsonObject();
                            JsonObject content = candidate.getAsJsonObject("content");
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts != null && parts.size() > 0) {
                                String text = parts.get(0).getAsJsonObject().get("text").getAsString();
                                parseAndNotify(text);
                            } else {
                                notifyError("No content parts");
                            }
                        } else {
                            notifyError("No candidates returned");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parsing error", e);
                        notifyError("Parsing Error");
                    }
                } else {
                    Log.e(TAG, "API Error: " + response.code());
                    notifyError("API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Network Failure", t);
                notifyError("Network Failure: " + t.getMessage());
            }
        });
    }

    private void notifyError(String errorMsg) {
        if (listener != null) {
            listener.onSafetyResult("Error", errorMsg);
        }
    }

    private void parseAndNotify(String fullText) {
        // Expected:
        // Safety Level: Risky
        // Reason: Poor lighting and isolated area.
        String level = "Unknown";
        String reason = "Could not parse reason.";

        // Use regex for more robust matching
        // Match "Safety Level" or "Threat Level" followed by optional colon, spaces,
        // and captue the word
        Pattern levelPattern = Pattern.compile("(?:Safety|Threat) Level[:\\s-*]+([A-Za-z]+)", Pattern.CASE_INSENSITIVE);
        Matcher levelMatcher = levelPattern.matcher(fullText);
        if (levelMatcher.find()) {
            level = levelMatcher.group(1).trim();
        }

        // Match "Reason" followed by optional colon, spaces, and capture the rest of
        // the line
        Pattern reasonPattern = Pattern.compile("Reason[:\\s-*]+(.*?)(?=\\n|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher reasonMatcher = reasonPattern.matcher(fullText);
        if (reasonMatcher.find()) {
            reason = reasonMatcher.group(1).trim();
        } else {
            // Fallback: if we found a level but no "Reason:" label, maybe the rest is the
            // reason
            if (!level.equals("Unknown")) {
                // Try to find the line after the level logic
                String[] lines = fullText.split("\n");
                for (String line : lines) {
                    if (!line.toLowerCase().contains("safety level") && !line.toLowerCase().contains("threat level")
                            && line.trim().length() > 0) {
                        if (reason.equals("Could not parse reason."))
                            reason = line.trim();
                        else
                            reason += " " + line.trim();
                    }
                }
            } else {
                reason = fullText; // If nothing matched, just return the whole text (likely an error message or
                                   // raw output)
            }
        }

        if (listener != null) {
            // Cap reason length for UI
            if (reason.length() > 100)
                reason = reason.substring(0, 97) + "...";
            listener.onSafetyResult(level, reason);
        }
    }
}
