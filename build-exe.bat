@echo off
echo ========================================================
echo   Building Native Windows Executable for HexAnalyzer
echo ========================================================

REM Step 1: Compile Launcher with CSC
echo [1/2] Compiling HexAnalyzer.exe with embedded icon...
"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /nologo /target:winexe /win32icon:app.ico /out:HexAnalyzer.exe HexAnalyzerLauncher.cs

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to compile HexAnalyzer.exe
    exit /b %ERRORLEVEL%
)

echo [2/2] HexAnalyzer.exe built successfully!
echo.
echo You can now double-click HexAnalyzer.exe to launch the application.
pause
