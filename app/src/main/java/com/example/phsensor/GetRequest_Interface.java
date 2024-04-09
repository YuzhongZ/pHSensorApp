package com.example.phsensor;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GetRequest_Interface {
        @POST("users/add")
        Call<DataType> createUser(@Body DataType data);
}
