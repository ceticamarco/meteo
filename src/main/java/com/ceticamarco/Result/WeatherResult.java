package com.ceticamarco.Result;

public final class WeatherResult implements IResult {
    public double fahrenheit_t;
    public double celsius_t;
    public String emoji;

    public WeatherResult(double fahrenheit_t, double celsius_t, String emoji) {
        this.fahrenheit_t = fahrenheit_t;
        this.celsius_t = celsius_t;
        this.emoji = emoji;
    }
}
