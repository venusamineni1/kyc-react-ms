# Venus KYC Viewer

## Overview
This is a KYC (Know Your Customer) Viewer application consisting of a **Spring Boot** backend and a **React** frontend.

## Prerequisites
Ensure your machine has the following installed:
- **Java Development Kit (JDK) 17** or higher.
- **Node.js** (LTS version recommended, e.g., v18+).
- **Maven** (optional, as `mvnw` wrapper is included).

## Setup & Running

### 1. Backend (Spring Boot)
The backend handles API requests and data persistence (H2 Database).

**Steps:**
1. Open a terminal in the root directory (`kyctest/viewer`).
2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   *Note: On Windows, use `mvnw.cmd spring-boot:run`.*

The backend will start on **[http://localhost:8080](http://localhost:8080)**.

### 2. Frontend (React + Vite)
The frontend provides the user interface.

**Steps:**
1. Open a **new** terminal window.
2. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
3. Install dependencies (only required for the first time):
   ```bash
   npm install
   ```
4. Start the development server:
   ```bash
   npm run dev
   ```

The frontend will start on **[http://localhost:5173](http://localhost:5173)**.

## Default Credentials
Use the following credentials to log in:
- **Admin**: `admin` / `admin`
- **Analyst**: `analyst` / `password`
- **Reviewer**: `reviewer` / `password`

## Configuration
- **Database**: Uses an in-memory H2 database by default. Data is reset on restart.
- **Ports**:
    - Backend: `8080` (Change in `src/main/resources/application.properties`)
    - Frontend: `5173` (Change in `frontend/vite.config.js`)

## Troubleshooting
- **Port already in use**: Ensure no other services are running on ports 8080 or 5173.
- **Connection Refused**: Ensure the backend is fully started before trying to log in on the frontend.
