# 99 Backend Tech Challenge

Microservices system built with Java 17 and Spring Boot 3.1.5. It consists of three independent applications:

| Service | Folder | Port | Description |
| Listing service | `listing-service` | 8081 | Provided. Stores listings (rent / sale). |
| User service | `Alfaz-user-service` | 8082 | Stores users. |
| Public API | `Alfaz-public-api` | 8080 | Public facing API. Calls the listing and user services over REST only. |

## Requirements

- JDK 17
- No database setup needed. The user service keeps its data in memory (data is lost when it restarts).

## How to run

Start the three services in three separate terminals. The listing service and the user service must be running before you call the public API.

Windows (PowerShell):

```powershell
cd listing-service
.\gradlew bootrun
```

```powershell
cd Alfaz-user-service
.\gradlew bootrun
```

```powershell
cd Alfaz-public-api
.\gradlew bootrun
```

macOS / Linux:

```bash
cd listing-service && ./gradlew bootrun
cd Alfaz-user-service && ./gradlew bootrun
cd Alfaz-public-api && ./gradlew bootrun
```

## Public API endpoints (port 8080)

| Method | URL | Body |
|---|---|---|
| POST | `/public-api/users` | JSON: `{"name": "Alfaz"}` |
| POST | `/public-api/listings` | JSON: `{"userId": 1, "listingType": "rent", "price": 6000}` |
| GET | `/public-api/listings?pageNum=1&pageSize=10&userId=1` | `pageNum`, `pageSize`, `userId` are optional |

The public API converts the JSON requests into `application/x-www-form-urlencoded` requests for the user and listing services. `GET /public-api/listings` fetches listings from the listing service, then adds the owner's `user` object (fetched from the user service) to each listing.

## Try it

Create a user first, so that `userId` 1 exists.

Windows (PowerShell):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/public-api/users -ContentType "application/json" -Body '{"name":"Alfaz"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/public-api/listings -ContentType "application/json" -Body '{"userId":1,"listingType":"rent","price":6000}'
Invoke-RestMethod -Uri "http://localhost:8080/public-api/listings?pageNum=1&pageSize=10"
```

macOS / Linux (curl):

```bash
curl -X POST localhost:8080/public-api/users -H "Content-Type: application/json" -d '{"name":"Alfaz"}'
curl -X POST localhost:8080/public-api/listings -H "Content-Type: application/json" -d '{"userId":1,"listingType":"rent","price":6000}'
curl "localhost:8080/public-api/listings?pageNum=1&pageSize=10"
```

## Notes

- `createdAt` and `updatedAt` are in microseconds.
- Validation: `name` is required, `listingType` must be `rent` or `sale`, `price` must be greater than 0, and the user must exist when creating a listing.
- If a downstream service is unavailable, the public API responds with HTTP 503.