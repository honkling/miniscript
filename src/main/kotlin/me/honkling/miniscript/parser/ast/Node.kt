package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.pass.Pass
import kotlin.reflect.KClass

abstract class Node<T>(
    var parent: T
) {
    abstract fun accept(pass: Pass)

    fun <T : Any> getParent(klass: KClass<T>): T
        = if (klass == parent!!::class) parent as T else (parent as Node<*>?)!!.getParent(klass)

    fun getBlockParent(): Block?
        = (parent as? Block) ?: (parent as Node<*>?)?.getBlockParent()

    fun getSymbol(name: String): Any? {
        val environment = getBlockParent()!!.miniScript.environment

        for (index in environment.executionStack.size - 1 downTo 0) {
            val frame = environment.executionStack[index]

            if (name in frame.symbolTable)
                return frame.symbolTable[name]
        }

        return null
    }

    fun setSymbol(name: String, value: Any?) {
        if (!setSymbolInternal(name, value)) {
            val miniScript = getBlockParent()!!.miniScript
            miniScript.environment.lastFrame.symbolTable[name] = value
        }
    }

    private fun setSymbolInternal(name: String, value: Any?): Boolean {
        if (this is SymbolHolder && name in symbolTable) {
            symbolTable[name] = value
            return true
        }

        return (parent as Node<*>?)?.setSymbolInternal(name, value) == true
    }
}