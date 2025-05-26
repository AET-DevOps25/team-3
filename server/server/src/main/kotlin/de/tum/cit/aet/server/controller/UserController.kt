package de.tum.cit.aet.server.controller

import de.tum.cit.aet.server.service.UserService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    // TODO: Implement user endpoints
    // - POST /register
    // - POST /login
    // - GET /profile
    // - PUT /profile
}
