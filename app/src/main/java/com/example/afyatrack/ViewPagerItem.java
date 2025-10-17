package com.example.afyatrack;

public class ViewPagerItem {
    private int lottieRawRes;
    private int imageRes;
    private String title;
    private String description;

    // Constructor for Lottie animations
    public ViewPagerItem(int lottieRawRes, String title, String description) {
        this.lottieRawRes = lottieRawRes;
        this.imageRes = 0;
        this.title = title;
        this.description = description;
    }

    // Constructor for static images
    public ViewPagerItem(String title, String description, int imageRes) {
        this.lottieRawRes = 0;
        this.imageRes = imageRes;
        this.title = title;
        this.description = description;
    }

    public int getLottieRawRes() {
        return lottieRawRes;
    }

    public int getImageRes() {
        return imageRes;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}