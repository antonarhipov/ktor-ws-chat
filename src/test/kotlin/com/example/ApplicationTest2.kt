package com.example

import com.example.db.DAOFacadeImpl
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer

class ApplicationTest2 : AbstractTest() {



//    private val testEnv: ApplicationEngineEnvironment
    fun testEnv() = createTestEnvironment {
//        container.start()

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

//    @Before
//    fun before(){
//        container.start()
//    }
//
//    @After
//    fun after(){
//        container.stop()
//    }

    @Test
    fun testSomething() {
        withApplication(testEnv()) {

            val dao = DAOFacadeImpl()

            handleWebSocketConversation("/chat") { incoming, outgoing ->
                println("***********************")
                val ignore = incoming.receive() //ignore the welcome message

                outgoing.send(Frame.Text("Testcontainers"))

                val responseText = (incoming.receive() as Frame.Text).readText()
                println(responseText)
                println("***********************")

                val messages = dao.getMessages()
                println("********************: ${messages.size}")

                assertEquals(1, messages.size)

                messages.forEach {
                    println(it)
                }
                println("***********************")
            }
        }
    }

    @Test
    fun testSomething2() {
        withApplication(testEnv()) {

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

                assertEquals(1, messages.size)

                messages.forEach {
                    println(it)
                }
                println("***********************")
            }
        }

    }

//    @Test
    fun testRoot() {
        withTestApplication(Application::module) {
            handleWebSocketConversation("/chat") { incoming, outgoing ->
                val responseText = (incoming.receive() as Frame.Text).readText()
                println(responseText)
            }
        }
    }
}