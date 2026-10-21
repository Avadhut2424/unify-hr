@echo off
echo ===================================================
echo   UnifyHR Enterprise Platform - Web Portal
echo ===================================================
echo Opening web portal at http://localhost:3000 ...
start http://localhost:3000
python -m http.server 3000 --directory frontend
