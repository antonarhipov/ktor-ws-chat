package com.example

import org.testcontainers.containers.PostgreSQLContainer

abstract class AbstractTest {

    companion object {
        @JvmStatic
        val container = PostgreSQLContainer("postgres:14-alpine")
            .withUsername("postgres")
            .withPassword("postgres")
            .withDatabaseName("ktorjournal").apply {
                start()
            }
    }


}