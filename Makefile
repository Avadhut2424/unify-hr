.PHONY: up down logs test clean

up:
	docker compose up -d --build

down:
	docker compose down -v

logs:
	docker compose logs -f

test:
	powershell -ExecutionPolicy Bypass -File .\verify_rbac.ps1

clean: down
