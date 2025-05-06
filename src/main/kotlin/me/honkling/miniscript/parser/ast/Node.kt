package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.pass.Pass

abstract class Node<T>(
    var parent: T
) {
    abstract fun accept(pass: Pass)

    fun getBlockParent(): Block?
        = (parent as? Block) ?: (parent as Node<*>?)?.getBlockParent()

    fun getSymbol(name: String): Any? {
        if (this is Block && name in symbolTable)
            return symbolTable[name]

        return (parent as Node<*>?)?.getSymbol(name)
    }

    fun setSymbol(name: String, value: Any?) {
        if (!setSymbolInternal(name, value))
            getBlockParent()!!.symbolTable[name] = value
    }

    private fun setSymbolInternal(name: String, value: Any?): Boolean {
        if (this is Block && name in symbolTable) {
            symbolTable[name] = value
            return true
        }

        return (parent as Node<*>?)?.setSymbolInternal(name, value) == true
    }
}