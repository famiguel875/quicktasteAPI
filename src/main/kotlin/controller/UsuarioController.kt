package com.es.controller

import com.es.dto.LoginUsuarioDTO
import com.es.dto.UsuarioDTO
import com.es.dto.UsuarioRegisterDTO
import com.es.error.exception.BadRequestException
import com.es.error.exception.ForbiddenException
import com.es.error.exception.NotAuthorizedException
import com.es.service.TokenService
import com.es.service.UsuarioService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/usuarios")
class UsuarioController {

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var tokenService: TokenService

    @Autowired
    private lateinit var usuarioService: UsuarioService

    // ----------------------------------------
    // User registration
    // ----------------------------------------
    @PostMapping("/register")
    fun register(
        @RequestBody dto: UsuarioRegisterDTO
    ): ResponseEntity<UsuarioDTO> {
        val usuarioDTO = usuarioService.register(dto)
        return ResponseEntity(usuarioDTO, HttpStatus.CREATED)
    }

    // ----------------------------------------
    // User login → returns JWT
    // ----------------------------------------
    @PostMapping("/login")
    fun login(@RequestBody credentials: LoginUsuarioDTO): ResponseEntity<Any> {
        val auth: Authentication = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    credentials.username,
                    credentials.password
                )
            )
        } catch (e: AuthenticationException) {
            throw ForbiddenException("Invalid credentials")
        }

        // Generate the token
        val token = tokenService.generarToken(auth)
        // Retrieve the DTO to return to the client
        val userDto = usuarioService.findByUsername(credentials.username)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "token" to token,
                "user"  to userDto
            )
        )
    }

    // ----------------------------------------
    // Get the profile of the authenticated user
    // ----------------------------------------
    @GetMapping("/me")
    fun me(authentication: Authentication): ResponseEntity<UsuarioDTO> {
        // authentication.name is the username from the JWT
        val username = authentication.name
        val dto = usuarioService.findByUsername(username)
        return ResponseEntity.ok(dto)
    }

    /**
     * PUT /usuarios/me/wallet
     * Updates only the wallet of the authenticated user.
     */
    @PutMapping("/me/wallet")
    fun updateMyWallet(
        authentication: Authentication,
        @RequestBody body: Map<String, Int>
    ): ResponseEntity<UsuarioDTO> {
        val newWallet = body["wallet"]
            ?: throw BadRequestException("You must provide the new wallet amount under 'wallet'")
        val username = authentication.name
        val updated = usuarioService.updateWallet(username, newWallet)
        return ResponseEntity.ok(updated)
    }

    // ----------------------------------------
    // Permission helpers
    // ----------------------------------------
    private fun isAdmin(auth: Authentication): Boolean =
        auth.authorities.any { it.authority == "ROLE_ADMIN" }

    private fun isOwnerOrAdmin(auth: Authentication, username: String): Boolean =
        auth.name == username || isAdmin(auth)

    // ----------------------------------------
    // Get all users (only ADMIN)
    // ----------------------------------------
    @GetMapping
    fun getAll(authentication: Authentication): ResponseEntity<List<UsuarioDTO>> {
        if (!isAdmin(authentication)) {
            throw ForbiddenException("Access denied to view all users.")
        }
        val list = usuarioService.findAll()
        return ResponseEntity.ok(list)
    }

    // ----------------------------------------
    // Get a user by username (ADMIN or owner)
    // ----------------------------------------
    @GetMapping("/{username}")
    fun getByUsername(
        authentication: Authentication,
        @PathVariable username: String
    ): ResponseEntity<UsuarioDTO> {
        if (!isOwnerOrAdmin(authentication, username)) {
            throw ForbiddenException("Access denied to view this user.")
        }
        val dto = usuarioService.findByUsername(username)
        return ResponseEntity.ok(dto)
    }

    // ----------------------------------------
    // Update a user
    // ----------------------------------------
    @PutMapping("/{username}")
    fun update(
        authentication: Authentication,
        @PathVariable username: String,
        @RequestBody dto: UsuarioRegisterDTO
    ): ResponseEntity<UsuarioDTO> {
        if (!isOwnerOrAdmin(authentication, username)) {
            throw ForbiddenException("Access denied to update this user.")
        }
        val updated = usuarioService.update(username, dto)
        return ResponseEntity.ok(updated)
    }

    // ----------------------------------------
    // Delete a user
    // ----------------------------------------
    @DeleteMapping("/{username}")
    fun delete(
        authentication: Authentication,
        @PathVariable username: String
    ): ResponseEntity<Void> {
        // Allow deletion by ADMIN or by the user themselves
        if (!(isOwnerOrAdmin(authentication, username))) {
            throw ForbiddenException("Access denied to delete this user.")
        }
        usuarioService.delete(username)
        return ResponseEntity.noContent().build()
    }

    /**
     * PUT /usuarios/{username}/wallet
     * Updates only the wallet of the specified user.
     */
    @PutMapping("/{username}/wallet")
    fun updateWallet(
        @PathVariable username: String,
        @RequestBody body: Map<String, Int>
    ): ResponseEntity<UsuarioDTO> {
        val newWallet = body["wallet"]
            ?: throw BadRequestException("You must provide the new wallet amount under 'wallet'")
        val updated = usuarioService.updateWallet(username, newWallet)
        return ResponseEntity.ok(updated)
    }
}