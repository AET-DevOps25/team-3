package de.tum.cit.aet.document

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class DocumentServiceApplication

fun main(args: Array<String>) {
    runApplication<DocumentServiceApplication>(*args)
} 