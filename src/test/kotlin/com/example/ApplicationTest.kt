package com.example

import com.example.db.DAOFacadeImpl
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.testcontainers.shaded.org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

class ApplicationTest : AbstractTest() {

    private val N = 5

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
    fun testMessageRoundtrip() {
        withApplication(testEnv()) {
            val dao = DAOFacadeImpl()
            assertThat(runBlocking { dao.getMessages() }).isEmpty();

            handleWebSocketConversation("/chat") { incoming, outgoing ->
                val ignoreWelcome = incoming.receive()
                repeat(N) {
                    outgoing.send(Frame.Text("Testcontainers $it"))
                    incoming.receive() // why???
                }
            }

            Awaitility.await("all messages were saved to the database")
                .atMost(2, TimeUnit.SECONDS)
                .until {
                    runBlocking {
                        val messages = dao.getMessages()
                        messages.size == N
                    }
                }

            handleWebSocketConversation("/chat") { incoming, outgoing ->
                val ignoreWelcome = incoming.receive()
                repeat(N) {
                    val responseText = (incoming.receive() as Frame.Text).readText()
                    assertThat(responseText).contains("Testcontainers")

                }
            }
        }
    }


}