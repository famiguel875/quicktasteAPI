package com.es.service

import com.es.dto.UsuarioDTO
import com.es.dto.UsuarioRegisterDTO
import com.es.error.exception.BadRequestException
import com.es.error.exception.NotFoundException
import com.es.model.Usuario
import com.es.repository.UsuarioRepository
import org.springframework.beans.factory.annotation.Autowired
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
    // Para Spring Security (autenticación JWT)
    // ----------------------------------------
    override fun loadUserByUsername(username: String?): UserDetails {
        val usuario = username
            ?.let { usuarioRepository.findByUsername(it) }
            ?.orElseThrow { NotFoundException("Usuario con username '$username' no encontrado") }
            ?: throw NotFoundException("El username no puede ser nulo")

        return User.builder()
            .username(usuario.username)
            .password(usuario.password)
            .roles(*(usuario.roles?.split(",")?.toTypedArray() ?: arrayOf("USER")))
            .build()
    }

    // -------------------
    // Registrar usuario
    // -------------------
    fun register(dto: UsuarioRegisterDTO): UsuarioDTO {
        if (dto.password != dto.passwordRepeat) {
            throw BadRequestException("Las contraseñas no coinciden")
        }
        if (usuarioRepository.findByUsername(dto.username).isPresent) {
            throw BadRequestException("Ya existe un usuario con username '${dto.username}'")
        }
        val entity = dto.toEntity(passwordEncoder)
        val saved = usuarioRepository.save(entity)
        return saved.toDTO()
    }

    // -------------------
    // Listar todos
    // -------------------
    fun findAll(): List<UsuarioDTO> =
        usuarioRepository.findAll().map { it.toDTO() }

    // -------------------
    // Buscar por username
    // -------------------
    fun findByUsername(username: String): UsuarioDTO {
        val u = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario con username '$username' no encontrado") }
        return u.toDTO()
    }

    // -------------------
    // Actualizar usuario (sin tocar wallet)
    // -------------------
    fun update(username: String, dto: UsuarioRegisterDTO): UsuarioDTO {
        if (username != dto.username) {
            throw BadRequestException("El username en la ruta y en el body debe coincidir")
        }
        val existing = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario con username '$username' no encontrado") }

        val newPassword = if (dto.password.isNotBlank()) {
            passwordEncoder.encode(dto.password)
        } else {
            existing.password
        }

        val updated = existing.copy(
            username = dto.username,
            password = newPassword,
            roles    = dto.rol ?: existing.roles,
            image    = existing.image,
            wallet   = existing.wallet
        )
        val saved = usuarioRepository.save(updated)
        return saved.toDTO()
    }

    // -------------------
    // Borrar usuario
    // -------------------
    fun delete(username: String) {
        if (!usuarioRepository.findByUsername(username).isPresent) {
            throw NotFoundException("Usuario con username '$username' no encontrado")
        }
        usuarioRepository.deleteByUsername(username)
    }

    // ------------------------------------
    // Actualizar solo el wallet por username
    // ------------------------------------
    fun updateWallet(username: String, newWallet: Int): UsuarioDTO {
        val u = usuarioRepository.findByUsername(username)
            .orElseThrow { NotFoundException("Usuario con username '$username' no encontrado") }
        u.wallet = newWallet
        val saved = usuarioRepository.save(u)
        return saved.toDTO()
    }

    // ------------------------------------
    // Mappers internos
    // ------------------------------------
    private fun Usuario.toDTO(): UsuarioDTO =
        UsuarioDTO(
            username = this.username,
            email    = this.email,
            password = this.password,
            rol      = this.roles ?: "USER",
            image    = this.image,
            wallet   = this.wallet
        )

    private fun UsuarioRegisterDTO.toEntity(passwordEncoder: PasswordEncoder): Usuario =
        Usuario(
            email    = this.email,
            username = this.username,
            password = passwordEncoder.encode(this.password),
            roles    = this.rol ?: "USER",
            image    = this.image,
            wallet   = 0
        )
}


