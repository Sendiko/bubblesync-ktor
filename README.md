# bubblesync

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:
 * [Ktor Documentation](https://ktor.io/docs/home.html)
 * [Ktor GitHub page](https://github.com/ktorio/ktor)
 * [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). [Request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).


## Features
Here's a list of features included in this project:

| Name | Description |
|------|-------------|
| [kotlinx.serialization](https://start.ktor.io/p/io.ktor/server-kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library |
| [Content Negotiation](https://start.ktor.io/p/io.ktor/server-content-negotiation) | Provides automatic content conversion according to Content-Type and Accept headers |
| [Exposed](https://start.ktor.io/p/org.jetbrains/server-exposed) | Adds Exposed database to your application |
| [Koin](https://start.ktor.io/p/io.insert-koin/server-koin) | Provides dependency injection |
| [Static Content](https://start.ktor.io/p/io.ktor/server-static-content) | Serves static files from defined locations |
| [CSRF](https://start.ktor.io/p/io.ktor/server-csrf) | Cross-site request forgery mitigation |
| [Authentication](https://start.ktor.io/p/io.ktor/server-auth) | Provides extension point for handling the Authorization header |
| [Authentication JWT](https://start.ktor.io/p/io.ktor/server-auth-jwt) | Handles JSON Web Token (JWT) bearer authentication scheme |
| [Swagger](https://start.ktor.io/p/io.ktor/server-swagger) | Serves Swagger UI for your project |
| [CORS](https://start.ktor.io/p/io.ktor/server-cors) | Enables Cross-Origin Resource Sharing (CORS) |

## API Documentation
The OpenAPI 3 specification is in [`src/main/resources/documentation.yaml`](src/main/resources/documentation.yaml). The server exposes the Swagger UI route at `/openapi`.

All API routes use the `/api` prefix. Register with `POST /api/auth/register` using `email`, `password`, and `name`; passwords must be 8-128 characters. Log in with `POST /api/auth/login`. Both return an access token and public user profile. Send the token as `Authorization: Bearer <accessToken>` to use protected routes. `GET /api/auth/me` remains available for compatibility; new clients can use `/api/users/me`.

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| `POST` | `/api/auth/register` | Create an account | No |
| `POST` | `/api/auth/login` | Issue an access token | No |
| `GET` | `/api/auth/me` | Read token profile (compatibility route) | Yes |
| `GET` | `/api/users/me` | Read current user profile | Yes |
| `PATCH` | `/api/users/me` | Update current user's name | Yes |
| `GET` | `/api/places` | List laundry places | Yes |
| `POST` | `/api/places` | Create a laundry place | Yes |
| `GET` | `/api/places/{id}` | Read a laundry place | Yes |
| `PUT` | `/api/places/{id}` | Replace a laundry place | Yes |
| `DELETE` | `/api/places/{id}` | Delete a laundry place | Yes |
| `GET` | `/api/records` | List current user's records | Yes |
| `POST` | `/api/records` | Create a record and its items | Yes |
| `GET` | `/api/records/{id}` | Read a current user's record | Yes |
| `PATCH` | `/api/records/{id}/status` | Change record status | Yes |
| `DELETE` | `/api/records/{id}` | Delete a record and its items | Yes |

### Laundry Records
Record endpoints only return or modify records belonging to the authenticated user. New records start with status `DROPPED_OFF`; valid statuses are `DROPPED_OFF`, `WASHING`, `READY`, and `PICKED_UP`. Record creation requires an existing `placeId`. `dropOffDateTime` is an ISO-8601 local date-time without a time-zone offset. `amountPaid` is a decimal string with at most two fractional digits, such as `"25.50"`, to preserve monetary precision. Item quantities must be between 1 and 1000, with up to 100 items per record.

Example request for `POST /api/records`:

```json
{
	"placeId": "8fe138c7-41e2-4286-a9c6-40c119e31a5d",
	"dropOffDateTime": "2026-10-30T12:15:00",
	"amountPaid": "25.50",
	"bulkPhotoUrl": null,
	"items": [
		{ "name": "Shirt", "quantity": 2 },
		{ "name": "Towel" }
	]
}
```

Deleting a record also deletes its items. A laundry place referenced by records cannot be deleted until those records are removed.

### Security Configuration
Passwords are stored as salted PBKDF2 hashes. Set `JWT_SECRET` to a random secret of at least 32 characters outside local development; the value in `application.yaml` is only a development fallback. `JWT_ISSUER` and `JWT_AUDIENCE` can also be overridden through environment variables.

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
```

## Building & Running
For a bare-metal deployment with MySQL already running on the host, do not start the MySQL service from `docker compose`. Configure the application to use the existing database, make sure the `bubblesync` user has access to the `bubblesync` database, then run:

```bash
cp .env.example .env
# Edit .env and set MYSQL_PASSWORD, JWT_SECRET, and MinIO credentials.
set -a
. ./.env
set +a

./gradlew build
./gradlew runShadow
```

The API listens on port `8080`. `MYSQL_HOST=localhost` is correct when the API runs directly on the Linux host. If the API later runs inside Docker, use the database service hostname instead, such as `mysql`.

The included `docker-compose.yml` starts a separate MySQL container for local development. Do not run `docker compose up -d` alongside a host MySQL instance using the same port unless you stop one of them or change the published port.

To build or run the project, use one of the following tasks:


| Task | Description |
|------|-------------|
| `./gradlew test`    | Run the tests     |
| `./gradlew build`   | Build the project |
| `./gradlew run`     | Run the server    |

If the server starts successfully, you'll see the following output:
```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
