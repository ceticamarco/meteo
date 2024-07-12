from flask import Flask
from flask import request
from gevent.pywsgi import WSGIServer
from typing import Tuple, Dict
from enum import Enum
from cache import database
import requests

class Units(Enum):
    METRIC = 0,
    IMPERIAL = 1

# Define global objects
app = Flask(__name__)
cache = database.Database(ttl=1)

# OpenWeatherMap API key
api_key = None

# Custom error data type
Error = str

def __get_city_coordinates(city: str) -> Tuple[float, float] | Error:
    ''' Given the city name, return a tuple containing 
        its latitude and its longitude  '''
    url = f"https://api.openweathermap.org/geo/1.0/direct?q={city}&limit=5&appid={api_key}"
    res = requests.get(url).json()

    # Check whether city has been found
    if not res: return "Cannot find this city\n"

    return ( res[0]["lat"], res[0]["lon"] )

def __get_emoji_from_condition(condition: str) -> str:
    ''' Given a weather condition return an emoji '''
    conditions_map = {
        "Thunderstorm": "⛈️",
        "Drizzle": "🌦",
        "Rain": "🌧",
        "Snow": "☃️",
        "Mist": "💭",
        "Smoke": "💭",
        "Haze": "💭",
        "Dust": "💭",
        "Fog": "💭",
        "Sand": "💭",
        "Ash": "💭",
        "Squall": "💭",
        "Tornado": "🌪",
        "Clear": "☀️",
        "Clouds": "☁️"
    }

    return conditions_map[condition]

def __get_city_weather(coordinates: Tuple[float, float]) -> Dict[str, str | Tuple[str, str]]:
    ''' Given the coordinates of a city, returns its weather '''
    url = f"https://api.openweathermap.org/data/2.5/weather?units=metric"\
          f"&lat={coordinates[0]}&lon={coordinates[1]}&appid={api_key}"
    res = requests.get(url).json()

    # Extract the weather condition and the temperature
    condition = res["weather"][0]["main"]
    temperature_celsius = round(res["main"]["temp"])

    # Compute the temperature in Fahrenheit
    temperature_fahrenheit = round((temperature_celsius * 1.8) + 32)

    # Map the weather condition to an emoji
    condition_emoji = __get_emoji_from_condition(condition)
    
    return { "emoji": condition_emoji, "temperature": (temperature_celsius, temperature_fahrenheit) }


@app.route("/api/<city>", methods=["GET"])
def meteo_route(city):
    # Retrieve unit of measurement. By default it will be set to "metric"
    if request.args.get("f") is not None:
        units = Units.IMPERIAL
    else:
        units = Units.METRIC

    # Check if city already exists on the cache
    cached_value = cache.get_key(city)
    if cached_value is not None:
        weather = cached_value
    else:
        # Retrieve coordinates from city name
        coordinates = __get_city_coordinates(city)

        # Check if city exists
        if isinstance(coordinates, Error):
            return coordinates, 400

        # Retrieve city weather
        weather = __get_city_weather(coordinates)
        # Save weather into the cache
        cache.add_key(city, weather)

    # Extract weather condition icon
    emoji = str(weather["emoji"])

    # Extract the appropriate temperature based on the unit of measurement
    temperature = weather["temperature"][0] if units == Units.METRIC else weather["temperature"][1]

    # Add '+' sign to temperature if it is positive
    temperature = f"{'+' if int(temperature) > 0 else ''}{temperature}"

    # Build result string
    result = f"Weather conditions: {emoji}({temperature}"\
             f"°{'C' if units == units.METRIC else 'F'})\n"

    # Return the result
    return result, 200

def launch_server(host: str, port: int, token: str) -> None:
    # set API key
    global api_key
    api_key = token

    http_server = WSGIServer((host, port), app, log=None)
    try:
        print(f"Listening on http://{host}:{port}")
        http_server.serve_forever()
    except KeyboardInterrupt:
        http_server.stop()

