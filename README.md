# Meteo 🌦 ![](https://github.com/ceticamarco/meteo/actions/workflows/meteo.yml/badge.svg)
**Meteo** is a HTTP weather forecast service that allows you to display weather conditions directly on your terminal or on the
`tmux` status bar. This service is written in Java using Spring Boot and relies on 
[OpenWeatherMap](https://openweathermap.org) to retrieve meteorological data.

## Usage
As an HTTP service, **Meteo** can be queried through any HTTP client, for example by using your browser or `cURL`. 
To retrieve the weather conditions of your city, send a `GET` request to the following endpoint specifying the city
in the URL. For example:

### Weather
```sh
$> curl 'http://127.0.0.1:9000/meteo/Rome'
☀️  +35°C
```

The service will yield an emoji(representing the current weather conditions) and the temperature formatted using
the metric system. To format the temperature using the imperial system, you can append the `i` parameter(_imperial_) to the URL.
That is:

```sh
$> curl 'http://127.0.0.1:9000/meteo/Rome?i'
☀️  +95°F
```

If your city consists of two or more words, you can format the URL as follows:
```sh
$> curl 'http://127.0.0.1:9000/meteo/Buenos+Aires'
```

### Humidity
**Meteo** can also be used to retrieve the percentage of humidity, the wind speed and the wind direction. Below
there is an example:

```sh
# Retrieve humidity of Amsterdam
$> curl 'http://127.0.0.1:9000/meteo/humidity/amsterdam'
75%
```

### Wind
```sh
# Retrieve wind speed of Macao
$> curl 'http://127.0.0.1:9000/meteo/wind/macao'
20.38kph SE ↖
```

To request the wind speed in MPH(Mile Per Hours) instead of KPM(Kilometers Per Hours), append
the `?i` parameter(_imperial_) to the URL. That is:

```sh
$> curl 'http://127.0.0.1:9000/meteo/wind/macao?i'
12.66mph SE ↖
```

### Report
You can also request all the previous data together by querying the following endpoint:

```sh
$> curl 'http://127.0.0.1:9000/meteo/report/St+Moritz'
Condition:  ☁️  +21°C
Humidity:   60%
Wind:       33.34kph SW ↗
```

To format the report using the imperial units of measurements, append the `?i` parameter to the URL. That is:

```sh
$> curl 'http://127.0.0.1:9000/meteo/report/St+Moritz?i'
Condition:  ☁️  +70°F
Humidity:   60%
Wind:       20.71mph SW ↗
```

You can also request the report in JSON by appending the `?j` parameter to the URL. That is:

```sh
$> curl 'http://127.0.0.1:9000/meteo/report/Montecarlo?j' # Metric version
{"Condition": "\u2600\ufe0f +35\u00b0C", "Humidity": "33%", "Wind": "19.3kph SW \u2197"}
$> curl 'http://127.0.0.1:9000/meteo/report/Montecarlo?j&i' # Imperial version
{"Condition": "\u2600\ufe0f +95\u00b0F", "Humidity": "33%", "Wind": "11.99mph SW \u2197"}
```

### Forecast
Finally, you can request the weather forecast of the next five days using the following interface:

```sh
$> curl '127.0.0.1:3000/meteo/forecast/paris' # Metric version 
Fri 04/10 -> ☀️  +13°C
Sat 05/10 -> ☁️  +16°C
Sun 06/10 -> 🌧  +14°C
Mon 07/10 -> 🌧  +17°C
Tue 08/10 -> 🌧  +18°C
curl '127.0.0.1:3000/meteo/forecast/paris?i' # Imperial + JSON version
{"forecast":[{"Fri 04/10":"☀️  +55°F"},{"Sat 05/10":"☁️  +61°F"},{"Sun 06/10":"🌧  +57°F"},{"Mon 07/10":"🌧  +62°F"},{"Tue 08/10":"🌧  +64°F"}]}%
```

## Cache
To minimize the amount of calls to the OpenWeatherMap servers, **Meteo** stores the weather data in a
built-in, in-memory cache data structure. Each time a client requests the weather of a given location, **Meteo**
tries to search it first on the cache; if it is found, the cached value is returned otherwise a new API call is
performed and the retrieved value is inserted in the cache before being returned to the client. The expiration time, expressed
in hours, is controlled by setting an environment variable(`METEO_CACHE_TTL`). After the cached value
is expired, **Meteo** must retrieve the weather data directly from OpenWeatherMap servers.

You can disable the cache by setting the `METEO_CACHE_TTL` variable to any non-positive value.

The cache system significantly improves **Meteo** performance by decreasing its latency, furthermore
it allows to reduce the amount of API calls per day(which is quite important if you are using the OpenWeatherMap free tier).

## Configuration
Before deploying the service, you must configure the following properties:

| Variable             | Meaning                                |
|----------------------|----------------------------------------|
| `SERVER_PORT`        | Listen port                            |
| `METEO_TOKEN`        | OpenWeatherMap API key                 |
| `METEO_CACHE_TTL`    | Cache time-to-live(expressed in hours) |

Each value must be set _before_ launching the application by exporting them as environment variable. If you plan to 
deploy the service using Docker, you can specify the previous variables by editing the `compose.yml`
file, otherwise you can set them by editing the `application.properties` file.

In order to use this service, you will also need an OpenWeatherMap API key. You can get one by following the
instructions [on their website](https://openweathermap.org/api).

## Deploy
The easiest way to deploy **Meteo** is by using Docker. In order to launch it, issue the following command:
```sh
$> docker compose up -d
```
This will build the container image and then launch it. By default the service will be available on `127.0.0.1:3000`
but you can easily change this property by modifying the associated environment variable(see section above).

## License
This software is released under the GPLv3 license. You can find a copy of the license with this repository 
or by visiting the [following page](https://choosealicense.com/licenses/gpl-3.0/).

