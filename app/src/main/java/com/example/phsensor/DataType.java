package com.example.phsensor;

public class DataType {
    private String username;

    private String email;
    private String data;
    private float ph;
    private float latitude;
    private float longitude;

    private String address;

    private boolean isShared;


    // Getters and setters
    public String getName() { return username; }
    public void setName(String name) { this.username = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public float getPh() { return ph; }
    public void setPh(float email) { this.ph = ph; }
    public float getLatitude() { return latitude; }
    public void setLatitude(float latitude) { this.latitude = latitude; }
    public float getLongitude() { return longitude; }
    public void setLongitude(float Longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean getShard() { return isShared; }
    public void setShard(boolean isShared) { this.isShared = isShared; }




}
