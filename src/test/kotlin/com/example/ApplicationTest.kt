package com.example

import com.example.db.DAOFacadeImpl
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.TimeUnit


open class BaseTest {
    companion object {
        @JvmStatic
        val container = PostgreSQLContainer("postgres:14-alpine")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("ktorjournal").also {
                it.start()
            }
    }
}

class ApplicationTest : BaseTest() {

    fun testEnv() = createTestEnvironment {

        val my: Config = ConfigFactory.parseMap(
            mapOf(
                "storage.jdbcURL" to container.jdbcUrl,
                "storage.driverClassName" to container.driverClassName
            )
        )
        val loaded: Config = ConfigFactory.load("application.conf")
        val merged = my.withFallback(loaded).resolve()

        config = HoconApplicationConfig(merged)
    }

    @Test
    fun testRoot() {
        withApplication(testEnv()) {
            val dao = DAOFacadeImpl()
            assertThat(runBlocking { dao.getMessages() }).isEmpty()

            handleWebSocketConversation("/chat") { incoming, outgoing ->
                val ignore = incoming.receive()
                outgoing.send(Frame.Text("Testcontainers"))
                incoming.receive() // todo
            }

            Awaitility.await().atMost(2, TimeUnit.SECONDS).until {
                runBlocking {
                    dao.getMessages().isNotEmpty()
                }
            }

            handleWebSocketConversation("/chat") { incoming, outgoing ->
                val ignore = incoming.receive()
                val message = (incoming.receive() as Frame.Text).readText()
                assertThat(message).contains("Testcontainers")
            }

        }
    }
}