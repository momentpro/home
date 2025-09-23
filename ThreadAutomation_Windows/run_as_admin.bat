@echo off
echo ==================================================
echo Thread Automation - Run as Administrator
echo ==================================================
echo.

echo This script will run with administrator privileges.
echo.

echo Checking Python installation...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Python is not installed.
    echo Please install Python first from: https://www.python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation.
    pause
    exit /b 1
)

echo SUCCESS: Python is installed.
echo.

echo Installing required packages...
python -m pip install --upgrade pip
python -m pip install selenium webdriver-manager requests

if %errorlevel% neq 0 (
    echo ERROR: Package installation failed.
    pause
    exit /b 1
)

echo.
echo SUCCESS: All packages installed!
echo.
echo Starting Thread Automation...
echo.

python thread_automation.py

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Program execution failed.
    echo Please check the error message above.
    pause
)

echo.
echo Program finished.
pause






