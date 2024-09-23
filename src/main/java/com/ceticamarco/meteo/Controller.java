package com.ceticamarco.meteo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    @GetMapping("/meteo/{city}")
    public ResponseEntity<String> getWeatherByCity(@PathVariable("city") String city) {
        return new ResponseEntity<>("City weather", HttpStatus.OK);
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
