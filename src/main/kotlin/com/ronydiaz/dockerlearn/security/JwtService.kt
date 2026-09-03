package com.ronydiaz.dockerlearn.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.ronydiaz.dockerlearn.models.User
import java.util.Date

object JwtService {
    val secret: String = System.getenv("JWT_SECRET") ?: "super-secret-key-rony-diaz-falabella-2026"
    const val ISSUER: String = "dockerlearn-backend"
    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    // Token válido por 24 horas
    private const val VALIDITY_IN_MS: Long = 24 * 60 * 60 * 1000L

    fun generateToken(user: User): String {
        val now = System.currentTimeMillis()
        val expiresAt = Date(now + VALIDITY_IN_MS)

        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(user.id)
            .withClaim("email", user.email)
            .withClaim("name", user.name)
            .withClaim("role", user.role)
            .withIssuedAt(Date(now))
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }

    fun verifyToken(token: String): DecodedJWT? {
        return runCatching {
            val verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
            verifier.verify(token)
        }.getOrNull()
    }
}
