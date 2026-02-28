#!/bin/bash

# Quick Redis Starter Script for KPI Tracking Tool

echo "🔍 Checking Redis status..."

# Check if Redis is installed
if command -v redis-server &> /dev/null; then
    echo "✅ Redis is installed"
    
    # Check if Redis is already running
    if redis-cli ping &> /dev/null; then
        echo "✅ Redis is already running!"
        echo ""
        echo "Redis Information:"
        redis-cli INFO server | grep "redis_version"
        redis-cli INFO server | grep "tcp_port"
        echo ""
        echo "You can now start your Spring Boot application."
    else
        echo "⚠️  Redis is not running. Starting Redis..."
        
        # Try to start Redis as a background service
        if command -v brew &> /dev/null; then
            # macOS with Homebrew
            echo "Starting Redis with brew services..."
            brew services start redis
            sleep 2
            
            if redis-cli ping &> /dev/null; then
                echo "✅ Redis started successfully!"
            else
                echo "❌ Failed to start Redis with brew services"
                echo "Try manually: redis-server"
            fi
        else
            # Linux or manual start
            echo "Starting Redis manually..."
            redis-server --daemonize yes
            sleep 2
            
            if redis-cli ping &> /dev/null; then
                echo "✅ Redis started successfully!"
            else
                echo "❌ Failed to start Redis"
                echo "Try manually: redis-server"
            fi
        fi
    fi
else
    echo "❌ Redis is not installed!"
    echo ""
    echo "Installation instructions:"
    echo ""
    echo "macOS:"
    echo "  brew install redis"
    echo "  brew services start redis"
    echo ""
    echo "Linux (Ubuntu/Debian):"
    echo "  sudo apt-get update"
    echo "  sudo apt-get install redis-server"
    echo "  sudo systemctl start redis"
    echo ""
    echo "Docker:"
    echo "  docker run -d -p 6379:6379 --name kpi-redis redis:latest"
    echo ""
    echo "Or disable Redis in your application:"
    echo "  Add to .env file: REDIS_ENABLED=false"
fi

echo ""
echo "📚 For more details, see: REDIS_CONNECTION_FIX.md"
