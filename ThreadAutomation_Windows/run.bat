@echo off
echo ==================================================
echo Thread Automation - Direct Run
echo ==================================================
echo.

echo Starting Thread Automation...
echo.

echo Checking and installing required packages...
python -m pip install --upgrade pip
python -m pip install selenium webdriver-manager requests

if %errorlevel% neq 0 (
    echo ERROR: Package installation failed.
    echo Please make sure Python is installed.
    echo Download from: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo.
echo SUCCESS: All packages installed!
echo.
echo Starting the program...
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






