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

## Authentication
Register with `POST /api/auth/register` using a JSON body containing `email`, `password`, and `name`. Passwords must be 8-128 characters. Login with `POST /api/auth/login` using `email` and `password`; both endpoints return a Bearer access token and public user profile. Send the token in the `Authorization: Bearer <token>` header to access `GET /api/auth/me`.

Passwords are stored as salted PBKDF2 hashes. Set `JWT_SECRET` to a random secret of at least 32 characters outside local development; the value in `application.yaml` is only a development fallback. `JWT_ISSUER` and `JWT_AUDIENCE` can also be overridden through environment variables.

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
```

## Building & Running
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
