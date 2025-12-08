package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.function.Parameter
import me.honkling.miniscript.pass.Pass
import me.honkling.miniscript.parser.ast.function.Function as MSFunction
import me.honkling.miniscript.parser.ast.prototype.Class as MSClass

abstract class Type<T>(parent: Node<*>? = null) : Node<Node<*>?>(null, parent) {
    object Char : Type<kotlin.Char>()
    object Number : Type<kotlin.Number>()
    object Boolean : Type<kotlin.Boolean>()
    class Array<T>(val innerType: Type<T>) : Type<kotlin.Array<T>>()
    object Dictionary : Type<HashMap<Any?, Any?>>()
    object Void : Type<Nothing?>()
    object Any : Type<Any>()
    class Class(val reference: Expression<*>, parent: Node<*>?) : Type<MSClass>(parent)
    class Function(
        val parameters: List<Parameter>,
        val returnType: Type<*>
    ) : Type<MSFunction>()

    override fun accept(pass: Pass) {
        pass.visitType(this)
    }
}