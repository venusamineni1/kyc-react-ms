#!/bin/bash
echo "Restarting Auth Service..."
PORT=8084
PID=$(lsof -t -i:$PORT)
if [ ! -z "$PID" ]; then
    echo "Stopping existing process (PID $PID) on port $PORT..."
    kill -9 $PID
    echo "Process stopped."
fi

echo "Starting Auth Service..."
cd auth-service
./mvnw spring-boot:run > ../auth-service.log 2>&1 &
echo "Waiting for Auth Service to be ready on port $PORT..."
while ! nc -z localhost $PORT; do   
  sleep 1
done
echo "Auth Service restarted successfully!"
cd ..
