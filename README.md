# Smart Parking Slot Reservation System

A comprehensive full-stack parking management system built with Java 21, Spring Boot, PostgreSQL, and modern web technologies.

## 🚀 Latest Features & Enhancements

### 🔐 Exit Page Authentication (NEW!)
- **Client-side Authentication**: Simple credentials-based access control
- **Session Management**: Uses sessionStorage for authentication state
- **Logout Functionality**: Clear session and redirect to auth page
- **View Booking Status**: Open booking page in view-only mode in new tab

**Default Credentials:**
- Username: `admin`
- Password: `admin123`

### ✨ Professional UI & Design System
- **Modern Design System**: Unified CSS variables and components
- **Responsive Layout**: Mobile-friendly interface
- **Ghost Badges**: Subtle status indicators (Available, Booked, Locked)
- **Skeleton Loaders**: Smooth loading experience

### 🎯 Advanced Admin Dashboard
- **Real-time Synchronization**: Instant updates after vehicle exits
- **Complete Data Display**: Phone numbers, vehicle types, duration, hours charged
- **Clean UI**: Removed duplicate refresh buttons for better UX
- **Live Statistics**: Today's exits, revenue, and active bookings

### 🔧 Enhanced Exit Process (NEW!)
- **Staff Confirmation Dialog**: Professional confirmation before vehicle release
- **Release Confirmation Modal**: Clear feedback with receipt download options
- **Controlled Receipt Download**: Staff-controlled download timing
- **Professional Workflow**: Enhanced user experience for parking operations
- **Complete Data Preservation**: Full booking details maintained through exit process

### 📊 Real-time Data Synchronization (NEW!)
- **Multi-method Communication**: BroadcastChannel, localStorage events, and polling
- **Duplicate Prevention**: Advanced notification deduplication
- **Complete Exit Data**: Phone numbers, vehicle types, and all booking details preserved
- **Instant Updates**: Admin dashboard updates immediately after vehicle exit
- **Data Integrity**: Prevents duplicate entries and notifications

### 🎯 Advanced Booking System
- **Vehicle Re-booking Support**: Same vehicle can book again after exit
- **Custom JSON Deserializer**: Bulletproof handling of booking requests
- **Enhanced Rate Limiting**: 50x more bookings (3→30/hour), 10x more locks (5→50/hour)
- **Debug Logging**: Comprehensive troubleshooting capabilities
- **5-minute Slot Lock**: Extended lock duration for better user experience

### 📄 Professional Receipt System
- **Dual Download Methods**: By booking ID or booking code
- **UTF-8 Encoding**: Proper file handling and readability
- **Smart Receipt Generation**: Works for both active and exited bookings
- **User-friendly Filenames**: Uses booking codes for easy identification
- **Enhanced Content**: Dynamic fee calculation and professional formatting

### 🔧 Technical Improvements
- **Enhanced Error Handling**: Better error messages and recovery
- **Debug Endpoints**: `/api/exit/debug/bookings` and `/api/exit/debug/vehicle/{number}`
- **Active Booking Queries**: Prevents double-booking with proper checks
- **Improved File Downloads**: Reliable receipt download mechanism
- **Synchronized Components**: All frontend and backend components aligned

## Core Functionality
- **Real-time Parking Slot Management**: View availability across 2 floors (200 slots total)
- **Temporary Slot Locking**: 5-minute lock mechanism to prevent double booking (enhanced from 2 minutes)
- **Strict Input Validation**: Vehicle number format (XX00XX0000), name validation, phone number validation
- **Concurrent Access Control**: Row-level locking prevents race conditions
- **Auto-refresh**: Frontend updates every 10 seconds (optimized from 5 seconds)

## User Interface & Design System

### 🎨 Professional Design System
- **Cambria Typography**: Professional font family across all pages
- **Bold Headings**: Cambria Bold for titles, page headers, and section headings
- **Consistent Styling**: Normal Cambria weight for body text and descriptions
- **Modern CSS Variables**: Unified design tokens for colors, spacing, and typography
- **Responsive Design**: Mobile-friendly interface with consistent breakpoints

### 📱 Page-Specific Features
- **Main Booking Page**: Real-time slot grid with visual status indicators
- **Admin Dashboard**: Live statistics, exit tracking, revenue reports, and professional tables
- **Exit Management**: Staff confirmation dialogs, active bookings tracking, and receipt generation
- **Authentication Page**: Role-based login with modern form styling

### 🔄 Real-time Updates
- **Auto-refresh**: Frontend updates every 10 seconds (optimized performance)
- **Live Status**: Real-time slot availability and lock countdown timers
- **Instant Notifications**: Admin dashboard updates immediately after vehicle exit
- **Visual Feedback**: Color-coded status and loading indicators

### 🎯 User Experience
- **Intuitive Navigation**: Clear page flow from booking to exit
- **Form Validation**: Real-time validation with helpful error messages
- **Confirmation Dialogs**: Professional confirmation for critical actions
- **Receipt Options**: Multiple download methods for user convenience
- **Role-based Access**: Admin and staff interfaces with appropriate functionality

