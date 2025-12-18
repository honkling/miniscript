package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.Value
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.prototype.FunctionReference
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.pass.Pass
import kotlin.collections.plus

class FunctionCall(
    val reference: Expression<*>,
    val arguments: List<Expression<*>>,
    val lambda: Function?,
    location: Location,
    parent: Node<*>?
) : Expression<Any?>(location, parent) {
    override fun get(): Pair<Any?, ExecutionResult> {
        val result = (reference as? Arithmetic)?.getWithLeftSide() ?: reference.get().first

        if (result is Class)
            return result.instantiate(arguments) to ExecutionResult.ContinueExecution

        if (result !is FunctionReference && (result as? Pair<*, *>)?.second !is FunctionReference)
            throw MiniScriptException.RuntimeError("Expected a function", this)

        val arguments = mutableListOf<Any>()
        val (instance, function) = (result as? Pair<Any, FunctionReference>)?.second ?: result as FunctionReference

        for ((index, parameter) in function.parameters.withIndex()) {
            if ((index == 0 && parameter.name == "this") || (index == 1 && parameter.name == "super"))
                continue

            if (parameter.isVararg) {
                val varargArguments = mutableListOf<Any>()
                val rest = this.arguments.slice(index..<this.arguments.size)

                for (argument in rest) {
                    val (value, result) = argument.get()

                    if (result != ExecutionResult.ContinueExecution)
                        return value to result

                    if (value == null)
                        throw MiniScriptException.RuntimeError("Expected argument value, found nothing", this)

                    if (argument is Value.Spread)
                        varargArguments.addAll((value as ClassInstance).fields["data"] as List<Any>)
                    else varargArguments += value
                }

                val arrayClass = getBlockParent()!!.miniScript.environment.arrayClass
                arguments += arrayClass.instantiate(varargArguments)
                break
            }

            val argument = this.arguments[index]

            if (argument is Value.Spread)
                throw MiniScriptException.RuntimeError("Cannot spread list for a non-vararg parameter.", this)

            arguments += argument.get().first
                ?: throw MiniScriptException.RuntimeError("Expected argument value, found nothing", this)
        }

        val symbols = mutableMapOf<String, Any?>()
        instance?.let { symbols["this"] = it }

        return function.call(*arguments.toTypedArray(), lambda = lambda, symbols = symbols) to ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitFunctionCall(this)
    }
}