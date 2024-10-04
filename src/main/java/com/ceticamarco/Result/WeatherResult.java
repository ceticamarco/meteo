package com.ceticamarco.Result;

public record WeatherResult(double fahrenheit_temp,
                            double celsius_temp,
                            String emoji)
        implements IResult { }