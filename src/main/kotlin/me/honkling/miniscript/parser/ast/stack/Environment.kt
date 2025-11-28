package me.honkling.miniscript.parser.ast.stack

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.pass.Pass

class Environment(val miniScript: MiniScript) : Node<Nothing?>(null) {
    val executionStack = mutableListOf(Frame())
    val lastFrame get() = executionStack.last()

    override fun accept(pass: Pass) {
        pass.visitEnvironment(this)
    }
}