@echo off
cd /d "F:\BOT\APK HEADUNIT"
git add .
set /p msg="Masukkan pesan commit (atau langsung Enter untuk default): "
if "%msg%"=="" set msg=Update file otomatis
git commit -m "%msg%"
git push
echo Sync ke GitHub Selesai!
pause