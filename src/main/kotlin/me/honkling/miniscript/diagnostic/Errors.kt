package me.honkling.miniscript.diagnostic

open class MiniScriptException(message: String? = null) : Exception(message) {
    class LexError : MiniScriptException()
    class ParseError : MiniScriptException()
    class RuntimeError(message: String? = null) : MiniScriptException(message)
}