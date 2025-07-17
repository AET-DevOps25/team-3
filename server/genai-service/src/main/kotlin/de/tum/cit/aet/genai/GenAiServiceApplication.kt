package de.tum.cit.aet.genai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class GenAiServiceApplication

fun main(args: Array<String>) {
    runApplication<GenAiServiceApplication>(*args)
} 