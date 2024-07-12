import os
from www import routes

def main():
    # Check whether environment variables are defined
    if not all(var in os.environ for var in ["METEO_LISTEN_ADDRESS", "METEO_LISTEN_PORT", "METEO_TOKEN"]):
        print("ERROR: 'METEO_LISTEN_ADDRESS', 'METEO_LISTEN_PORT' and 'METEO_TOKEN' must be set.")
        return 1

    # Otherwise, retrieve variables
    listen_address = os.environ["METEO_LISTEN_ADDRESS"]
    listen_port = int(os.environ["METEO_LISTEN_PORT"])
    token = os.environ["METEO_TOKEN"]

    # Start the webserver
    routes.launch_server(listen_address, listen_port, token)


if __name__ == "__main__":
    main()
