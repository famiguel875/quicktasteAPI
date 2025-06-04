package com.es.service

import com.es.dto.UsuarioDTO
import com.es.dto.UsuarioRegisterDTO
import com.es.error.exception.BadRequestException
import com.es.error.exception.ForbiddenException
import com.es.error.exception.NotFoundException
import com.es.model.Usuario
import com.es.repository.UsuarioRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UsuarioService(
    @Autowired private val usuarioRepository: UsuarioRepository,
    @Autowired private val passwordEncoder: PasswordEncoder
) : UserDetailsService {

    // ----------------------------------------
    // For Spring Security (JWT)
    // ----------------------------------------
    override fun loadUserByUsername(username: String?): UserDetails {
        val userNameNonNull = username
            ?: throw NotFoundException("Username cannot be null")
        val usuario = usuarioRepository.findByUsername(userNameNonNull)
            .orElseThrow { NotFoundException("User '$userNameNonNull' not found") }
        val rolesArray = usuario.roles
            ?.split(",")
            ?.toTypedArray()
            ?: arrayOf("USER")
        return User.builder()
            .username(usuario.username)
            .password(usuario.password)
            .roles(*rolesArray)
            .build()
    }

    // ----------------------------------------
    // Open registration
    // ----------------------------------------
    fun register(dto: UsuarioRegisterDTO): UsuarioDTO {
        if (dto.password != dto.passwordRepeat) {
            throw BadRequestException("Passwords do not match")
        }
        if (usuarioRepository.findByUsername(dto.username).isPresent) {
            throw BadRequestException("Username '${dto.username}' already exists")
        }
        val entity = Usuario(
            email    = dto.email,
            username = dto.username,
            roles    = dto.rol ?: "USER",
            image    = dto.image,
            password = passwordEncoder.encode(dto.password),
            wallet   = 0
        )
        val saved = usuarioRepository.save(entity)
        return saved.toDTO()
    }

    // ----------------------------------------
    // List all (ADMIN)
    // ----------------------------------------
    fun findAll(): List<UsuarioDTO> {
        requireAdmin()
        return usuarioRepository.findAll().map { it.toDTO() }
    }

    // ----------------------------------------
    // Find by username (authenticated)
    // ----------------------------------------
    fun findByUsername(username: String): UsuarioDTO {
        val u = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("User '$username' not found") }
        return u.toDTO()
    }

    // ----------------------------------------
    // Update user (ADMIN or owner)
    // ----------------------------------------
    fun update(username: String, dto: UsuarioRegisterDTO): UsuarioDTO {
        // only admin or the user themselves can update
        val auth = SecurityContextHolder.getContext().authentication
        if (auth.name != username && auth.authorities.none { it.authority == "ROLE_ADMIN" }) {
            throw ForbiddenException("You do not have permission to update this user")
        }
        if (username != dto.username) {
            throw BadRequestException("Username cannot be modified")
        }
        val existing = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("User '$username' not found") }

        val newPass = if (dto.password.isNotBlank()) {
            passwordEncoder.encode(dto.password)
        } else existing.password

        val updated = existing.copy(
            email    = dto.email,
            username = dto.username,
            roles    = dto.rol ?: existing.roles,
            image    = dto.image,
            password = newPass,
            wallet   = existing.wallet
        )
        return usuarioRepository.save(updated).toDTO()
    }

    // ----------------------------------------
    // Delete user (ADMIN or self)
    // ----------------------------------------
    fun delete(username: String) {
        val auth = SecurityContextHolder.getContext().authentication
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        // Allow if ADMIN or if the user is deleting themselves
        if (auth.name != username && !isAdmin) {
            throw ForbiddenException("You do not have permission to delete this user")
        }
        if (!usuarioRepository.findByUsername(username).isPresent) {
            throw NotFoundException("User '$username' not found")
        }
        usuarioRepository.deleteByUsername(username)
    }

    // ----------------------------------------
    // Update only the wallet (ADMIN or owner)
    // ----------------------------------------
    fun updateWallet(username: String, newWallet: Int): UsuarioDTO {
        val auth = SecurityContextHolder.getContext().authentication
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        if (auth.name != username && !isAdmin) {
            throw ForbiddenException("You do not have permission to modify this wallet")
        }
        val u = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("User '$username' not found") }
        val updated = u.copy(wallet = newWallet)
        return usuarioRepository.save(updated).toDTO()
    }

    // ----------------------------------------
    // Internal ADMIN role verification
    // ----------------------------------------
    private fun requireAdmin() {
        val auth = SecurityContextHolder.getContext().authentication
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        if (!isAdmin) {
            throw ForbiddenException("Only administrators can perform this operation")
        }
    }

    // ----------------------------------------
    // Entity → DTO mapper
    // ----------------------------------------
    private fun Usuario.toDTO(): UsuarioDTO = UsuarioDTO(
        email    = this.email,
        username = this.username,
        password = this.password,
        roles    = this.roles,
        image    = this.image,
        wallet   = this.wallet
    )
}