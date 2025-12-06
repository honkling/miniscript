package me.honkling.miniscript.parser.ast.stack

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.pass.Pass

class Environment(val miniScript: MiniScript) : Node<Nothing?>(null, null) {
    val executionStack = mutableListOf(Frame())
    val lastFrame get() = executionStack.last()

    lateinit var stringClass: Class; private set

    fun resolveReferences() {
        val symbolTable = executionStack[0].symbolTable
        stringClass = symbolTable["String"] as Class
    }

    override fun accept(pass: Pass) {
        pass.visitEnvironment(this)
    }
}