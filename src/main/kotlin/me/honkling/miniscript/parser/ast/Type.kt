package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.parser.ast.function.Parameter
import me.honkling.miniscript.pass.Pass
import me.honkling.miniscript.parser.ast.function.Function as MSFunction

abstract class Type<T> : Node<Nothing?>(null) {
    object String : Type<kotlin.String>()
    object Number : Type<kotlin.Number>()
    object Boolean : Type<kotlin.Boolean>()
    class Array<T>(val innerType: Type<T>) : Type<kotlin.Array<Any>>()
    object Dictionary : Type<HashMap<String, Any>>()
    object Void : Type<Nothing?>()
    object Any : Type<Any>()
    class Function(
        val parameters: List<Parameter>,
        val returnType: Type<*>
    ) : Type<MSFunction>()

    override fun accept(pass: Pass) {
        pass.visitType(this)
    }
}