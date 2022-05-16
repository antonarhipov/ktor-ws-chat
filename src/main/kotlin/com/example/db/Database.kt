package com.example.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

interface DAOFacade {
    suspend fun saveMessage(body: String)

    suspend fun getMessages(): List<String>
}

class DAOFacadeImpl : DAOFacade {
    override suspend fun saveMessage(body: String): Unit = dbQuery {
        Messages.insert { it[Messages.text] = body }
    }

    override suspend fun getMessages(): List<String> = dbQuery {
        Messages.selectAll().map { it[Messages.text] }
    }
}


fun initDatabase(config: ApplicationConfig) {
    val driverClassName = config.property("storage.driverClassName").getString()
    val jdbcURL = config.property("storage.jdbcURL").getString() +
            // used to provide an absolute path for H2 on-disk option:
            (config.propertyOrNull("storage.dbFilePath")?.getString()?.let {
                File(it).canonicalFile.absolutePath
            } ?: "")

    Database.connect(createHikariDataSource(
        url = jdbcURL,
        driver = driverClassName,
        user = "postgres",
        pwd = "postgres"
    ))
//    val db2 = Database.connect(createHikariDataSource(url = jdbcURL, driver = driverClassName))

    transaction {
        SchemaUtils.create(Messages)
    }
}

private fun createHikariDataSource(
    url: String,
    driver: String,
    user: String,
    pwd: String
) = HikariDataSource(HikariConfig().apply {
    driverClassName = driver
    jdbcUrl = url
    maximumPoolSize = 3
    isAutoCommit = false
    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    username = user
    password = pwd
    validate()
})

object Messages : Table() {
    val id = integer("id").autoIncrement()
    val text = varchar("body", 1024)

    override val primaryKey = PrimaryKey(id)
}

private suspend fun <T> dbQuery(block: () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }