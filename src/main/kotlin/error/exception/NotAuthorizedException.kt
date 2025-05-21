package com.es.error.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class NotAuthorizedException(message: String) : RuntimeException("Not Authorized Exception (401). $message")