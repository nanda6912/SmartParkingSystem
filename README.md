# Smart Parking Slot Reservation System

A comprehensive full-stack parking management system built with Java 21, Spring Boot, PostgreSQL, and modern web technologies.

## 🚀 Latest Features & Enhancements

### ✨ Professional UI & Design System (NEW!)
- **Cambria Font Family**: Professional typography across all pages
- **Bold Headings**: Cambria Bold for all titles and headings
- **Consistent Styling**: Normal Cambria for body text
- **Modern Design System**: Unified CSS variables and components
- **Responsive Layout**: Mobile-friendly interface with consistent styling

### 🎯 Advanced Admin Dashboard (NEW!)
- **Real-time Synchronization**: Instant updates after vehicle exits
- **Complete Data Display**: Phone numbers, vehicle types, duration, hours charged
- **Clean UI**: Removed duplicate refresh buttons for better UX
- **Live Statistics**: Today's exits, revenue, and active bookings
- **Professional Tables**: Clean, organized data presentation

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

3. **Build and run the application**
```bash
mvn clean install
mvn spring-boot:run
```

4. **Access the application**
- **Main Application**: http://localhost:8081/index.html
- **API Base URL**: http://localhost:8081/api
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

### Receipt Downloads
- `GET /receipt/download/{bookingCode}` - Download booking confirmation receipt
- `GET /api/exit/receipt/{bookingId}` - Download exit receipt by ID
- `GET /api/exit/receipt/by-code/{bookingCode}` - Download exit receipt by code
- `GET /api/exit/calculate-fee/{bookingId}` - Calculate parking fee

### Debug & Monitoring
- `GET /api/exit/debug/bookings` - View all bookings for troubleshooting
- `GET /api/exit/debug/vehicle/{vehicleNumber}` - Check vehicle booking status
- `GET /api/receipt/download/{bookingCode}` - Download booking receipt (main page)

### Authentication
- `POST /api/auth/login` - User authentication
- `POST /api/auth/refresh` - Refresh JWT token

## 🔧 Configuration

