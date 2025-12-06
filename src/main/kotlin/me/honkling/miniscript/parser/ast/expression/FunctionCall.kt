package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.Value
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.prototype.FunctionReference
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

        if (result is Class)
            return result.instantiate(arguments) to ExecutionResult.ContinueExecution

        if (result !is FunctionReference && (result as? Pair<*, *>)?.second !is FunctionReference)
            throw MiniScriptException.RuntimeError("Expected a function")

        val arguments = mutableListOf<Any>()
        val (instance, function) = (result as? Pair<Any, FunctionReference>)?.second ?: result as FunctionReference

        for ((index, parameter) in function.parameters.withIndex()) {
            if ((index == 0 && parameter.name == "this") || (index == 1 && parameter.name == "super"))
                continue

            if (parameter.isVararg) {
                val rest = this.arguments.slice(index..<this.arguments.size)
                arguments += rest.map { it.get().first }
                break
            }

            arguments += this.arguments[index].get().first
                ?: throw MiniScriptException.RuntimeError("Expected argument value, found nothing")
        }

        val symbols = mutableMapOf<String, Any?>()
        instance?.let { symbols["this"] = it }

        return function.call(*arguments.toTypedArray(), lambda = lambda, symbols = symbols) to ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitFunctionCall(this)
    }
}