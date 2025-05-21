package com.es

import com.es.security.RSAKeysProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(RSAKeysProperties::class)
class quicktasteAPIAplication

fun main(args: Array<String>) {
    runApplication<quicktasteAPIAplication>(*args)
}