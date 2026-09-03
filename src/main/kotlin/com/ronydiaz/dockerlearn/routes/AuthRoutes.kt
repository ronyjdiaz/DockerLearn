package com.ronydiaz.dockerlearn.routes

import com.ronydiaz.dockerlearn.models.AuthResponse
import com.ronydiaz.dockerlearn.models.LoginRequest
import com.ronydiaz.dockerlearn.models.RegisterRequest
import com.ronydiaz.dockerlearn.repository.UserRepository
import com.ronydiaz.dockerlearn.security.JwtService
import com.ronydiaz.dockerlearn.security.PasswordHasher
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json

private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

fun Route.authRouting(userRepository: UserRepository) {
    route("/auth") {
        post("/register") {
            try {
                val bodyText = call.receiveText()
                val request = jsonParser.decodeFromString<RegisterRequest>(bodyText)

                if (!request.email.contains("@") || !request.email.contains(".")) {
                    return@post call.respondText(
                        "El formato del correo no es válido",
                        status = HttpStatusCode.BadRequest
                    )
                }
                if (request.password.length < 6) {
                    return@post call.respondText(
                        "La contraseña debe tener al menos 6 caracteres",
                        status = HttpStatusCode.BadRequest
                    )
                }

                val existing = userRepository.findByEmail(request.email)
                if (existing != null) {
                    return@post call.respondText(
                        "Ya existe una cuenta con este correo electrónico",
                        status = HttpStatusCode.Conflict
                    )
                }

                val passwordHash = PasswordHasher.hash(request.password)
                val user = userRepository.createUser(request, passwordHash)
                val token = JwtService.generateToken(user)

                call.respond(HttpStatusCode.Created, AuthResponse(token, user))
            } catch (e: Exception) {
                call.respondText(
                    "Error en registro: ${e.message}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        post("/login") {
            try {
                val bodyText = call.receiveText()
                val request = jsonParser.decodeFromString<LoginRequest>(bodyText)

                val record = userRepository.findByEmail(request.email)
                if (record == null) {
                    return@post call.respondText(
                        "Credenciales inválidas",
                        status = HttpStatusCode.Unauthorized
                    )
                }

                val isPasswordValid = PasswordHasher.verify(request.password, record.passwordHash)
                if (!isPasswordValid) {
                    return@post call.respondText(
                        "Credenciales inválidas",
                        status = HttpStatusCode.Unauthorized
                    )
                }

                val token = JwtService.generateToken(record.user)
                call.respond(HttpStatusCode.OK, AuthResponse(token, record.user))
            } catch (e: Exception) {
                call.respondText(
                    "Error en login: ${e.message}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }
    }
}
