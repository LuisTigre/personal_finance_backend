@echo off
echo Starting Personal Finance App...
echo.
echo This will start:
echo   - PostgreSQL database
echo   - KeyCloak authentication server
echo   - Spring Boot backend
echo   - Angular frontend
echo.

docker info >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not running. Please start Docker first.
    exit /b 1
)

echo Starting services with Docker Compose...
docker-compose up -d

echo.
echo Waiting for services to start...
timeout /t 5 /nobreak >nul

echo.
echo Services are starting up!
echo.
echo Access the application:
echo   Frontend:        http://localhost:4200
echo   Backend API:     http://localhost:8081
echo   KeyCloak Admin:  http://localhost:8080
echo   PostgreSQL:      localhost:5432
echo.
echo Default credentials:
echo   KeyCloak Admin:  admin / admin
echo   Test User:       testuser / test123
echo.
echo To view logs:
echo   docker-compose logs -f
echo.
echo To stop services:
echo   docker-compose down
echo.
pause
