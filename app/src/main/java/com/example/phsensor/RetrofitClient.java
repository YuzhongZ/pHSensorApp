package com.example.phsensor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://18.212.144.114/";
    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            //.addConverterFactory(GsonConverterFactory.create())
            .build();

    public static GetRequest_Interface getApiService() {
        return retrofit.create(GetRequest_Interface.class);
    }
}
