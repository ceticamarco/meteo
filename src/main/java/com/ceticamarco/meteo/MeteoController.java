package com.ceticamarco.meteo;

import com.ceticamarco.lambdatonic.Left;
import com.ceticamarco.lambdatonic.Right;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class MeteoController {
    private final MeteoService meteoService;

    @Autowired
    public MeteoController(MeteoService meteoService) {
        this.meteoService = meteoService;
    }

    @GetMapping("/meteo/{city}")
    public ResponseEntity<String> getWeatherByCity(@PathVariable("city") String city,
                                                   @RequestParam(required = false) String f) throws IOException, InterruptedException {
        Units units = f != null ? Units.IMPERIAL : Units.METRIC;

        var result = meteoService.getWeather(city, units);

        switch (result) {
            case Left<Error, String> err -> { return new ResponseEntity<>(err.value().getMessage(), HttpStatus.BAD_REQUEST); }
            case Right<Error, String> content -> { return new ResponseEntity<>(content.value(), HttpStatus.OK); }
        }
    }

    @GetMapping("/meteo/humidity/{city}")
    public ResponseEntity<String> getHumidityByCity(@PathVariable("city") String city) {
        return new ResponseEntity<>("City humidity", HttpStatus.OK);
    }

    @GetMapping("/meteo/wind/{city}")
    public ResponseEntity<String> getWindByCity(@PathVariable("city") String city) {
        return new ResponseEntity<>("City wind", HttpStatus.OK);
    }

    @GetMapping("/meteo/report/{city}")
    public ResponseEntity<String> getWeatherReportByCity(@PathVariable("city") String city) {
        return new ResponseEntity<>("Weather report", HttpStatus.OK);
    }
}
