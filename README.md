# Smart Parking Slot Reservation System

A comprehensive full-stack parking management system built with Java 21, Spring Boot, PostgreSQL, and modern web technologies.

## Features

### Core Functionality
- **Real-time Parking Slot Management**: View availability across 2 floors (200 slots total)
- **Temporary Slot Locking**: 2-minute lock mechanism to prevent double booking
- **Strict Input Validation**: Vehicle number format (XX00XX0000), name validation, phone number validation
- **Concurrent Access Control**: Row-level locking prevents race conditions
- **Auto-refresh**: Frontend updates every 5 seconds

### Security & Performance
- **JWT Authentication**: Secure token-based authentication for admin and staff
- **Rate Limiting**: Protection against API abuse and brute force attempts
- **Input Validation**: Both frontend and backend validation
- **Concurrent Control**: Database-level locking for slot operations

### User Interface
- **Modern Responsive Design**: Clean, intuitive interface
- **Real-time Updates**: Live slot status and lock countdown timers
- **Visual Feedback**: Color-coded slot status (Available/Green, Locked/Yellow, Occupied/Red)
- **Form Validation**: Client-side validation with error messages

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
- **Main Application**: http://localhost:8081/api/index.html
- **API Base URL**: http://localhost:8081/api

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
- **Token Bucket Algorithm**: IP-based throttling with configurable limits
- **Multi-layer Protection**: Different limits per endpoint type (Lock: 5/min, Book: 3/min, View: 120/min)
- **Burst Handling**: Allows natural user behavior while preventing abuse
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
│   │   ├── controller/     # REST controllers
│   │   ├── dto/           # Data transfer objects
│   │   ├── entity/        # JPA entities
│   │   ├── enums/         # Enumerations
│   │   ├── repository/    # Data repositories
│   │   ├── scheduler/     # Scheduled tasks
│   │   ├── security/      # Security configuration
│   │   └── service/       # Business logic
│   └── resources/
│       ├── static/        # Frontend files
│       └── application.properties
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
