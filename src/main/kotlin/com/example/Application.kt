package com.example

import com.example.db.DAOFacadeImpl
import com.example.db.initDatabase
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.network.sockets.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*

import kotlin.collections.LinkedHashSet

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)


fun Application.module(){

    install(WebSockets)

    initDatabase(this.environment.config)

    val dao = DAOFacadeImpl()

    routing {

        val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())
        webSocket("/chat") {
            val c = Connection(this)
            connections += c

            c.session.send("Welcome to the chat! There are ${connections.size} users online.")

            dao.getMessages().forEach {
                c.session.send(it)
            }

            for (frame in incoming) {
                frame as? Frame.Text ?: continue

                val text = frame.readText()

                val message = "${c.name}: $text"

                dao.saveMessage(message)

                connections.forEach{
                    it.session.send(message)
                }
            }
        }
    }
}




















//region cheat
fun Application.module2() {

    initDatabase(environment.config)

    val dao = DAOFacadeImpl()

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())

        webSocket("/chat") {
            val c = Connection(this)
            connections += c

            try {
                c.session.send("Welcome to the chat. ${connections.size} users are online")
                dao.getMessages().forEach {
                    c.session.send(it)
                }

                for (frame in incoming) {

                    frame as? Frame.Text ?: continue
                    val text = frame.readText()

                    val message = "${c.name} $text"

                    dao.saveMessage(message)

                    connections.forEach {
                        it.session.send(message)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                println("${c.name} disconnected")
                connections -= c
            }

        }
    }

}
//endregion

