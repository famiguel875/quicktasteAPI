package com.es.dto

data class UsuarioRegisterDTO(
    val username: String,
    val email: String,
    val password: String,
    val passwordRepeat: String,
    val rol: String? = "USER",
    val image: String?
)
