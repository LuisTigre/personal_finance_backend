#!/bin/bash

echo "🚀 Starting Personal Finance App..."
echo ""
echo "This will start:"
echo "  - PostgreSQL database"
echo "  - KeyCloak authentication server"
echo "  - Spring Boot backend"
echo "  - Angular frontend"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker first."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Error: docker-compose is not installed."
    exit 1
fi

echo "📦 Starting services with Docker Compose..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to start..."
sleep 5

echo ""
echo "✅ Services are starting up!"
echo ""
echo "Access the application:"
echo "  Frontend:        http://localhost:4200"
echo "  Backend API:     http://localhost:8081"
echo "  KeyCloak Admin:  http://localhost:8080"
echo "  PostgreSQL:      localhost:5432"
echo ""
echo "Default credentials:"
echo "  KeyCloak Admin:  admin / admin"
echo "  Test User:       testuser / test123"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f"
echo ""
echo "To stop services:"
echo "  docker-compose down"
echo ""
