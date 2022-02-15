package com.example

import com.example.db.DAOFacadeImpl
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer

class ApplicationTest {


    val container = PostgreSQLContainer("postgres:14-alpine")
        .withUsername("postgres")
        .withPassword("postgres")
        .withDatabaseName("ktorjournal")

    private val testEnv: ApplicationEngineEnvironment = createTestEnvironment {
        container.start()

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
    fun testSomething() {
        withApplication(testEnv) {

            val dao = DAOFacadeImpl()

                handleWebSocketConversation("/chat") { incoming, outgoing ->
                    println("***********************")
                    val ignore = incoming.receive() //ignore the welcome message

                    outgoing.send(Frame.Text("JetBrains"))

                    val responseText = (incoming.receive() as Frame.Text).readText()
                    println(responseText)
                    println("***********************")

                    val messages = dao.getMessages()
                    println("********************: ${messages.size}")
                    messages.forEach {
                        println(it)
                    }
                    println("***********************")
                }


        }

    }


//    @Test
//    fun testRoot() {
//        withTestApplication {
//            handleRequest(HttpMethod.Get, "/chat").apply {
//                assertEquals(HttpStatusCode.OK, response.status())
//                assertEquals("Hello World!", response.content)
//            }
//        }
//    }
}