@echo off
echo Setting up PostgreSQL database...

REM Check if PGPASSWORD environment variable is set
IF NOT DEFINED PGPASSWORD (
    echo PostgreSQL password not set in environment.
    set /p PGPASSWORD=Enter PostgreSQL password for postgres user: 
)

REM Use the environment variable (either existing or user-provided)
echo Using password from environment variable...

REM First validate connectivity to the server
echo Testing PostgreSQL connectivity...
psql -h localhost -U postgres -c "\conninfo" >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Failed to connect to PostgreSQL server.
    echo Please check:
    echo - PostgreSQL server is running
    echo - Host and port are correct (localhost:5432)
    echo - Password is correct for postgres user
    pause
    exit /b 1
)
echo PostgreSQL connectivity successful!

REM Check if database exists, create if not
echo Checking if database smart_parking_db exists...
psql -h localhost -U postgres -c "SELECT 1 FROM pg_database WHERE datname='smart_parking_db';" | find "1" >nul
if %errorlevel% neq 0 (
    echo Database smart_parking_db does not exist. Creating...
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

REM Test connection to the specific database
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
