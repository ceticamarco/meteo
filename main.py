import os
from www import routes

def main():
    # Check whether environment variables are defined
    if not all(var in os.environ for var in ["METEO_LISTEN_ADDRESS", "METEO_LISTEN_PORT", "METEO_TOKEN", "METEO_CACHE_TTL"]):
        print("ERROR: 'METEO_LISTEN_ADDRESS', 'METEO_LISTEN_PORT', 'METEO_TOKEN' and 'METEO_CACHE_TTL' must be set.")
        return 1

    # Otherwise, retrieve variables
    listen_address = os.environ["METEO_LISTEN_ADDRESS"]
    listen_port = int(os.environ["METEO_LISTEN_PORT"])
    token = os.environ["METEO_TOKEN"]
    cache_ttl = int(os.environ["METEO_CACHE_TTL"])

    # Check whether API key is empty
    if not token:
        print("'METEO_TOKEN' cannot be empty")
        return 1

    # Start the webserver
    ws = routes.WebServer(listen_address, listen_port, token, cache_ttl)
    ws.launch_server()

if __name__ == "__main__":
    main()
