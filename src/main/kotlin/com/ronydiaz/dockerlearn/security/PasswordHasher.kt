package com.ronydiaz.dockerlearn.security

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {
    private const val COST = 12

    fun hash(password: String): String {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray())
    }

    fun verify(password: String, hash: String): Boolean {
        return runCatching {
            BCrypt.verifyer().verify(password.toCharArray(), hash.toCharArray()).verified
        }.getOrDefault(false)
    }
}
