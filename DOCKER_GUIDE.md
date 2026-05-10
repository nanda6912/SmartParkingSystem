# Docker Guide - Smart Parking System

## 🐳 Quick Start (WSL2)

### Prerequisites
- Docker Desktop with WSL2 integration enabled
- Git
- Make sure Docker Desktop is running

### 1. Clone and Setup
```bash
git clone <repository-url>
cd SmartParkingSystem
git checkout docker-setup
```

### 2. Environment Configuration
```bash
# Copy environment template
cp .env.example .env

# Edit environment variables (required)
nano .env
```

**Important: Update these values in `.env`:**
- `POSTGRES_PASSWORD=your_secure_password_here`
- `SPRING_DATASOURCE_PASSWORD=your_secure_password_here`
- `JWT_SECRET=your-jwt-secret-key-change-this`
- `PARKING_UPI_MERCHANT_ID=your-actual-upi-id`
- Other values as needed

### 3. Start Application
```bash
# Build and start all services
docker-compose up --build -d

# View logs
docker-compose logs -f

# Check container status
docker ps
```

### 4. Verify Application
```bash
# Health check
curl http://localhost:8081/actuator/health

# Test API endpoints
curl http://localhost:8081/api/parking-slots
curl http://localhost:8081/api/exit/active-bookings
```

### 5. Access the Application
- **Main Application**: http://localhost:8081
- **Ground Floor**: http://localhost:8081/ground-floor.html
- **First Floor**: http://localhost:8081/floor1.html
- **Exit Management**: http://localhost:8081/exit.html
- **Health Check**: http://localhost:8081/actuator/health

## 🛠️ Docker Commands Reference

### Start Services
```bash
# Build and start
docker-compose up --build -d

# Start without rebuilding
docker-compose up -d

# Start with logs
docker-compose up --build
```

### Monitor Services
```bash
# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f parking-backend
docker-compose logs -f parking-db

# Check container status
docker ps
docker-compose ps
```

### Stop Services
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This deletes database data)
docker-compose down -v

# Stop and remove images
docker-compose down --rmi all
```

### Maintenance
```bash
# Rebuild specific service
docker-compose up --build parking-backend

# View resource usage
docker stats

# Access container shell
docker exec -it smart-parking-backend sh
docker exec -it smart-parking-db psql -U postgres -d smart_parking_db
```

## 🔧 Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Check what's using port 8081
netstat -tulpn | grep :8081

# Kill process
sudo kill -9 <PID>
```

#### 2. Database Connection Issues
```bash
# Check database container
docker-compose logs parking-db

# Test database connection
docker exec -it smart-parking-db psql -U postgres -d smart_parking_db -c "SELECT 1;"
```

#### 3. Backend Startup Issues
```bash
# View backend logs
docker-compose logs parking-backend

# Check backend health
curl http://localhost:8081/actuator/health
```

#### 4. Permission Issues (WSL2)
```bash
# Fix permissions for logs directory
sudo chown -R $USER:$USER logs/
sudo chmod -R 755 logs/
```

### Health Checks

#### Backend Health
```bash
# Detailed health check
curl http://localhost:8081/actuator/health

# Application info
curl http://localhost:8081/actuator/info

# Metrics
curl http://localhost:8081/actuator/metrics
```

#### Database Health
```bash
# Check database container health
docker inspect smart-parking-db | grep Health -A 10

# Connect to database
docker exec -it smart-parking-db psql -U postgres -d smart_parking_db
```

## 🔄 Development Workflow

### Making Changes
```bash
# 1. Make code changes
# 2. Rebuild and restart
docker-compose up --build -d parking-backend

# 3. View logs to verify
docker-compose logs -f parking-backend
```

### Environment Variables
```bash
# Update .env file
nano .env

# Restart services to apply changes
docker-compose down
docker-compose up --build -d
```

## 🌐 Network Configuration

### Docker Network
```bash
# View network details
docker network ls
docker network inspect smart-parking-network

# Test connectivity between containers
docker exec smart-parking-backend ping parking-db
```

## 📁 File Structure in Docker

### Container Paths
- **Application**: `/app`
- **Logs**: `/app/logs` (mounted to `./logs`)
- **Temporary files**: `/app/temp` (mounted to `./temp`)
- **Database data**: `/var/lib/postgresql/data` (Docker volume)

### Important Files
- **JAR file**: `/app/app.jar`
- **Static resources**: Inside JAR at `/app.jar`
- **Configuration**: Environment variables + `application-prod.properties`

## 🔒 Security Notes

### Production Deployment
1. **Change default passwords** in `.env`
2. **Use strong JWT secret**
3. **Enable HTTPS** in production
4. **Restrict network access**
5. **Regular security updates**

### Environment Variables
```bash
# Never commit .env to version control
echo ".env" >> .gitignore

# Use different values for production
POSTGRES_PASSWORD=your_production_password
SPRING_DATASOURCE_PASSWORD=your_production_password
JWT_SECRET=your_production_jwt_secret
PARKING_UPI_MERCHANT_ID=your_production_upi_id
```

## 🚀 Performance Tuning

### JVM Options
```bash
# Edit .env to customize JVM settings
JAVA_OPTS="-Xms1g -Xmx4g -XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
```

