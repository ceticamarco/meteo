FROM python:latest
LABEL author="Marco Cetica"

# Specify working directory
WORKDIR /app

# Copy source files
COPY . .

# Install dependencies
RUN python -m pip install -r requirements.txt

# Launch the application
CMD ["python", "main.py"]
