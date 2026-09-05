package com.example.estatefinder.data.remote;

import com.example.estatefinder.data.remote.dto.PropertyResponse;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RemoteDataSource {

    private static final String BASE_URL = "http://10.0.2.2:8080/";

    private final ApiService apiService;

    public RemoteDataSource() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    public List<PropertyResponse> fetchAllProperties() throws IOException {
        Call<List<PropertyResponse>> call = apiService.getAllProperties();
        Response<List<PropertyResponse>> response = call.execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected response code: " + response.code());
        }
        List<PropertyResponse> body = response.body();
        return body != null ? body : List.of();
    }
}