## Security & Performance
- **JWT Authentication**: Secure token-based authentication for admin and staff
- **Enhanced Rate Limiting**: User-friendly limits (50 locks, 30 bookings, 300 general requests per hour)
- **Input Validation**: Both frontend and backend validation with custom JSON deserializer
- **Concurrent Control**: Database-level locking for slot operations
- **Debug Support**: Comprehensive logging and troubleshooting endpoints

## Technology Stack

### Backend
- **Java 21**: Latest Java features and performance
- **Spring Boot 3.2.0**: Modern Spring framework
- **PostgreSQL**: Robust relational database
- **Spring Security**: Authentication and authorization
- **JWT**: Token-based authentication
- **Spring Data JPA**: Database operations
- **Maven**: Dependency management

### Frontend
- **HTML5/CSS3**: Modern web standards
- **Vanilla JavaScript**: No framework dependencies
- **Responsive Design**: Mobile-friendly interface
- **Real-time Updates**: Fetch API for communication

## Database Configuration

### PostgreSQL Setup
```sql
-- Create database
CREATE DATABASE smart_parking_db;

-- The application will auto-create tables on startup
```

### Connection Details (configured in application.properties)
- **Host**: localhost:5432
- **Database**: smart_parking_db
- **Username**: postgres
- **Password**: Nanda@123

## Installation & Setup

### Prerequisites
- Java 21 or higher
- Maven 3.6+
- PostgreSQL 12+
- Git

### Steps

1. **Clone the repository**
```bash
git clone <repository-url>
cd SmartParkingSystem
```

2. **Set up PostgreSQL database**
```sql
-- Connect to PostgreSQL and run:
CREATE DATABASE smart_parking_db;
```

3. **Run the application**

Development mode (H2 database):
```bash
mvn spring-boot:run
```

Production mode (PostgreSQL):
```powershell
mvn spring-boot:run "-Dspring.profiles.active=prod"
```

4. **Access the application**
- **Main Application**: http://localhost:8081/index.html
- **Exit Management**: http://localhost:8081/exit.html
- **Authentication**: http://localhost:8081/auth.html

## 📡 API Endpoints

### Parking Management
- `GET /api/parking-slots` - Get all parking slots with status
- `POST /api/parking-slots/lock/{slotId}` - Lock a parking slot
- `POST /api/parking-slots/book` - Create a new booking
- `POST /api/parking-slots/book-debug` - Debug endpoint for booking requests

### Exit Management
- `GET /api/exit/active-bookings` - Get all active bookings
- `POST /api/exit/process/{bookingId}` - Process vehicle exit
- `GET /api/exit/receipt/{bookingId}` - Download receipt by booking ID
- `GET /api/exit/receipt/by-code/{bookingCode}` - Download receipt by booking code

### Authentication
- `POST /api/auth/login` - User authentication (for admin)
- `POST /api/auth/refresh` - Refresh JWT token

## 🔧 Configuration

### Application Properties Files
- `application.properties` - Development configuration (H2 database)
- `application-prod.properties` - Production configuration (PostgreSQL)

### Environment Variables
```bash
# JWT Secret (generate with: openssl rand -base64 32)
export JWT_SECRET="your-secret-key"

# Database (optional - can use properties file)
export DB_URL="jdbc:postgresql://localhost:5432/smart_parking_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your-password"
```

## 🐛 Troubleshooting

### Common Issues & Solutions

#### 1. Receipt Download Shows "Site Unavailable"
**Solution**: The system now supports both booking ID and booking code downloads:
- Use booking ID: `/api/exit/receipt/{bookingId}`
- Use booking code: `/api/exit/receipt/by-code/{bookingCode}`

#### 2. "Too Many Requests" Error
**Solution**: Rate limiting has been enhanced:
- Bookings: 30 per hour (increased from 3)
- Locks: 50 per hour (increased from 5)
- General requests: 300 per hour (increased from 60)

#### 3. Vehicle Cannot Be Re-booked After Exit
**Solution**: The system now supports vehicle re-booking:
- Only active bookings are checked for duplicates
- Exited vehicles can book again immediately

#### 4. Slot Lock Expires Too Quickly
**Solution**: Lock duration extended to 5 minutes:
- Previous: 2 minutes
- Current: 5 minutes

### Debug Tools
- **View All Bookings**: `GET /api/exit/debug/bookings`
- **Check Vehicle Status**: `GET /api/exit/debug/vehicle/{vehicleNumber}`
- **Debug Booking Requests**: `POST /api/parking-slots/book-debug`

## 📅 Recent Updates (March 2026)

### 🎉 Major UI & UX Enhancements Released
- **Professional Typography**: Cambria font family implemented across all pages
- **Bold Headings**: Cambria Bold for all titles and headings for better hierarchy
- **Admin Dashboard Cleanup**: Removed duplicate refresh buttons, improved data display
- **Real-time Synchronization**: Advanced multi-method communication for instant updates
- **Complete Data Preservation**: Phone numbers, vehicle types, and all booking details maintained
- **Modern Design System**: Unified CSS variables and components for consistency