### Application Properties
```properties
# Enhanced Rate Limiting (Updated for better user experience)
rate.limit.lock.requests=50      # 50 locks per hour
rate.limit.book.requests=30      # 30 bookings per hour  
rate.limit.view.requests=200     # 200 views per hour
rate.limit.receipt.requests=50   # 50 receipts per hour
rate.limit.general.requests=300  # 300 general requests per hour

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/smart_parking_db
spring.datasource.username=postgres
spring.datasource.password=your_password

# JWT Configuration
jwt.secret=mySecretKey123456789012345678901234567890
jwt.expiration=86400000
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

## Default Users

The system automatically creates default users on first startup:

### Admin User
- **Username**: admin
- **Password**: admin123
- **Role**: Administrator

### Staff User
- **Username**: staff
- **Password**: staff123
- **Role**: Staff

## API Endpoints

### Public Endpoints
- `GET /api/parking-slots` - Get all parking slots
- `GET /api/parking-slots/floor/{floor}` - Get slots by floor
- `POST /api/parking-slots/lock/{slotId}` - Lock a slot for 2 minutes
- `POST /api/parking-slots/book` - Book a slot

### Authentication Required
- `POST /api/auth/login` - User login
- `GET /api/admin/dashboard` - Admin dashboard
- `GET /api/staff/bookings` - View active bookings
- `POST /api/exit/vehicle` - Release vehicle and calculate fees

## Parking Slot Status

### Available (Green)
- Slot is free for booking
- Click to lock and start booking process

### Locked (Yellow)
- Slot is temporarily locked (2 minutes)
- Countdown timer shows remaining time
- Automatically reverts to Available if not booked

### Occupied (Red)
- Slot is currently booked
- Cannot be selected until vehicle exits

## Booking Process

1. **Select Slot**: Click on an available (green) slot
2. **Lock Slot**: System locks the slot for 2 minutes
3. **Complete Form**: Fill in vehicle and customer details
4. **Validation**: System validates all input fields
5. **Confirm Booking**: Submit to complete the booking
6. **Slot Occupied**: Slot status changes to Occupied

## Input Validation Rules

### Vehicle Number
- Format: XX00XX0000 (e.g., MH12AB1234)
- Must be uppercase
- Unique across all bookings

### Customer Name
- Maximum 20 characters
- Alphabets and spaces only
- Required field

### Phone Number
- Exactly 10 digits
- Numeric characters only
- Required field

### Vehicle Type
- Car, Bike, SUV, Van
- Required selection

## Security Features

### Authentication
- JWT-based authentication
- Role-based access control (Admin/Staff)
- Secure password hashing with BCrypt

### Rate Limiting
- **Enhanced Token Bucket Algorithm**: IP-based throttling with user-friendly limits
- **Multi-layer Protection**: Different limits per endpoint type (Lock: 50/hour, Book: 30/hour, View: 200/hour)
- **User-Friendly Approach**: Eliminates "too many requests" warnings for normal usage
- **Burst Handling**: Allows natural user behavior while preventing abuse
- **Recent Updates**: 50x more bookings (3→30/hour), 10x more locks (5→50/hour)
- **Thread-safe Implementation**: Concurrent access support with automatic cleanup
- **Monitoring**: Real-time statistics and health check endpoints

### Data Validation
- Server-side validation with Bean Validation
- SQL injection prevention
- XSS protection

## Concurrency Control

### Database Locking
- Pessimistic locking for slot operations
- Prevents double booking scenarios
- Row-level locking with JPA @Lock

### Transaction Management
- ACID compliance for all operations
- Rollback on errors
- Consistent data state

## Scheduled Tasks

### Lock Release
- Runs every 30 seconds
- Automatically releases expired locks
- Updates slot status to Available

## Error Handling

### Frontend Validation
- Real-time form validation
- User-friendly error messages
- Visual feedback for errors

### Backend Validation
- Comprehensive input validation
- Proper HTTP status codes
- Detailed error responses

## Performance Features

### Caching
- Application-level caching
- Reduced database queries
- Improved response times

### Auto-refresh
- Frontend updates every 5 seconds
- Real-time slot status
- Efficient data loading

## Future Enhancements

### Planned Features
- PDF receipt generation
- Advanced admin dashboard
- Revenue analytics
- Mobile app support
- Payment integration
- Vehicle exit automation
- Advanced reporting

### Scalability
- Microservices architecture
- Load balancing
- Database sharding
- Caching strategies

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Verify PostgreSQL is running
   - Check connection credentials
   - Ensure database exists

2. **Port Already in Use**
   - Change server.port in application.properties
   - Kill existing process on port 8080

3. **Maven Build Errors**
   - Check Java version (requires Java 21)
   - Update Maven dependencies
   - Clean and rebuild project

### Logs
- Application logs available in console
- Database query logging enabled
- Security events logged

## Development

### Project Structure
```
src/
├── main/
│   ├── java/com/smartparking/
│   │   ├── controller/     # REST controllers (Parking, Exit, Auth, Receipt, etc.)
│   │   ├── dto/           # Data transfer objects
│   │   ├── entity/        # JPA entities
│   │   ├── enums/         # Enumerations
│   │   ├── filter/        # Security filters (Rate Limiting, JWT)
│   │   ├── ratelimit/     # Rate limiting implementation
│   │   ├── repository/    # Data repositories
│   │   ├── scheduler/     # Scheduled tasks
│   │   ├── security/      # Security configuration
│   │   ├── service/       # Business logic
│   │   └── config/       # Web configuration
│   └── resources/
│       ├── static/        # Frontend files (HTML, CSS, JS)
│       │   ├── styles/    # CSS design system
│       │   ├── admin.html # Admin dashboard
│       │   ├── auth.html  # Authentication page
│       │   ├── exit.html  # Exit management
│       │   └── index.html # Main booking page
│       └── application.properties
├── test/                 # Test files
├── pom.xml              # Maven configuration
├── README.md            # Project documentation
└── API_ANALYSIS.md      # API documentation
```

### Contributing
1. Fork the repository
2. Create feature branch
3. Make changes
4. Add tests
5. Submit pull request

## License

This project is licensed under the MIT License.

## Support

For support and questions, please contact the development team or create an issue in the repository.
