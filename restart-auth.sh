#!/bin/bash
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORT=8084

# Gradle 8.12 cannot run on newer JDKs (e.g. "Unsupported class file major version 69").
# Use the bundled JDK 21 if present so the bootRun below works regardless of the system default JDK.
BUNDLED_JDK="$ROOT_DIR/jdk-21.0.2+13/Contents/Home"
if [ -d "$BUNDLED_JDK" ]; then
    export JAVA_HOME="$BUNDLED_JDK"
fi

echo "Restarting Auth Service..."
PID=$(lsof -t -i:$PORT)
if [ -n "$PID" ]; then
    echo "Stopping existing process (PID $PID) on port $PORT..."
    kill -9 $PID
    echo "Process stopped."
fi

echo "Starting Auth Service..."
nohup "$ROOT_DIR/gradlew" -p "$ROOT_DIR" :auth-service:bootRun > "$ROOT_DIR/auth.log" 2>&1 &

echo "Waiting for Auth Service to be ready on port $PORT..."
while ! nc -z localhost $PORT; do sleep 1; done
echo "Auth Service restarted successfully!"