### 🔧 Technical Improvements
- **Advanced Synchronization**: BroadcastChannel, localStorage events, and polling mechanisms
- **Duplicate Prevention**: Advanced notification deduplication using sessionStorage and Sets
- **Enhanced Exit Process**: Professional staff confirmation dialogs and release workflows
- **Vehicle Re-booking Support**: Same vehicles can book again after exit
- **Professional Receipt System**: Dual download methods with UTF-8 encoding
- **Enhanced Rate Limiting**: 50x more bookings, 10x more locks for better UX

### 🐛 Bug Fixes
- **Fixed Font Inconsistency**: Cambria font now consistently applied across all pages
- **Fixed Duplicate UI Elements**: Removed extra refresh buttons from admin dashboard
- **Fixed Data Loss**: Complete booking details now preserved through exit process
- **Fixed "Unknown" Values**: Phone numbers and vehicle types now display correctly
- **Fixed Sync Issues**: Admin dashboard updates immediately after vehicle exit
- **Fixed Duplicate Notifications**: Advanced deduplication prevents multiple updates

### Default Users

> **⚠️ SECURITY WARNING**: These default credentials are **INSECURE** and intended for development only. **MUST be changed before any production deployment!**
>
> **Required Actions for Production:**
> - **Rotate or remove default accounts** immediately
> - Set admin credentials via **environment variables** or secure setup flow
> - Enforce **strong password policy** (minimum 12 characters, mixed case, numbers, symbols)
> - Disable or regenerate default user during **first-run provisioning**
> - Never commit actual credentials to version control

#### Exit Page
- **Username**: admin
- **Password**: admin123

#### Admin Dashboard (JWT)
- **Username**: admin
- **Password**: admin123

## Project Structure

```
SmartParkingSystem/
├── src/
│   ├── main/
│   │   ├── java/com/smartparking/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── dto/           # Data transfer objects
│   │   │   ├── entity/        # JPA entities (Booking, ParkingSlot, User)
│   │   │   ├── enums/         # Enumerations
│   │   │   ├── filter/        # Security filters
│   │   │   ├── repository/    # Data repositories
│   │   │   ├── scheduler/     # Scheduled tasks
│   │   │   ├── security/      # Security configuration
│   │   │   ├── service/       # Business logic
│   │   │   └── config/        # Web configuration
│   │   └── resources/
│   │       ├── static/        # Frontend files
│   │       │   ├── styles/     # CSS design system
│   │       │   ├── auth.html   # Exit authentication
│   │       │   ├── exit.html   # Exit management
│   │       │   └── index.html  # Main booking page
│   │       ├── application.properties         # Application configuration (PostgreSQL)
│   └── test/                  # Test files
├── pom.xml                    # Maven configuration
├── setup-database.bat        # Database setup script
├── .gitignore               # Git ignore rules
└── README.md                # Project documentation
```

## Security Implementation

### JWT Authentication
- **Token-based authentication** using JSON Web Tokens (JWT)
- **Token expiration**: 24 hours (configurable via `jwt.expiration`)
- **Secret key**: Configured via environment variable `JWT_SECRET` or default in properties
- **Algorithm**: HS256 for signing tokens

### Implementation Details
- **Filter**: `JwtAuthenticationFilter` - Intercepts requests and validates JWT tokens
- **Entry Point**: `JwtAuthenticationEntryPoint` - Handles unauthorized access attempts
- **Util**: `JwtUtil` - Token generation, validation, and extraction
- **Security Config**: `SecurityConfig` - Configures security rules and filters

### Protected Endpoints
- `/api/admin/**` - Admin-only endpoints
- `/api/staff/**` - Staff and Admin access
- All API endpoints except public ones require authentication

### Public Endpoints
- `/api/parking-slots/**` - View parking slots (GET)
- `/api/auth/login` - Login endpoint
- `/` - Static resources

## Rate Limiting Implementation

### Token Bucket Algorithm
Implements a token bucket algorithm for rate limiting with the following features:

### Configuration (requests per hour)
| Endpoint Type | Limit | Purpose |
|--------------|-------|---------|
| Lock | 50 | Slot locking attempts |
| Book | 30 | Booking confirmations |
| View | 200 | Page views and slot queries |
| Receipt | 50 | Receipt downloads |
| General | 300 | All other requests |

### Key Components
- **RateLimitingFilter**: Servlet filter that intercepts all requests
- **RateLimitingService**: Core service implementing token bucket logic
- **RateLimitConfig**: Configuration properties for limits
- **Cleanup**: Automatic cleanup of expired buckets every 5 minutes

### Features
- **IP-based tracking**: Limits applied per client IP address
- **Thread-safe**: Concurrent access handling
- **Burst handling**: Allows short bursts while maintaining overall limits
- **Automatic cleanup**: Removes inactive IP records to prevent memory leaks

### Response Headers
```
X-RateLimit-Limit: 50
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 3600
```

## License

This project is licensed under the MIT License.

## Support

For support and questions, please contact the development team or create an issue in the repository.
