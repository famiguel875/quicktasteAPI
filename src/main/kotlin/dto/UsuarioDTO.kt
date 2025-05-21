package com.es.dto

data class UsuarioDTO(
    val email: String,
    val username: String,
    val password: String,
    val rol: String,
    val image: String?
)
