# 🔐 Security Configuration Guide

## Environment Variables Setup

### Required Environment Variables

#### Database Configuration
```bash
export DB_URL="jdbc:postgresql://localhost:5432/smart_parking_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_secure_database_password"
```

#### JWT Configuration
```bash
# Generate a cryptographically strong JWT secret (minimum 256 bits)
export JWT_SECRET="$(openssl rand -base64 32)"
```

### Deployment Instructions

#### 1. Generate Secure JWT Secret
```bash
# Using OpenSSL (recommended)
openssl rand -base64 32

# Using Java KeyTool
keytool -genseckey -alias jwtkey -keyalg AES -keysize 256 -storetype JCEKS

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

#### 2. Set Environment Variables
```bash
# Production deployment
export DB_URL="jdbc:postgresql://your-db-host:5432/smart_parking_db"
export DB_USERNAME="your_db_user"
export DB_PASSWORD="your_secure_db_password"
export JWT_SECRET="your_generated_jwt_secret_here"

# Development (optional - defaults will work)
export JWT_SECRET="dev_secret_only_for_local_testing"
```

#### 3. Application Startup
```bash
# With environment variables
java -jar smart-parking-system.jar

# Or with Spring Boot profile
java -jar smart-parking-system.jar --spring.profiles.active=prod
```

## Security Best Practices

### 🔒 JWT Secret Management
1. **Never commit secrets to version control**
2. **Use environment variables or secret manager**
3. **Rotate secrets regularly**
4. **Use minimum 256-bit secrets**
5. **Store secrets in secure vaults (AWS Secrets Manager, Azure Key Vault, etc.)**

### 🗄️ Database Security
1. **Use strong, unique passwords**
2. **Limit database user permissions**
3. **Enable SSL/TLS connections**
4. **Regular password rotation**
5. **Use connection pooling**

### 🚀 Production Deployment
1. **Set secure JWT secret**: `export JWT_SECRET="$(openssl rand -base64 32)"`
2. **Configure database credentials**: Use environment variables, not hardcoded values
3. **Enable HTTPS**: Configure SSL certificates
4. **Set up monitoring**: Log security events
5. **Regular security updates**: Keep dependencies updated

## Configuration Files

### application.properties.example
This file contains template values for reference. **Do not use this file directly in production.**

### application.properties (gitignored)
This file contains actual configuration values and should **NOT** be committed to version control.

## Authentication System

### Development Mode
- Uses demo authentication (`/auth-demo.html`)
- Works on localhost and development domains
- Not suitable for production

### Production Mode
- Requires server-side authentication implementation
- Form is disabled and shows clear message
- Contact administrator for setup

### Implementing Production Authentication
1. Create `/api/auth/login` endpoint
2. Implement JWT or session-based authentication
3. Use secure token storage (HttpOnly cookies recommended)
4. Add token refresh mechanism
5. Implement proper logout functionality

## Security Monitoring

### Logs to Monitor
- Authentication failures
- Unauthorized access attempts
- Database connection issues
- JWT token validation failures

### Security Headers
- X-Content-Type-Options: nosniff
- X-Frame-Options: DENY
- X-XSS-Protection: enabled
- Cache-Control: no-cache for sensitive data

## Emergency Procedures

### Compromised JWT Secret
1. Immediately rotate the JWT secret
2. Invalidate all existing tokens
3. Force users to re-authenticate
4. Review access logs for suspicious activity

### Compromised Database Credentials
1. Change database password immediately
2. Review database access logs
3. Update application environment variables
4. Restart application with new credentials

## Contact Information

For security issues or questions:
- System Administrator: [admin-contact@company.com]
- Security Team: [security@company.com]
