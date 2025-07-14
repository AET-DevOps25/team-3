package de.tum.cit.aet.genai.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.beans.factory.annotation.Value
import java.time.Duration

@Configuration
class WebClientConfig {
    
    @Value("\${genai.backend.url}")
    private lateinit var genaiBackendUrl: String
    
    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    @Bean
    fun restTemplate(builder: RestTemplateBuilder, objectMapper: ObjectMapper): RestTemplate {
        val jsonConverter = MappingJackson2HttpMessageConverter(objectMapper)
        
        return builder
            .setConnectTimeout(Duration.ofMinutes(10))
            .setReadTimeout(Duration.ofMinutes(10))
            .additionalMessageConverters(jsonConverter)
            .build()
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .baseUrl(genaiBackendUrl)
            .build()
    }
} 