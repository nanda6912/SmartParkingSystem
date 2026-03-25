# Smart Parking Management System

A comprehensive parking management system built with Java 21, Spring Boot, PostgreSQL, and modern web technologies.

## 🚀 Features

### ✨ Professional UI & Design System
- **Modern Design System**: Unified CSS variables and components
- **Responsive Layout**: Mobile-friendly interface
- **Ghost Badges**: Subtle status indicators (Available, Booked, Locked)
- **Skeleton Loaders**: Smooth loading experience

### 🎯 Exit Management Dashboard
- **Real-time Synchronization**: Instant updates after vehicle exits
- **Complete Data Display**: Phone numbers, vehicle types, duration, hours charged
- **Live Statistics**: Today's exits, revenue, and active bookings
- **Fee Calculation**: Automatic parking fee computation
- **Receipt Generation**: Professional receipt downloads

### 🔧 Enhanced Exit Process
- **Staff Confirmation Dialog**: Professional confirmation before vehicle release
- **Release Confirmation Modal**: Clear feedback with receipt download options
- **Controlled Receipt Download**: Staff-controlled download timing
- **Professional Workflow**: Enhanced user experience for parking operations

### 📊 Parking Slot Management
- **Real-time Slot Status**: Available, Booked, Locked states
- **Floor-based Organization**: Multi-floor parking structure
- **Vehicle Type Support**: Cars, Bikes, SUVs, Vans
- **Booking Management**: Create and manage parking reservations
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
- **Exit Management**: Staff confirmation dialogs, active bookings tracking, and receipt generation
- **Direct Access**: No authentication required - immediate access to all features

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
- **Direct Access**: Immediate access to all features without login

## Performance & Reliability
- **Enhanced Rate Limiting**: User-friendly limits (50 locks, 30 bookings, 300 general requests per hour)
- **Input Validation**: Both frontend and backend validation with custom JSON deserializer
- **Concurrent Control**: Database-level locking for slot operations
- **Debug Support**: Comprehensive logging and troubleshooting endpoints

## Technology Stack

### Backend
- **Java 21**: Latest Java features and performance
- **Spring Boot 3.2.0**: Modern Spring framework
- **PostgreSQL**: Robust relational database
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

## 🔧 Configuration

### Application Properties Files
- `application.properties` - Development configuration (H2 database)
- `application-prod.properties` - Production configuration (PostgreSQL)

