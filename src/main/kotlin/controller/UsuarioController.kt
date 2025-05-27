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
    // Registro de usuario
    // ----------------------------------------
    @PostMapping("/register")
    fun register(
        @RequestBody dto: UsuarioRegisterDTO
    ): ResponseEntity<UsuarioDTO> {
        val usuarioDTO = usuarioService.register(dto)
        return ResponseEntity(usuarioDTO, HttpStatus.CREATED)
    }

    // ----------------------------------------
    // Login de usuario → devuelve JWT
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
            throw NotAuthorizedException("Credenciales incorrectas")
        }

        // Generamos el token
        val token = tokenService.generarToken(auth)
        // Obtenemos ya el DTO para devolverlo al cliente
        val userDto = usuarioService.findByUsername(credentials.username)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "token" to token,
                "user"  to userDto
            )
        )
    }
    // ----------------------------------------
    // Obtener el perfil del usuario autenticado
    // ----------------------------------------
    @GetMapping("/me")
    fun me(authentication: Authentication): ResponseEntity<UsuarioDTO> {
        // authentication.name es el username del JWT
        val username = authentication.name
        val dto = usuarioService.findByUsername(username)
        return ResponseEntity.ok(dto)
    }

    /**
     * PUT /usuarios/me/wallet
     * Actualiza únicamente el wallet del usuario autenticado.
     */
    @PutMapping("/me/wallet")
    fun updateMyWallet(
        authentication: Authentication,
        @RequestBody body: Map<String, Int>
    ): ResponseEntity<UsuarioDTO> {
        val newWallet = body["wallet"]
            ?: throw BadRequestException("Debes indicar el nuevo wallet en 'wallet'")
        val username = authentication.name
        val updated = usuarioService.updateWallet(username, newWallet)
        return ResponseEntity.ok(updated)
    }

    // ----------------------------------------
    // Helpers de permiso
    // ----------------------------------------
    private fun isAdmin(auth: Authentication): Boolean =
        auth.authorities.any { it.authority == "ROLE_ADMIN" }

    private fun isOwnerOrAdmin(auth: Authentication, username: String): Boolean =
        auth.name == username || isAdmin(auth)

    // ----------------------------------------
    // Obtener todos los usuarios (solo ADMIN)
    // ----------------------------------------
    @GetMapping
    fun getAll(authentication: Authentication): ResponseEntity<List<UsuarioDTO>> {
        if (!isAdmin(authentication)) {
            throw ForbiddenException("Acceso denegado para ver todos los usuarios.")
        }
        val list = usuarioService.findAll()
        return ResponseEntity.ok(list)
    }

    // ----------------------------------------
    // Obtener un usuario por username (ADMIN o dueño)
    // ----------------------------------------
    @GetMapping("/{username}")
    fun getByUsername(
        authentication: Authentication,
        @PathVariable username: String
    ): ResponseEntity<UsuarioDTO> {
        // autoriza sólo si es ADMIN o el propio usuario
        val isAdmin = authentication.authorities.any { it.authority == "ROLE_ADMIN" }
        if (authentication.name != username && !isAdmin) {
            throw ForbiddenException("Acceso denegado para ver este usuario")
        }
        val dto = usuarioService.findByUsername(username)
        return ResponseEntity.ok(dto)
    }

    // ----------------------------------------
    // Actualizar un usuario
    // ----------------------------------------
    @PutMapping("/{username}")
    fun update(
        authentication: Authentication,
        @PathVariable username: String,
        @RequestBody dto: UsuarioRegisterDTO
    ): ResponseEntity<UsuarioDTO> {
        if (!isOwnerOrAdmin(authentication, username)) {
            throw ForbiddenException("Acceso denegado para actualizar este usuario.")
        }
        val updated = usuarioService.update(username, dto)
        return ResponseEntity.ok(updated)
    }

    // ----------------------------------------
    // Eliminar un usuario
    // ----------------------------------------
    @DeleteMapping("/{username}")
    fun delete(
        authentication: Authentication,
        @PathVariable username: String
    ): ResponseEntity<Void> {
        if (!isAdmin(authentication)) {
            throw ForbiddenException("Acceso denegado para eliminar este usuario.")
        }
        usuarioService.delete(username)
        return ResponseEntity.noContent().build()
    }

    /**
     * PUT /usuarios/{username}/wallet
     * Actualiza únicamente el saldo (wallet) del usuario.
     */
    @PutMapping("/{username}/wallet")
    fun updateWallet(
        @PathVariable username: String,
        @RequestBody body: Map<String, Int>
    ): ResponseEntity<UsuarioDTO> {
        val newWallet = body["wallet"]
            ?: throw BadRequestException("Debe proporcionarse el nuevo saldo bajo la clave 'wallet'")
        val updated = usuarioService.updateWallet(username, newWallet)
        return ResponseEntity.ok(updated)
    }
}




