package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.pass.Pass
import kotlin.reflect.KClass

abstract class Node<T>(
    val location: Location?,
    var parent: T
) {
    abstract fun accept(pass: Pass)

    fun <T : Any> getParent(klass: KClass<T>): T
        = if (klass == parent!!::class) parent as T else (parent as Node<*>?)!!.getParent(klass)

    fun getBlockParent(): Block?
        = (parent as? Block) ?: (parent as Node<*>?)?.getBlockParent()

    fun getEnvironment(): Environment
        = getBlockParent()?.miniScript?.environment ?: parent as? Environment
            ?: this as Environment

    fun getSymbol(name: String): Any? {
        val environment = getEnvironment()

        for (index in environment.executionStack.size - 1 downTo 0) {
            val frame = environment.executionStack[index]

            if (name in frame.symbolTable)
                return frame.symbolTable[name]
        }

        return null
    }

    fun setSymbol(name: String, value: Any?) {
        val miniScript = if (this is Environment) miniScript else getBlockParent()!!.miniScript
        val executionStack = miniScript.environment.executionStack
        var foundSymbol = false

        for (index in executionStack.size - 1 downTo 0) {
            val frame = executionStack[index]

            if (name in frame.symbolTable) {
                frame.symbolTable[name] = value
                foundSymbol = true
                break
            }
        }

        if (!foundSymbol)
            miniScript.environment.lastFrame.symbolTable[name] = value
    }
}