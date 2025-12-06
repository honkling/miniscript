package me.honkling.miniscript.diagnostic

import me.honkling.miniscript.parser.ast.Node

open class MiniScriptException(message: String? = null) : Exception(message) {
    class WrongChanger : MiniScriptException()
    class LexError : MiniScriptException()
    class ParseError : MiniScriptException()
    class RuntimeError(message: String, val node: Node<*>?) : MiniScriptException(message)
}