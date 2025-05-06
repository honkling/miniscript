package me.honkling.miniscript.pass

import me.honkling.miniscript.parser.ast.Node

class PassManager {
    private val passes = mutableListOf<Pass>()

    fun register(pass: Pass) {
        passes += pass
    }

    fun deregister(pass: Pass) {
        passes -= pass
    }

    fun accept(node: Node<*>) {
        for (pass in passes)
            pass.visit(node)
    }
}