# 99 Backend Tech Challenge

This is my solution to the 99 backend challenge. It is a small microservices system written in Java 17 with Spring Boot 3.1.5, made of three separate applications.

## Services

listing-service runs on port 8081. It is the service provided with the challenge and it stores the listings.

Alfaz-user-service runs on port 8082. It stores the users. The data is kept in memory, so it is cleared whenever the service restarts.

Alfaz-public-api runs on port 8080. This is the API for outside clients. It has no database of its own and only talks to the other two services through their REST APIs.

## Requirements

JDK 17

## How to run

Open three terminals and start each service from its own folder. Start the listing service and the user service before using the public API.

On Windows (PowerShell):

```
cd Alfaz-user-service
.\gradlew bootrun
```

Do the same for listing-service and Alfaz-public-api. On macOS or Linux, use ./gradlew instead of .\gradlew.

## Public API

Create a user. The request body is JSON:

```
POST /public-api/users
{"name": "Alfaz"}
```

Create a listing. The request body is JSON:

```
POST /public-api/listings
{"userId": 1, "listingType": "rent", "price": 6000}
```

Get listings:

```
GET /public-api/listings?pageNum=1&pageSize=10&userId=1
```

pageNum defaults to 1, pageSize defaults to 10, and userId is optional. Each listing in the response includes the user who created it.

The public API accepts JSON, then forwards the data to the user and listing services as form data, which is what those services expect.

## Example

Create a user first, so that userId 1 exists.

PowerShell:

```
Invoke-RestMethod -Method Post -Uri http://localhost:8080/public-api/users -ContentType "application/json" -Body '{"name":"Alfaz"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/public-api/listings -ContentType "application/json" -Body '{"userId":1,"listingType":"rent","price":6000}'
Invoke-RestMethod -Uri "http://localhost:8080/public-api/listings?pageNum=1&pageSize=10"
```

curl:

```
curl -X POST localhost:8080/public-api/users -H "Content-Type: application/json" -d '{"name":"Alfaz"}'
curl -X POST localhost:8080/public-api/listings -H "Content-Type: application/json" -d '{"userId":1,"listingType":"rent","price":6000}'
curl "localhost:8080/public-api/listings?pageNum=1&pageSize=10"
```

## Notes

createdAt and updatedAt are in microseconds.

The public API checks that the user exists before creating a listing, and it returns an error if the listing type is not rent or sale or if the price is not above zero.

If one of the other services is down, the public API responds with HTTP 503.
