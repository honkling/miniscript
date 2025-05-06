package me.honkling.miniscript

import java.io.File
import java.time.Instant

fun main(args: Array<String>) {
    val name = args.getOrNull(0)
        ?: return println("expected usage: miniscript <resource name>")

    val resource = MiniScript::class.java.getResource("/$name")?.toURI()
        ?: return println("miniscript: cant find resource")

    val file = File(resource)
    val miniScript = miniScript()

    miniScript.evaluate(file)
}