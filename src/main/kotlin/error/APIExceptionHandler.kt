package com.es.error

import com.es.error.exception.BadRequestException
import com.es.error.exception.ForbiddenException
import com.es.error.exception.NotAuthorizedException
import com.es.error.exception.NotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.NumberFormatException

@ControllerAdvice
class APIExceptionHandler {

    @ExceptionHandler(
        IllegalArgumentException::class,
        NumberFormatException::class,
        BadRequestException::class
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleBadRequest(request: HttpServletRequest, e: Exception): ErrorRespuesta {
        return ErrorRespuesta(e.message ?: "Bad Request", request.requestURI)
    }

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    fun handleForbidden(request: HttpServletRequest, e: Exception): ErrorRespuesta {
        return ErrorRespuesta(e.message ?: "Forbidden", request.requestURI)
    }

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    fun handleNotFound(request: HttpServletRequest, e: Exception): ErrorRespuesta {
        return ErrorRespuesta(e.message ?: "Not Found", request.requestURI)
    }

    @ExceptionHandler(NotAuthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    fun handleNotAuthorized(request: HttpServletRequest, e: Exception): ErrorRespuesta {
        return ErrorRespuesta(e.message ?: "Not Authorized", request.requestURI)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleGeneric(request: HttpServletRequest, e: Exception): ErrorRespuesta {
        return ErrorRespuesta(e.message ?: "Internal Server Error", request.requestURI)
    }
}