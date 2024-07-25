from flask import Flask
from flask import request
from gevent.pywsgi import WSGIServer
from typing import Tuple, Dict, List, cast
from enum import Enum
from cache import database
from datetime import datetime, time
import json
import requests

class Units(Enum):
    METRIC = 0,
    IMPERIAL = 1

# Custom error data type
Error = str

class WebServer:
    def __init__(self, host: str, port: int, token: str, cache_ttl: int) -> None:
        self.host = host
        self.port = port
        self.token = token
        self.cache_ttl = cache_ttl
        # Initialize Flask app
        self.app = Flask(__name__)
        # Initialize cache
        self.cache = database.Database(ttl=self.cache_ttl)
        # Register API route
        self.app.add_url_rule("/meteo/<city>", view_func=self.weather_route)
        self.app.add_url_rule("/meteo/humidity/<city>", view_func=self.humidity_route)
        self.app.add_url_rule("/meteo/wind/<city>", view_func=self.wind_route)
        self.app.add_url_rule("/meteo/report/<city>", view_func=self.report_route)

    def __get_city_coordinates(self, city: str) -> Tuple[float, float] | Error:
        ''' Given the city name, return a tuple containing 
            its latitude and its longitude  '''
        url = f"https://api.openweathermap.org/geo/1.0/direct?q={city}&limit=5&appid={self.token}"
        res = requests.get(url).json()

        # Check whether city has been found
        if not res: return "Cannot find this city\n"

        return ( res[0]["lat"], res[0]["lon"] )

    def __get_emoji_from_condition(self, condition: str) -> str:
        ''' Given a weather condition return an emoji '''
        conditions_map = {
            "Thunderstorm": "⛈️",
            "Drizzle": "🌦 ",
            "Rain": "🌧 ",
            "Snow": "☃️",
            "Mist": "💭",
            "Smoke": "💭",
            "Haze": "💭",
            "Dust": "💭",
            "Fog": "💭",
            "Sand": "💭",
            "Ash": "💭",
            "Squall": "💭",
            "Tornado": "🌪 ",
            "Clear": "☀️",
            "Clouds": "☁️"
        }

        # If condition is "clear" and it's nighttime, return 🌙 instead of ☀️
        now = datetime.now().time()
        if condition == "Clear" and (now >= time(22,00) or now <= time(7,00)):
            return "🌙"

        return conditions_map[condition]

    def __get_city_weather(self, coordinates: Tuple[float, float]) -> Dict[str, str | Tuple[str, str]]:
        ''' Given the coordinates of a city, returns its weather '''
        url = f"https://api.openweathermap.org/data/2.5/weather?units=metric"\
              f"&lat={coordinates[0]}&lon={coordinates[1]}&appid={self.token}"
        res = requests.get(url).json()

        # Extract the weather condition and the temperature
        condition = res["weather"][0]["main"]
        temperature_celsius = round(res["main"]["temp"])

        # Compute the temperature in Fahrenheit
        temperature_fahrenheit = round((temperature_celsius * 1.8) + 32)

        # Map the weather condition to an emoji
        condition_emoji = self.__get_emoji_from_condition(condition)
        
        return { "emoji": condition_emoji, "temperature": (temperature_celsius, temperature_fahrenheit) }

    def __get_city_humidity(self, coordinates: Tuple[float, float]) -> Dict[str, str]:
        ''' Given the coordinates of a city, returns its humidity level(in percentage) '''
        url = f"https://api.openweathermap.org/data/2.5/weather?units=metric"\
              f"&lat={coordinates[0]}&lon={coordinates[1]}&appid={self.token}"
        res = requests.get(url).json()

        # Extract the humidity level
        humidity = res["main"]["humidity"]

        return { "value": humidity }

    def __get_city_wind(self, coordinates: Tuple[float, float]) -> Dict[str, str]:
        ''' Given the coordinates of a city, returns its wind speed and its direction '''
        url = f"https://api.openweathermap.org/data/2.5/weather?units=metric"\
              f"&lat={coordinates[0]}&lon={coordinates[1]}&appid={self.token}"
        res = requests.get(url).json()

        # Extract wind fields
        wind_speed = res["wind"]["speed"]
        wind_deg = int(res["wind"]["deg"])

        # Map wind degree to cardinal direction
        # Each cardinal direction represent a segment of 22.5 degrees
        cardinal_directions: List[Tuple[str, str]] = [
            ("N", "↑"),   # 0/360 DEG
            ("NNE", "⇗"), # 22.5 DEG
            ("NE",  "↗"), # 45 DEG
            ("ENE", "⇖"), # 67.5 DEG
            ("E",   "→"), # 90 DEG
            ("ESE", "⇘"), # 112.5 DEG
            ("SE",  "↘"), # 135 DEG
            ("SSE", "⇙"), # 157.5 DEG
            ("S",   "↓"), # 180 DEG
            ("SSW", "⇘"), # 202.5 DEG
            ("SW",  "↙"), # 225 DEG
            ("WSW", "⇖"), # 247.5 DEG
            ("W",   "←"), # 270 DEG
            ("WNW", "⇗"), # 292.5 DEG
            ("NW",  "↖"), # 315 DEG
            ("NNW", "⇖"), # 337.5 DEG
        ]
        
        # We compute "idx ≡ round(wind_deg / 22.5) (mod 16)" 
        # to ensure that values above 360 degrees or below 0 degrees
        # "stay" bounded to the map
        idx = round(wind_deg / 22.5) % 16

        # Get wind direction and direction icon
        wind_direction = cardinal_directions[idx][0]
        wind_icon = cardinal_directions[idx][1]


        return { "speed": wind_speed, "direction": wind_direction, "icon": wind_icon }


    def humidity_route(self, city):
        # Check if city already exists on the cache
        cached_value = self.cache.get_key(f"{city}_humidity")
        if cached_value is not None:
            humidity = cached_value
        else:
            # Retrieve coordinates from city name
            coordinates = self.__get_city_coordinates(city)

            # Check if city exists
            if isinstance(coordinates, Error):
                return coordinates, 400

            # Retrieve city humidity
            humidity = self.__get_city_humidity(coordinates)
            # Save humidity into the cache
            self.cache.add_key(f"{city}_humidity", cast(Dict[str, str | Tuple[str, str]], humidity))

        # Build result string
        result = f"{humidity['value']}%\n"

        # Return the result
        return result, 200

    def wind_route(self, city):
        # Retrieve unit of measurement. By default, it will be set to "metric"
        if request.args.get("m") is not None:
            units = Units.IMPERIAL
        else:
            units = Units.METRIC

        # Check if city already exists on the cache
        cached_value = self.cache.get_key(f"{city}_wind")
        if cached_value is not None:
            wind = cached_value
        else:
            # Retrieve coordinates from city name
            coordinates = self.__get_city_coordinates(city)

            # Check if city exists
            if isinstance(coordinates, Error):
                return coordinates, 400

            # Retrieve city wind
            wind = self.__get_city_wind(coordinates)
            # Save wind into the cache
            self.cache.add_key(f"{city}_wind", cast(Dict[str, str | Tuple[str, str]], wind))

        # Convert wind speed(by default represented in m/s) according to the unit of measurement 
        # 1 m/s = 2.23694 mph
        # 1 m/s = 3.6 kph
        wind_speed = str(wind["speed"])
        wind_speed = float(wind_speed) * 2.23694 if units == Units.IMPERIAL else float(wind_speed) * 3.6

        # Extract wind direction
        wind_direction = str(wind["direction"])
        wind_icon = str(wind["icon"])

        # Build result string
        result = f"{round(wind_speed, 2)}{'mph' if units == Units.IMPERIAL else 'kph'} {wind_direction} {wind_icon}\n"

        # Return the result
        return result, 200

    def report_route(self, city):
        # Retrieve unit of measurement. By default it will be set to "metric"
        if request.args.get("i") is not None:
            units = Units.IMPERIAL
        else:
            units = Units.METRIC

        # Check if weather already exists on the cache
        cached_weather = self.cache.get_key(f"{city}_weather")
        if cached_weather is not None:
            weather = cached_weather
        else:
            coordinates = self.__get_city_coordinates(city)

            # Check if city exists
            if isinstance(coordinates, Error):
                return coordinates, 400

            # Retrieve city weather
            weather = self.__get_city_weather(coordinates)
            # Save wwather into the cache
            self.cache.add_key(f"{city}_weather", weather)

        # Check if humidity already exists on the cache
        cached_humidity = self.cache.get_key(f"{city}_humidity")
        if cached_humidity is not None:
            humidity = cached_humidity
        else:
            coordinates = self.__get_city_coordinates(city)
            # Retrieve city humidity
            humidity = self.__get_city_humidity(cast(Tuple[float, float], coordinates))
            # Save humidity into the cache
            self.cache.add_key(f"{city}_humidity", cast(Dict[str, str | Tuple[str, str]], humidity))

        # Check if wind already exists on the cache
        cached_wind = self.cache.get_key(f"{city}_wind")
        if cached_wind is not None:
            wind = cached_wind
        else:
            # Retrieve coordinates from city name
            coordinates = self.__get_city_coordinates(city)

            # Retrieve city wind
            wind = self.__get_city_wind(cast(Tuple[float, float], coordinates))
            # Save wind into the cache
            self.cache.add_key(f"{city}_wind", cast(Dict[str, str | Tuple[str, str]], wind))


        # Extract weather condition icon
        emoji = str(weather["emoji"])

        # Extract the appropriate temperature based on the unit of measurement
        temperature = weather["temperature"][0] if units == Units.METRIC else weather["temperature"][1]

        # Add '+' sign to temperature if it is positive
        temperature = f"{'+' if int(temperature) > 0 else ''}{temperature}"

        # Convert wind speed(by default represented in m/s) according to the unit of measurement 
        wind_speed = str(wind["speed"])
        wind_speed = float(wind_speed) * 2.23694 if units == Units.IMPERIAL else float(wind_speed) * 3.6

        # Extract wind direction
        wind_direction = str(wind["direction"])
        wind_icon = str(wind["icon"])

        # Build weather condition string
        weather_condition = f"{emoji} {temperature}°{'C' if units == units.METRIC else 'F'}"

        # Build humidity string
        humidity_condition = f"{humidity['value']}%"

        # Build wind string
        wind_condition = f"{round(wind_speed, 2)}{'mph' if units == Units.IMPERIAL else 'kph'} {wind_direction} {wind_icon}"

        # Build reseult string according to the requested format
        if request.args.get("j") is not None:
            # Format report as JSON data
            report = {
                "Condition": weather_condition,
                "Humidity": humidity_condition,
                "Wind": wind_condition
            }
            report = json.dumps(report)
        else:
            # Format report as raw data
            report = f"Condition:  {weather_condition}\n"\
                     f"Humidity:   {humidity_condition}\n"\
                     f"Wind:       {wind_condition}\n"


        # Return the result
        return report, 200


    def weather_route(self, city):
        # Retrieve unit of measurement. By default it will be set to "celsius"
        if request.args.get("f") is not None:
            units = Units.IMPERIAL
        else:
            units = Units.METRIC

        # Check if city already exists on the cache
        cached_value = self.cache.get_key(f"{city}_weather")
        if cached_value is not None:
            weather = cached_value
        else:
            # Retrieve coordinates from city name
            coordinates = self.__get_city_coordinates(city)

            # Check if city exists
            if isinstance(coordinates, Error):
                return coordinates, 400

            # Retrieve city weather
            weather = self.__get_city_weather(coordinates)
            # Save weather into the cache
            self.cache.add_key(f"{city}_weather", weather)

        # Extract weather condition icon
        emoji = str(weather["emoji"])

        # Extract the appropriate temperature based on the unit of measurement
        temperature = weather["temperature"][0] if units == Units.METRIC else weather["temperature"][1]

        # Add '+' sign to temperature if it is positive
        temperature = f"{'+' if int(temperature) > 0 else ''}{temperature}"

        # Build result string
        result = f"{emoji} {temperature}"\
                 f"°{'C' if units == units.METRIC else 'F'}\n"

        # Return the result
        return result, 200


    def launch_server(self) -> None:
        http_server = WSGIServer((self.host, self.port), self.app, log=None)
        try:
            print(f"Listening on http://{self.host}:{self.port}")
            http_server.serve_forever()
        except KeyboardInterrupt:
            http_server.stop()

