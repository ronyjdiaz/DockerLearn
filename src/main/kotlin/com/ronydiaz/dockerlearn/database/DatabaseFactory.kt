package com.ronydiaz.dockerlearn.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URI
import java.util.Properties

object DatabaseFactory {
    fun init(databaseUrl: String? = null): Boolean {
        val rawUrl = databaseUrl
            ?: System.getenv("DATABASE_URL")
            ?: getLocalProperty("DATABASE_URL")

        if (rawUrl.isNullOrBlank()) {
            println("⚠️ No se encontró DATABASE_URL. Usando repositorio en memoria...")
            return false
        }

        return try {
            val dataSource = createHikariDataSource(rawUrl)
            Database.connect(dataSource)

            transaction {
                SchemaUtils.create(TasksTable, CategoriesTable)
            }
            println("✅ Conexión exitosa a PostgreSQL en Neon y tablas 'tasks', 'categories' verificadas!")
            true
        } catch (e: Exception) {
            println("❌ Error conectando a PostgreSQL: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun getLocalProperty(key: String): String? {
        val file = File("local.properties")
        if (!file.exists()) return null
        return runCatching {
            val props = Properties()
            file.inputStream().use { props.load(it) }
            props.getProperty(key)
        }.getOrNull()
    }

    private fun createHikariDataSource(rawUrl: String): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"

        if (rawUrl.startsWith("jdbc:postgresql://")) {
            config.jdbcUrl = rawUrl
        } else {
            val uri = URI(rawUrl)
            val userInfo = uri.userInfo ?: ""
            val user = if (userInfo.contains(":")) userInfo.substringBefore(":") else userInfo
            val password = if (userInfo.contains(":")) userInfo.substringAfter(":") else ""
            val port = if (uri.port > 0) uri.port else 5432
            val path = if (!uri.path.isNullOrBlank()) uri.path else "/neondb"
            val query = uri.query?.let { "?$it" } ?: ""

            config.jdbcUrl = "jdbc:postgresql://${uri.host}:$port$path$query"
            config.username = user
            config.password = password
        }

        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
