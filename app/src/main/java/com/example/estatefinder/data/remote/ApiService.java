package com.example.estatefinder.data.remote;

import com.example.estatefinder.data.remote.dto.PropertyResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("properties")
    Call<List<PropertyResponse>> getAllProperties();
}