package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.Value
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.pass.Pass
import kotlin.collections.plus

class FunctionCall(
    val reference: Expression<*>,
    val arguments: List<Expression<*>>,
    val lambda: Function?,
    parent: Node<*>?
) : Expression<Any?>(parent) {
    override fun get(): Pair<Any?, ExecutionResult> {
        val result = (reference as? Arithmetic)?.getWithLeftSide() ?: reference.get().first
        val arguments = mutableListOf<Any>()

        if (result is Class) {
            val instance = mutableMapOf<Any?, Any?>()

            for (method in result.methods)
                instance[method.name!!] = method

            for (field in result.fields)
                field.value?.let { instance[field.name] = it.get().first }

            return instance to ExecutionResult.ContinueExecution
        }

        if ((result !is Pair<*, *> || result.second !is Function) && result !is Function)
            throw MiniScriptException.RuntimeError("Expected a function")

        val function = (result as? Pair<Any, Function>)?.second ?: result as Function

        if (function.parameters.firstOrNull()?.name == "this") {
            // This function belongs to a class.
            val instance = (result as Pair<Any, Function>).first
            arguments += instance
        }

        var index = 0

        for (parameter in function.parameters) {
            if (parameter.isVararg) {
                val rest = this.arguments.slice(index..<this.arguments.size)
                arguments += rest.map { it.get().first }
                break
            }

            arguments += this.arguments[index++].get().first
                ?: throw MiniScriptException.RuntimeError("Expected argument value, found nothing")
        }

        return function.call(*arguments.toTypedArray(), lambda = lambda) to ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitFunctionCall(this)
    }
}