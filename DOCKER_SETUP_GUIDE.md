# Smart Parking System - Docker Setup Guide

## 🐳 Prerequisites

1. **Docker Desktop** (for Windows/Mac) or **Docker Engine** (for Linux)
2. **Docker Compose** (included with Docker Desktop)
3. **WSL2** (for Windows users)
4. **Git** (for cloning the repository)

## 🚀 Quick Start

### 1. Clone and Setup
```bash
git clone <repository-url>
cd SmartParkingSystem
```

### 2. Configure Environment
```bash
# Copy environment template
cp .env.example .env

# Edit .env with your configuration
nano .env
```

### 3. Build and Run
```bash
# Build and start all services
docker compose up --build -d

# View logs
docker compose logs -f

# Stop services
docker compose down
```

### 4. Verify Deployment
```bash
# Check health status
curl http://localhost:8081/actuator/health

# Check parking slots API
curl http://localhost:8081/api/parking-slots

# Access application
# Main Page: http://localhost:8081
# Exit Management: http://localhost:8081/exit.html
```

## 📋 Environment Variables

### Required Variables
- `DB_PASSWORD`: PostgreSQL database password
- `UPI_MERCHANT_ID`: UPI merchant ID for payments

### Optional Variables
- `DB_NAME`: Database name (default: smart_parking_db)
- `DB_USERNAME`: Database username (default: postgres)
- `SERVER_PORT`: Application port (default: 8081)
- `ACTUATOR_PORT`: Actuator port (default: 8082)

## 🔧 Docker Services

### parking-db (PostgreSQL)
- **Image**: postgres:16-alpine
- **Port**: 5432 (host) → 5432 (container)
- **Volume**: postgres_data (persistent storage)
- **Health Check**: pg_isready

### parking-backend (Spring Boot)
- **Build**: Multi-stage Dockerfile
- **Port**: 8081 (host) → 8081 (container)
- **Port**: 8082 (host) → 8082 (container) - Actuator
- **Volume**: ./logs:/app/logs
- **Health Check**: curl http://localhost:8081/actuator/health

## 🗂️ File Structure

```
SmartParkingSystem/
├── Dockerfile                 # Multi-stage build configuration
├── docker-compose.yml          # Service orchestration
├── .dockerignore             # Build context exclusions
├── .env                      # Environment variables (create from .env.example)
├── .env.example              # Environment template
├── logs/                    # Application logs (mounted volume)
└── src/                     # Source code
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Check what's using the port
netstat -tulpn | grep :8081

# Kill the process or change port in .env
SERVER_PORT=8082
```

#### 2. Database Connection Issues
```bash
# Check database logs
docker compose logs parking-db

# Restart database
docker compose restart parking-db

# Reset database volume (WARNING: Deletes all data)
docker compose down -v
docker compose up -d
```

#### 3. Build Failures
```bash
# Clean build
docker compose down
docker system prune -f
docker compose up --build --force-recreate
```

#### 4. Permission Issues (Linux)
```bash
# Fix log directory permissions
sudo chown -R $USER:$USER logs/
chmod -R 755 logs/
```

### Health Checks

#### Check Container Status
```bash
docker compose ps
```

#### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f parking-backend
docker compose logs -f parking-db
```

#### Container Shell Access
```bash
# Backend container
docker compose exec parking-backend bash

# Database container
docker compose exec parking-db psql -U postgres -d smart_parking_db
```

## 📊 Monitoring

### Application Health
- **Health Endpoint**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8082/actuator/metrics
- **Info**: http://localhost:8082/actuator/info

### Database Health
```bash
# Check database connection
docker compose exec parking-db pg_isready -U postgres

# Connect to database
docker compose exec parking-db psql -U postgres -d smart_parking_db
```

## 🔄 Development Workflow

### 1. Development Mode
```bash
# Run with hot reload (if enabled)
docker compose up --build

# View logs in real-time
docker compose logs -f parking-backend
```

### 2. Production Mode
```bash
# Run in detached mode
docker compose up --build -d

# Check status
docker compose ps
```

### 3. Updates
```bash
# Pull latest changes
git pull

# Rebuild and restart
docker compose down
docker compose up --build -d
```

## 🌐 Network Configuration

### Docker Network
- **Name**: smart-parking-network
- **Subnet**: 172.20.0.0/16
- **Driver**: bridge

### Service Communication
- Backend connects to database via: `parking-db:5432`
- Host machine accesses services via: `localhost:8081`, `localhost:8082`

## 📦 Volumes

### Persistent Data
- **postgres_data**: Database data (named volume)
- **./logs**: Application logs (bind mount)

### Backup Database
```bash
# Create backup
docker compose exec parking-db pg_dump -U postgres smart_parking_db > backup.sql

# Restore backup
docker compose exec -T parking-db psql -U postgres smart_parking_db < backup.sql
```

## 🔒 Security Considerations

### Production Deployment
1. Change default passwords in `.env`
2. Use HTTPS (configure SSL in production)
3. Restrict CORS origins
4. Enable firewall rules
5. Regular security updates

### Environment Variables
- Never commit `.env` to version control
- Use strong passwords
- Rotate secrets regularly
- Use secrets management in production

## 📈 Performance Tuning

### JVM Options
```bash
# Default JVM settings
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+UseStringDeduplication -Xms512m -Xmx1024m"
```

### Database Settings
```bash
# Connection pool settings (in application-prod.properties)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
```

## 🆘 Support

If you encounter issues:

1. Check the logs: `docker compose logs -f`
2. Verify environment variables in `.env`
3. Check Docker and Docker Compose versions
4. Ensure sufficient disk space and memory
5. Review this troubleshooting guide

For additional support, create an issue in the repository with:
- Error messages
- Docker and system versions
- Steps to reproduce
- Logs output
