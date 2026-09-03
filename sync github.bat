@echo off
title Innova 2KD - Auto Sync to GitHub
color 0B

echo =======================================================
echo    INNOVA 2KD LAUNCHER - GITHUB AUTO-SYNC TOOL
echo    Target: github.com/krisdwiantara12/innova2kd
echo =======================================================
echo.

:: Pindah ke direktori tempat file BAT ini berada
cd /d "%~dp0"

:: Cegah crash Git Credential Manager Laragon lama
git config credential.helper wincred >nul 2>&1

echo [1/3] Menyiapkan berkas yang diperbarui...
git add .

echo.
set /p msg="Masukkan catatan update (atau tekan ENTER untuk otomatis): "
if "%msg%"=="" (
    for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
    set msg=Pembaruan launcher otomatis (%date% %time:~0,5%)
)

echo.
echo [2/3] Menyimpan catatan perubahan (Commit)...
git commit -m "%msg%"

echo.
echo [3/3] Mengunggah ke GitHub (Push ke branch main)...
git -c "credential.helper=" -c "credential.helper=wincred" push origin main

if %ERRORLEVEL% equ 0 (
    echo.
    echo =======================================================
    echo   [SUKSES] Sinkronisasi ke GitHub Berhasil 100%%!
    echo   - Repositori: https://github.com/krisdwiantara12/innova2kd
    echo   - Headunit kini siap mendeteksi update via Hotspot HP!
    echo =======================================================
) else (
    echo.
    echo =======================================================
    echo   [PERHATIAN] Terjadi kendala saat push ke GitHub.
    echo   Pastikan PC terhubung ke internet.
    echo =======================================================
)

echo.
pause