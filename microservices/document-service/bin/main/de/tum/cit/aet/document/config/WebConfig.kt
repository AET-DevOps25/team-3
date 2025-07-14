package de.tum.cit.aet.document.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val objectMapper: ObjectMapper
) : WebMvcConfigurer {
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // Remove any existing MappingJackson2HttpMessageConverter
        converters.removeIf { it is MappingJackson2HttpMessageConverter }
        
        // Add our custom MappingJackson2HttpMessageConverter at the beginning of the list
        val jsonConverter = MappingJackson2HttpMessageConverter(objectMapper).apply {
            supportedMediaTypes = listOf(
                org.springframework.http.MediaType.APPLICATION_JSON,
            )
        }
        converters.add(0, jsonConverter)
    }

    @Bean
    fun validator(): LocalValidatorFactoryBean {
        return LocalValidatorFactoryBean()
    }
} 