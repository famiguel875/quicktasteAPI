package com.es.repository

import com.es.model.Usuario
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UsuarioRepository : MongoRepository<Usuario, String> {
    /**
     * Busca un usuario por su username.
     */
    fun findByUsername(username: String): Optional<Usuario>

    /**
     * Elimina un usuario por su username.
     */
    fun deleteByUsername(username: String)
}
