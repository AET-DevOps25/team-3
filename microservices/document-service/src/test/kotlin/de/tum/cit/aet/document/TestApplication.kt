package de.tum.cit.aet.document

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient

@TestConfiguration
class TestConfig {
    
    @Bean
    @Primary
    fun mockWebClient(): WebClient {
        return WebClient.builder().build()
    }
}

@SpringBootApplication
class TestApplication