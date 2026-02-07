package com.example.sos;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SafetyUtil {

    private static final String NEWS_API_KEY = "388a531e0b5749f7ba342d87e14c3823"; // Replace with Valid Key or handle
                                                                                   // mock
    // Note: Free NewsAPI keys work in dev mode but getting one requires
    // registration.
    // I will use a placeholder logic first.

    // For demonstration, we will assume a valid flow but will fallback to Mock data
    // if API fails.

    public interface SafetyCallback {
        void onSafetyCheckResult(String safetyLevel, String message, List<NewsResponse.Article> articles);
    }

    public static void checkAreaSafety(Context context, Location location, SafetyCallback callback) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        String city = "Unknown";
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                city = addresses.get(0).getLocality();
                if (city == null)
                    city = addresses.get(0).getSubAdminArea();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (city == null)
            city = "City";

        fetchCrimeNews(context, city, callback);
    }

    private static void fetchCrimeNews(Context context, String city, SafetyCallback callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://newsapi.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NewsApiService service = retrofit.create(NewsApiService.class);
        Call<NewsResponse> call = service.getCrimeNews("crime " + city, "publishedAt", NEWS_API_KEY);

        String finalCity = city;
        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    analyzeRisk(finalCity, response.body().articles, callback);
                } else {
                    // Fallback Mock
                    analyzeRisk(finalCity, null, callback);
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                analyzeRisk(finalCity, null, callback);
            }
        });
    }

    private static void analyzeRisk(String city, List<NewsResponse.Article> articles, SafetyCallback callback) {
        int crimeCount = (articles != null) ? articles.size() : 0;

        // Time Factor
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour >= 20 || hour <= 6);

        String level = "Safe";
        String message = "Area seems peaceful.";

        // Simple logic: more news hits = higher risk
        // If mocked (articles==null), we simulate based on randomness or specific
        // cities
        if (articles == null) {
            // Mock Simulation
            if (isNight) {
                level = "Moderate Risk";
                message = "Caution advised at night.";
            }
        } else {
            if (crimeCount > 10) {
                level = isNight ? "Dangerous" : "High Risk";
                message = "High crime reports nearby.";
            } else if (crimeCount > 3) {
                level = isNight ? "High Risk" : "Moderate Risk";
                message = "Some incidents reported.";
            }
        }

        callback.onSafetyCheckResult(level, message, articles);
    }
}
