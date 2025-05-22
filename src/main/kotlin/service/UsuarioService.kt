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
    // Para Spring Security (JWT)
    // ----------------------------------------
    override fun loadUserByUsername(username: String?): UserDetails {
        val userNameNonNull = username
            ?: throw NotFoundException("El username no puede ser nulo")
        val usuario = usuarioRepository.findByUsername(userNameNonNull)
            .orElseThrow { NotFoundException("Usuario '$userNameNonNull' no encontrado") }
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
    // Registro abierto
    // ----------------------------------------
    fun register(dto: UsuarioRegisterDTO): UsuarioDTO {
        if (dto.password != dto.passwordRepeat) {
            throw BadRequestException("Las contraseñas no coinciden")
        }
        if (usuarioRepository.findByUsername(dto.username).isPresent) {
            throw BadRequestException("El username '${dto.username}' ya existe")
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
    // Listar todos (ADMIN)
    // ----------------------------------------
    fun findAll(): List<UsuarioDTO> {
        requireAdmin()
        return usuarioRepository.findAll().map { it.toDTO() }
    }

    // ----------------------------------------
    // Buscar por username (auth’d)
    // ----------------------------------------
    fun findByUsername(username: String): UsuarioDTO {
        val u = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario '$username' no encontrado") }
        return u.toDTO()
    }

    // ----------------------------------------
    // Actualizar usuario (ADMIN o dueño)
    // ----------------------------------------
    fun update(username: String, dto: UsuarioRegisterDTO): UsuarioDTO {
        // solo admin o el mismo username puede actualizar
        val auth = SecurityContextHolder.getContext().authentication
        if (auth.name != username && auth.authorities.none { it.authority == "ROLE_ADMIN" }) {
            throw ForbiddenException("No tienes permiso para actualizar este usuario")
        }
        if (username != dto.username) {
            throw BadRequestException("El username no puede modificarse")
        }
        val existing = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario '$username' no encontrado") }

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
    // Borrar usuario (ADMIN)
    // ----------------------------------------
    fun delete(username: String) {
        requireAdmin()
        if (!usuarioRepository.findByUsername(username).isPresent) {
            throw NotFoundException("Usuario '$username' no encontrado")
        }
        usuarioRepository.deleteByUsername(username)
    }

    // ----------------------------------------
    // Actualizar solo el wallet (ADMIN)
    // ----------------------------------------
    fun updateWallet(username: String, newWallet: Int): UsuarioDTO {
        requireAdmin()
        val u = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario '$username' no encontrado") }
        val updated = u.copy(wallet = newWallet)
        return usuarioRepository.save(updated).toDTO()
    }

    // ----------------------------------------
    // Verificación interna ADMIN
    // ----------------------------------------
    private fun requireAdmin() {
        val auth = SecurityContextHolder.getContext().authentication
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        if (!isAdmin) {
            throw ForbiddenException("Solo administradores pueden realizar esta operación")
        }
    }

    // ----------------------------------------
    // Mapper Entidad → DTO
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







