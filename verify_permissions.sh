#!/bin/bash

# 1. Login
echo "Logging in as Analyst..."
LOGIN_RESP=$(curl -s -X POST http://localhost:8083/api/auth/login -H "Content-Type: application/json" -d '{"username": "analyst", "password": "password"}')
ANALYST_TOKEN=$(echo $LOGIN_RESP | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "Logging in as Reviewer..."
LOGIN_RESP_REV=$(curl -s -X POST http://localhost:8083/api/auth/login -H "Content-Type: application/json" -d '{"username": "reviewer", "password": "password"}')
REVIEWER_TOKEN=$(echo $LOGIN_RESP_REV | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "Logging in as AFC User..."
LOGIN_RESP_AFC=$(curl -s -X POST http://localhost:8083/api/auth/login -H "Content-Type: application/json" -d '{"username": "afc_user", "password": "password"}')
AFC_TOKEN=$(echo $LOGIN_RESP_AFC | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

# 2. Advance Case 1 (if needed) to ensure it reaches AFC stage
# Analyst complete
echo "Checking Analyst tasks..."
TASKS=$(curl -s -H "Authorization: Bearer $ANALYST_TOKEN" http://localhost:8083/api/cases/tasks)
TID=$(echo $TASKS | grep -o '"taskId":"[^"]*' | head -1 | cut -d'"' -f4)
if [ ! -z "$TID" ]; then 
  echo "Completing Analyst Task $TID..."
  curl -s -X POST "http://localhost:8083/api/cases/tasks/$TID/complete" -H "Authorization: Bearer $ANALYST_TOKEN"
fi

# Reviewer complete
echo "Checking Reviewer tasks (to advance)..."
TASKS_REV=$(curl -s -H "Authorization: Bearer $REVIEWER_TOKEN" http://localhost:8083/api/cases/tasks)
TID_REV=$(echo $TASKS_REV | grep -o '"taskId":"[^"]*' | head -1 | cut -d'"' -f4)
if [ ! -z "$TID_REV" ]; then 
  echo "Completing Reviewer Task $TID_REV..."
  curl -s -X POST "http://localhost:8083/api/cases/tasks/$TID_REV/complete" -H "Authorization: Bearer $REVIEWER_TOKEN"
fi

# 3. Check Visibility
echo "--- VERIFICATION ---"
echo "Reviewer Tasks (Should be EMPTY):"
curl -s -H "Authorization: Bearer $REVIEWER_TOKEN" http://localhost:8083/api/cases/tasks
echo ""
echo "AFC Tasks (Should contain AFC Review):"
curl -s -H "Authorization: Bearer $AFC_TOKEN" http://localhost:8083/api/cases/tasks
echo ""
