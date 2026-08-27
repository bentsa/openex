package com.openex.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpenExCoreApplication

fun main(args: Array<String>) {
    runApplication<OpenExCoreApplication>(*args)
}
