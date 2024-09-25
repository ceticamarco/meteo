package com.ceticamarco.meteo;

import com.ceticamarco.Result.HumidityResult;
import com.ceticamarco.Result.WeatherResult;
import com.ceticamarco.Result.WindResult;
import com.ceticamarco.cache.Cache;
import com.ceticamarco.lambdatonic.Either;
import com.ceticamarco.lambdatonic.Left;
import com.ceticamarco.lambdatonic.Right;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final Cache cache;
    private final String API_KEY;
    private final HashMap<String, String> conditionsMap = new HashMap<>();

    private Either<Error, double[]> getCityCoordinates(String city) throws IOException, InterruptedException {
        String api_uri = String.format("https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=5&appid=%s", city, API_KEY);
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api_uri))
                .GET()
                .build();

        HttpResponse<String> jsonRes = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (jsonRes.body().equals("[]")) {
            return new Left<>(new Error("Cannot find this city\n"));
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

    public HumidityResult getCityHumidity(double[] coordinates) throws IOException, InterruptedException {
        String api_uri = String.format("https://api.openweathermap.org/data/2.5/weather?units=metric&lat=%f&lon=%f&appid=%s",
                coordinates[0], coordinates[1], API_KEY);
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api_uri))
                .GET()
                .build();

        HttpResponse<String> jsonRes = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        HumidityResult humidity = new HumidityResult("");
        try {
            var objectMapper = new ObjectMapper();
            var root = objectMapper.readTree(jsonRes.body());

            humidity = new HumidityResult(root.get("main").get("humidity").asText());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        httpClient.close();

        return humidity;
    }

    private WindResult getCityWind(double[] coordinates) throws IOException, InterruptedException {
        String api_uri = String.format("https://api.openweathermap.org/data/2.5/weather?units=metric&lat=%f&lon=%f&appid=%s",
                coordinates[0], coordinates[1], API_KEY);
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api_uri))
                .GET()
                .build();

        HttpResponse<String> jsonRes = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        double windSpeed = 0;
        int windDegree = 0;
        try {
            var objectMapper = new ObjectMapper();
            var root = objectMapper.readTree(jsonRes.body());

            windSpeed = root.get("wind").get("speed").asDouble();
            windDegree = root.get("wind").get("deg").asInt();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Map wind degree to cardinal direction
        // Each cardinal direction represent a segment of 22.5 degrees
        String[][] cardinalDirections = {
            {"N", "↓"},   // 0/360 DEG
            {"NNE", "↙"}, // 22.5 DEG
            {"NE",  "↙"}, // 45 DEG
            {"ENE", "↙"}, // 67.5 DEG
            {"E",   "←"}, // 90 DEG
            {"ESE", "↖"}, // 112.5 DEG
            {"SE",  "↖"}, // 135 DEG
            {"SSE", "↖"}, // 157.5 DEG
            {"S",   "↑"}, // 180 DEG
            {"SSW", "↗"}, // 202.5 DEG
            {"SW",  "↗"}, // 225 DEG
            {"WSW", "↗"}, // 247.5 DEG
            {"W",   "→"}, // 270 DEG
            {"WNW", "↘"}, // 292.5 DEG
            {"NW",  "↘"}, // 315 DEG
            {"NNW", "↘"}, // 337.5 DEG
        };

        // Computes "idx ≡ round(wind_deg / 22.5) (mod 16)"
        // to ensure that values above 360 degrees or below 0 degrees
        // "stay" bounded to the map
        var idx = (int)Math.round(windDegree / 22.5) % 16;
        var windDirection = cardinalDirections[idx][0];
        var windIcon = cardinalDirections[idx][1];

        httpClient.close();

        return new WindResult(windSpeed, windDirection, windIcon);
    }

    @Autowired
    public MeteoService(@Value("${meteo.cache.ttl}") int ttl, @Value("${meteo.token}") String token) {
        this.cache = new Cache(ttl);
        // Read OpenWeatherMap API token
        this.API_KEY = token;
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
        WeatherResult weatherResult;

        // Check whether the value exists in the cache
        if(this.cache.getValue(String.format("%s_weather", city)).isPresent()) {
            weatherResult = (WeatherResult)this.cache.getValue(String.format("%s_weather", city)).get();
        } else {
            var coordinatesEither = getCityCoordinates(city);
            if (coordinatesEither.isLeft()) {
                return new Left<>(coordinatesEither.fromLeft(new Error("")));
            }
            weatherResult = getCityWeather(coordinatesEither.fromRight(new double[]{}));
            this.cache.addValue(String.format("%s_weather", city), weatherResult);
        }

        var emoji = weatherResult.emoji;
        var temperature = units == Units.METRIC ? weatherResult.celsius_t : weatherResult.fahrenheit_t;
        var fmt_temp = temperature > 0.0 ? String.format("+%d", (int)temperature) : String.valueOf((int)temperature);
        var fmt_units = units == Units.METRIC ? 'C' : 'F';
        var fmt_weather = String.format("%s %s°%c\n", emoji, fmt_temp, fmt_units);

        return new Right<>(fmt_weather);
    }

    public Either<Error, String> getHumidity(String city) throws IOException, InterruptedException {
        HumidityResult humidityResult;

        // Check whether the value exists in the cache
        if(this.cache.getValue(String.format("%s_humidity", city)).isPresent()) {
            humidityResult = (HumidityResult)this.cache.getValue(String.format("%s_humidity", city)).get();
        } else {
            var coordinatesEither = getCityCoordinates(city);
            if (coordinatesEither.isLeft()) {
                return new Left<>(coordinatesEither.fromLeft(new Error("")));
            }

            humidityResult = getCityHumidity(coordinatesEither.fromRight(new double[]{}));
            this.cache.addValue(String.format("%s_humidity", city), humidityResult);
        }

        var humidity = humidityResult.humidity;
        humidity += "%\n";

        return new Right<>(humidity);
    }

    public Either<Error, String> getWind(String city, Units units) throws IOException, InterruptedException {
        WindResult windResult;

        // Check whether the value exists in the cache
        if(this.cache.getValue(String.format("%s_wind", city)).isPresent()) {
            windResult = (WindResult) this.cache.getValue(String.format("%s_wind", city)).get();
        } else {
            var coordinatesEither = getCityCoordinates(city);
            if (coordinatesEither.isLeft()) {
                return new Left<>(coordinatesEither.fromLeft(new Error("")));
            }

            windResult = getCityWind(coordinatesEither.fromRight(new double[]{}));
            this.cache.addValue(String.format("%s_wind", city), windResult);
        }

        // Convert wind speed(by default represented in m/s) according to the unit of measurement
        // 1 m/s = 2.23694 mph
        // 1 m/s = 3.6 kph
        var wind_speed = windResult.speed;
        wind_speed = units == Units.IMPERIAL ? (wind_speed * 2.23694) : (wind_speed * 3.6);

        var wind_direction = windResult.direction;
        var wind_icon = windResult.icon;

        var fmt_wind = String.format("%.2f%s %s %s\n",
                wind_speed,
                units == Units.IMPERIAL ? "mph" : "km/h",
                wind_direction,
                wind_icon
        );

        return new Right<>(fmt_wind);
    }

    public Either<Error, String> getReport(String city, Units units, boolean toJson) throws IOException, InterruptedException {
        // Get weather, humidity and wind
        var weatherEither = getWeather(city, units);
        var humidityEither = getHumidity(city);
        var windEither = getWind(city, units);

        // Check for errors
        if(weatherEither.isLeft()) {
            return weatherEither;
        } else if(humidityEither.isLeft()) {
            return humidityEither;
        } else if(windEither.isLeft()) {
            return windEither;
        }

        // Extract the actual value
        var weather = weatherEither.fromRight("").replace("\n", "");
        var humidity = humidityEither.fromRight("").replace("\n", "");
        var wind = windEither.fromRight("").replace("\n", "");

        // Build the result string according to the requested format
        String report;
        if(toJson) {
            report = String.format("{\"Condition\": \"%s\", \"Humidity\": \"%s\", \"Wind\": \"%s\"}",
                    weather, humidity, wind);
        } else {
            report = String.format("""
                            Condition:  %s
                            Humidity:   %s
                            Wind:       %s
                            """,
                    weather, humidity, wind);
        }

        return new Right<>(report);
    }
}