### Environment Variables
```bash
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

## 📅 Latest Updates (March 2026)

### 🚀 Authentication Removal - Direct Access System
- **Removed Authentication System**: Complete removal of login/logout functionality
- **Direct Access**: Immediate access to all parking management features
- **Simplified Architecture**: No user management, roles, or session handling
- **Clean Database**: Removed users table and authentication-related data
- **Streamlined UI**: Removed login pages and authentication redirects

### ✨ Professional UI & Design System
- **Modern Design System**: Unified CSS variables and components
- **Responsive Layout**: Mobile-friendly interface
- **Ghost Badges**: Subtle status indicators (Available, Booked, Locked)
- **Skeleton Loaders**: Smooth loading experience
- **Professional Typography**: Cambria font family across all pages

### 🎯 Enhanced Exit Management
- **Real-time Synchronization**: Advanced multi-method communication
- **Complete Data Display**: Phone numbers, vehicle types, duration, hours charged
- **Live Statistics**: Today's exits, revenue, and active bookings
- **Fee Calculation**: Automatic parking fee computation
- **Receipt Generation**: Professional receipt downloads
- **Staff Confirmation Dialogs**: Professional confirmation before vehicle release

### 📊 Parking Slot Management
- **Real-time Slot Status**: Available, Booked, Locked states
- **Floor-based Organization**: Multi-floor parking structure (200 slots total)
- **Human-Readable Slot IDs**: AG06, BG15, AF22 format for easy identification
- **Vehicle Type Support**: Cars, Bikes, SUVs, Vans
- **Booking Management**: Create and manage parking reservations
- **5-minute Slot Lock**: Extended lock duration for better user experience
- **Vehicle Re-booking Support**: Same vehicles can book again after exit
- **Clean Receipt Format**: Simplified slot display (shows only slot ID like AG06)
- **Optimized Database Structure**: Efficient slot and booking management

### 🔧 Technical Improvements
- **Enhanced Rate Limiting**: 50 locks, 30 bookings, 300 general requests per hour
- **Input Validation**: Both frontend and backend validation
- **Concurrent Control**: Database-level locking for slot operations
- **Debug Support**: Comprehensive logging and troubleshooting endpoints
- **Professional Receipt System**: Dual download methods with UTF-8 encoding
- **Streamlined Architecture**: Direct access without authentication overhead
- **Optimized Performance**: Fast response times and efficient database queries

## 🏗️ Project Structure

```
SmartParkingSystem/
├── src/
│   ├── main/
│   │   ├── java/com/smartparking/
│   │   │   ├── controller/     # REST controllers (Parking, Exit, Home)
│   │   │   ├── dto/           # Data transfer objects
│   │   │   ├── entity/        # JPA entities (Booking, ParkingSlot)
│   │   │   ├── enums/         # Enumerations (SlotStatus, VehicleType)
│   │   │   ├── filter/        # Request filters (Rate limiting)
│   │   │   ├── ratelimit/     # Rate limiting implementation
│   │   │   ├── repository/    # Data repositories
│   │   │   ├── scheduler/     # Scheduled tasks
│   │   │   └── service/       # Business logic services
│   │   └── resources/
│   │       ├── static/        # HTML pages and assets
│   │       │   ├── index.html     # Parking management page
│   │       │   ├── exit.html      # Exit management page
│   │       │   └── styles/        # CSS stylesheets
│   │       └── application.properties  # Configuration
├── pom.xml                    # Maven dependencies
├── README.md                  # Project documentation
└── setup-database.bat         # Database setup script
```

## 🎯 Key Features

### Direct Access System
- **No Authentication Required**: Immediate access to all features
- **Simplified User Experience**: Direct navigation to parking and exit management
- **Clean Interface**: Streamlined UI without login barriers

### Core Functionality
- **Real-time Parking Management**: 200 slots across 2 floors with human-readable IDs
- **Vehicle Exit Processing**: Fee calculation and receipt generation
- **Booking System**: Slot locking and reservation management
- **Data Synchronization**: Real-time updates across all interfaces
- **Professional Receipts**: Clean format with slot ID display (e.g., AG06)
- **Multi-floor Support**: Organized parking structure with clear slot identification

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
X-RateLimit-Reset: 1640995200
```

## 🎯 Slot ID Format

### Structure
The parking system uses a human-readable slot ID format for easy identification:

**Format**: `[Area][Floor][Number]`

### Examples
- **AG06**: Area A, Ground Floor (1), Slot 06
- **BG15**: Area B, Ground Floor (1), Slot 15  
- **AF22**: Area A, Floor 2, Slot 22
- **DF08**: Area D, Floor 2, Slot 08

### Floor Layout
- **Floor 1**: 5 areas (A-E), 20 slots each = 100 slots
  - AG01-AG20, BG01-BG20, CG01-CG20, DG01-DG20, EG01-EG20
- **Floor 2**: 5 areas (A-E), 20 slots each = 100 slots
  - AF01-AF20, BF01-BF20, CF01-CF20, DF01-DF20, EF01-EF20

### Receipt Display
- **Before**: SLOT ID: AG06, SLOT NUMBER: 6, FLOOR: 1 (confusing)
- **After**: SLOT NUMBER: AG06 (clean and clear)

### Internal Structure
The system maintains both formats internally:
- `slot_id`: Human-readable (AG06) - shown to users
- `slot_number`: Numeric (6) - used for sorting
- `floor`: Numeric (1) - used for grouping

This provides the best of both worlds: user-friendly display and efficient database operations.

## License

This project is licensed under the MIT License.

## Support

For support and questions, please contact the development team or create an issue in the repository.
