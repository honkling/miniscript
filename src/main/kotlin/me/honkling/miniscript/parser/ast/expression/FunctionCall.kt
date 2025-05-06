package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.pass.Pass
import kotlin.collections.plus

class FunctionCall(
    val reference: Expression<*>,
    val arguments: List<Expression<*>>,
    val lambda: Function?,
    parent: Node<*>?
) : Expression<Any?>(parent) {
    override fun get(): Any? {
        val function = reference.get()
        val arguments = mutableListOf<Any>()

        if (function !is Function)
            throw MiniScriptException.RuntimeError("Expected a function")

        for (argument in this.arguments)
            arguments += argument.get()
                ?: throw MiniScriptException.RuntimeError("Expected argument value, found nothing")

        return function.call(*arguments.toTypedArray(), lambda = lambda)
    }

    override fun accept(pass: Pass) {
        pass.visitFunctionCall(this)
    }
}