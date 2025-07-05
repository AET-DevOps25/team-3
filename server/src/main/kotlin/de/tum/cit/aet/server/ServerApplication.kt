package de.tum.cit.aet.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class ServerApplication

fun main(args: Array<String>) {
	runApplication<ServerApplication>(*args)
}
