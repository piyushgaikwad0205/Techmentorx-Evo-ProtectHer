package com.example.sos;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {
    @GET("v2/everything")
    Call<NewsResponse> getCrimeNews(
            @Query("q") String query,
            @Query("sortBy") String sortBy,
            @Query("apiKey") String apiKey);
}
