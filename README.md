# SkillSync Backend

REST API backend for the SkillSync Academic Mentorship & Code Review Platform.

## Tech Stack

- **Java 21** + **Spring Boot 3.5**
- **Spring Security** + **JWT** authentication
- **Spring Data JPA** + **PostgreSQL**
- **Lombok** + **Validation**
- **SpringDoc OpenAPI** (Swagger UI)
- **Docker** + **Docker Compose**

## Features

- Role-based access control (Admin / Mentor / Student)
- JWT token authentication
- Mentor availability & 45-minute slot booking
- Concurrency-safe session booking with transaction isolation
- AI-powered session audit log generation
- Swagger UI at `/swagger-ui.html`

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL
- Maven

### Run Locally

```bash
# Start PostgreSQL and create the database
sudo systemctl start postgresql
sudo -u postgres psql -c "CREATE DATABASE skillsync;"
sudo -u postgres psql -c "CREATE USER skillsync_user WITH PASSWORD 'skillsync123';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE skillsync TO skillsync_user;"

# Run the app
mvn spring-boot:run
```

### Run with Docker

```bash
docker-compose up --build
```

## API Documentation

Once running, visit:

```
http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `APP_JWT_SECRET` | JWT signing secret |
| `APP_JWT_EXPIRATION` | Token expiry in milliseconds |

## Project Structure

```
src/main/java/com/pentastack/skillsync/
├── auth/
├── mentor/
├── student/
├── stack/
├── session/
└── shared/
    ├── security/
    └── exception/
```
