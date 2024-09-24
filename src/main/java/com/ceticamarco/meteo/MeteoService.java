package com.ceticamarco.meteo;

import com.ceticamarco.lambdatonic.Either;
import com.ceticamarco.lambdatonic.Left;
import com.ceticamarco.lambdatonic.Right;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Objects;

@Service
public class MeteoService {
    private final String API_KEY;
    private final HashMap<String, String> conditionsMap = new HashMap<>();
    private record WeatherResult(double fahrenheit_t, double celsius_t, String emoji) {}

    private Either<Error, double[]> getCityCoordinates(String city) throws IOException, InterruptedException {
        String api_uri = String.format("https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=5&appid=%s", city, API_KEY);
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api_uri))
                .GET()
                .build();

        HttpResponse<String> jsonRes = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (jsonRes.body().equals("[]")) {
            return new Left<>(new Error("Cannot find this city"));
        }

        double[] coordinates = new double[2];
        try {
            var objectMapper = new ObjectMapper();
            var arrayNode = objectMapper.readTree(jsonRes.body());
            var firstElement = arrayNode.get(0);

            var lat = firstElement.get("lat").asDouble();
            var lon = firstElement.get("lon").asDouble();
            coordinates[0] = lat;
            coordinates[1] = lon;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        httpClient.close();

        return new Right<>(coordinates);
    }

    private WeatherResult getCityWeather(double[] coordinates) throws IOException, InterruptedException {
        String api_uri = String.format("https://api.openweathermap.org/data/2.5/weather?units=metric&lat=%f&lon=%f&appid=%s",
                coordinates[0], coordinates[1], API_KEY);
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api_uri))
                .GET()
                .build();

        HttpResponse<String> jsonRes = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        WeatherResult weatherResult = new WeatherResult(0.0, 0.0, "");
        try {
            var objectMapper = new ObjectMapper();
            var root = objectMapper.readTree(jsonRes.body());

            var condition = root.get("weather").get(0).get("main").asText();
            var celsius_t = Math.round(root.get("main").get("temp").asDouble());
            var fahrenheit_t = Math.round((celsius_t * 1.8) + 32);

            weatherResult = new WeatherResult(fahrenheit_t, celsius_t, getEmojiFromCondition(condition));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        httpClient.close();

        return weatherResult;
    }

    private String getEmojiFromCondition(String condition) {
        LocalTime now = LocalTime.now();
        LocalTime evening = LocalTime.of(20, 0);
        LocalTime morning = LocalTime.of(7, 0);

        // If weather condition is "clear" and localtime is between 08:00 PM and 07:00 AM
        if (Objects.equals(condition, "Clear") && (now.isAfter(evening) || now.isBefore(morning))) {
            return "🌙";
        }

        return this.conditionsMap.get(condition);
    }


    public MeteoService() {
        // Read OpenWeatherMap API token
        this.API_KEY = System.getenv("METEO_TOKEN");
        // Set condition-emoji hashmap
        this.conditionsMap.put("Thunderstorm", "⛈️");
        this.conditionsMap.put("Drizzle", "🌦 ");
        this.conditionsMap.put("Rain", "🌧 ");
        this.conditionsMap.put("Snow", "☃️");
        this.conditionsMap.put("Mist", "💭");
        this.conditionsMap.put("Smoke", "💭");
        this.conditionsMap.put("Haze", "💭");
        this.conditionsMap.put("Dust", "💭");
        this.conditionsMap.put("Fog", "💭");
        this.conditionsMap.put("Sand", "💭");
        this.conditionsMap.put("Ash", "💭");
        this.conditionsMap.put("Squall", "💭");
        this.conditionsMap.put("Tornado", "🌪 ");
        this.conditionsMap.put("Clear", "☀️");
        this.conditionsMap.put("Clouds", "☁️");
    }

    public Either<Error, String> getWeather(String city, Units units) throws IOException, InterruptedException {
        // TODO: Check the cache
        var coordinatesEither = getCityCoordinates(city);
        if(coordinatesEither.isLeft()) {
            return new Left<>(coordinatesEither.fromLeft(new Error("")));
        }

        var weatherResult = getCityWeather(coordinatesEither.fromRight(new double[]{}));
        // TODO: Save value onto the cache
        var emoji = weatherResult.emoji;
        var temperature = units == Units.METRIC ? weatherResult.celsius_t : weatherResult.fahrenheit_t;
        var fmt_temp = temperature > 0.0 ? String.format("+%d", (int)temperature) : String.valueOf((int)temperature);
        var fmt_units = units == Units.METRIC ? 'C' : 'F';
        var fmt_weather = String.format("%s %s °%c\n", emoji, fmt_temp, fmt_units);

        return new Right<>(fmt_weather);
    }
}
