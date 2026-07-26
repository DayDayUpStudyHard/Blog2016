@echo off
title Blog2026 Launcher

echo ==============================
echo   Blog2026 Start
echo ==============================
echo.

cd /d "%~dp0"

echo [1/7] Starting backend (Spring Boot :8080) ...
start "Blog2026-Server" cmd /k "title Blog2026-Server && cd /d %~dp0blog-server && mvnw.cmd spring-boot:run"

echo [2/7] Starting admin (Vite :5173) ...
start "Blog2026-Admin" cmd /k "title Blog2026-Admin && cd /d %~dp0blog-admin && npm run dev"

echo [3/7] Starting front (Vite :5174) ...
start "Blog2026-Front" cmd /k "title Blog2026-Front && cd /d %~dp0blog-front && npm run dev"

echo [4/7] Starting travel backend (FastAPI :8001) ...
start "Travel-Backend" cmd /k "title Travel-Backend && cd /d %~dp0tools\travel-assistant\backend && pip install -r requirements.txt -q && python run.py"

echo [5/7] Starting travel frontend (Vite :5175) ...
start "Travel-Frontend" cmd /k "title Travel-Frontend && cd /d %~dp0tools\travel-assistant\frontend && npm install && npm run dev"

echo [6/7] Starting crypto toolbox (Vite :5176) ...
start "Crypto-Toolbox" cmd /k "title Crypto-Toolbox && cd /d %~dp0tools\crypto-toolbox\frontend && npm install && npm run dev"

echo [7/7] Starting chat assistant (FastAPI :8088) ...
start "Chat-Assistant" cmd /k "title Chat-Assistant && cd /d %~dp0tools\chat-assistant\backend && if exist .venv-ocr\Scripts\python.exe (.venv-ocr\Scripts\python.exe run.py) else (pip install -r requirements.txt -q && python run.py)"

echo.
echo ==============================
echo   All services launched:
echo     Backend:           http://localhost:8080
echo     Admin:             http://localhost:5173
echo     Front:             http://localhost:5174
echo     Travel Backend:    http://localhost:8001
echo     Travel Frontend:   http://localhost:5175
echo     Crypto Toolbox:    http://localhost:5176
echo     Chat Assistant:    http://localhost:8088
echo ==============================
echo.
echo Close each window to stop the service.
pause
