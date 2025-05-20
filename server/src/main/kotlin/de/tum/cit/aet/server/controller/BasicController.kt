package de.tum.cit.aet.server.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BasicController {

    @GetMapping("/hi")
    fun hi() = "Hello, world!"
}