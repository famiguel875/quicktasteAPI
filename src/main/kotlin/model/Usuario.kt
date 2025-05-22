package com.es.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "usuarios")
data class Usuario(
    @Id val email: String,       // PK
    val username: String,
    var roles: String? = null,    // e.g. "USER" | "ADMIN"
    val image: String? = null,    // URL o ruta
    var password: String, // contraseña (hasheada en producción)
    var wallet: Int = 0
)
