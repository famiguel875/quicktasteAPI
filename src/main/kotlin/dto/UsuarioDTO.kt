package com.es.dto

data class UsuarioDTO(
    val email: String,
    val username: String,
    val password: String,
    var roles: String? = null,
    val image: String? = null,
    var wallet: Int = 0
)