### Database Performance
```bash
# Connect to database and tune PostgreSQL settings
docker exec -it smart-parking-db psql -U postgres -d smart_parking_db
```

## 📱 Access from Different Platforms

### Windows
- Access via `http://localhost:8081`
- Docker Desktop handles port mapping

### WSL2
- Access via `http://localhost:8081`
- Same as Windows due to Docker Desktop integration

### macOS
- Access via `http://localhost:8081`
- Docker Desktop handles port mapping

### Linux
- Access via `http://localhost:8081`
- Direct Docker installation

## 🆘 Support

### Getting Help
```bash
# Check container status
docker-compose ps

# View system logs
docker-compose logs

# Check Docker system
docker system df
docker system events
```

### Reset Everything
```bash
# Complete reset (WARNING: Deletes all data)
docker-compose down -v --rmi all
docker system prune -a
docker volume prune
```

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    Docker Network                          │
│                 smart-parking-network                       │
├─────────────────────┬───────────────────────────────────────┤
│   parking-db        │         parking-backend               │
│   PostgreSQL 16     │       Spring Boot 3.2.0              │
│   Port: 5432       │       Port: 8081                      │
│   Volume: data     │       Health: /actuator/health        │
└─────────────────────┴───────────────────────────────────────┘
```

## 🔧 Key Technical Features

### Multi-stage Dockerfile
- **Build Stage**: Maven 3.9.6 + Eclipse Temurin JDK 21
- **Runtime Stage**: Eclipse Temurin JRE 21 Alpine
- **Security**: Non-root user (appuser:1001)
- **Optimization**: Layer caching, UTF-8 encoding, Asia/Kolkata timezone
- **Health Checks**: Built-in container health monitoring

### Docker Compose Services
- **parking-db**: PostgreSQL 16 Alpine with persistent volumes
- **parking-backend**: Spring Boot application with health dependencies
- **Network**: Dedicated bridge network (172.20.0.0/16)
- **Volumes**: PostgreSQL data, application logs, temporary files

### Environment Configuration
- **Database**: PostgreSQL connection parameters
- **Application**: Server port, Spring profiles
- **Security**: JWT secret, API keys
- **Performance**: Rate limiting, JVM options
- **Business**: UPI merchant details

## 📊 Application Features Preserved

### Core Parking Management
- ✅ 600 parking slots (300 per floor)
- ✅ Real-time slot status tracking
- ✅ Multi-floor organization (Ground & First)
- ✅ Human-readable slot IDs (AG01, AF01, etc.)

### Booking System
- ✅ Direct access (no authentication)
- ✅ 5-minute slot locking
- ✅ Vehicle type support (Cars, Bikes, SUVs, Vans)
- ✅ Booking confirmation downloads

### Exit Management
- ✅ Real-time synchronization
- ✅ Fee calculation
- ✅ Professional receipts (PDF/text)
- ✅ Staff confirmation dialogs

### Payment Integration
- ✅ UPI QR code generation
- ✅ iText PDF generation
- ✅ ZXing QR code support
- ✅ UTF-8 encoding support

### Technical Features
- ✅ Caffeine caching
- ✅ Rate limiting (token bucket)
- ✅ Scheduled lock cleanup
- ✅ Spring Boot Actuator
- ✅ Real-time data synchronization

## 🔒 Security & Production Considerations

### Container Security
- ✅ Non-root user execution
- ✅ Minimal runtime image (Alpine)
- ✅ Health checks and monitoring
- ✅ Graceful shutdown handling

### Data Security
- ✅ Environment variable externalization
- ✅ Persistent volume encryption (host-dependent)
- ✅ Network isolation via bridge network
- ✅ No sensitive data in images

### Operational Security
- ✅ Rate limiting protection
- ✅ Input validation preserved
- ✅ Database connection pooling
- ✅ Log rotation and management

## 📈 Performance Optimizations

### JVM Tuning
- ✅ Container support enabled
- ✅ Memory percentage-based allocation
- ✅ G1GC with string deduplication
- ✅ Optimized for container environments

### Database Performance
- ✅ Connection pooling (HikariCP)
- ✅ Optimized PostgreSQL settings
- ✅ Persistent volume performance
- ✅ Health monitoring

### Application Performance
- ✅ Caffeine caching enabled
- ✅ Optimized Docker layers
- ✅ Efficient resource utilization
- ✅ Fast startup times

## 🔧 Configuration Management

### Environment Variables
```bash
# Database
POSTGRES_DB=smart_parking_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
SPRING_DATASOURCE_URL=jdbc:postgresql://parking-db:5432/smart_parking_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8081

# Business Logic
PARKING_UPI_MERCHANT_ID=merchant@upi
PARKING_UPI_MERCHANT_NAME=Smart Parking System
JWT_SECRET=your-jwt-secret

# Performance
RATE_LIMIT_BOOKINGS=30
RATE_LIMIT_LOCKS=50
RATE_LIMIT_GENERAL=300
```

### Spring Profiles
- **prod**: Production configuration with environment variables
- **dev**: Development configuration (local PostgreSQL)
- **Docker**: Uses prod profile with container networking

---

**Last Updated**: $(date)
**Compatible with**: Docker Desktop 4.0+, Docker Compose 2.0+
**Tested Platforms**: Windows 11 + WSL2, Ubuntu 22.04, macOS 13+
