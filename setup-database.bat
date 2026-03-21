@echo off
echo Setting up PostgreSQL database...

REM Set the password for postgres user
set PGPASSWORD=postgres

REM Check if database exists, create if not
psql -h localhost -U postgres -c "SELECT 1 FROM pg_database WHERE datname='smart_parking_db';" | find "1" >nul
if %errorlevel% neq 0 (
    echo Creating database smart_parking_db...
    createdb -h localhost -U postgres smart_parking_db
    if %errorlevel% equ 0 (
        echo Database created successfully!
    ) else (
        echo Failed to create database
        pause
        exit /b 1
    )
) else (
    echo Database smart_parking_db already exists
)

REM Test connection
echo Testing database connection...
psql -h localhost -U postgres -d smart_parking_db -c "SELECT version();" >nul 2>&1
if %errorlevel% equ 0 (
    echo Database connection successful!
) else (
    echo Database connection failed
    pause
    exit /b 1
)

echo Database setup complete!
pause
