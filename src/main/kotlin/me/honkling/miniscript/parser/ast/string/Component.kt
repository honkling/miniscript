package me.honkling.miniscript.parser.ast.string

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Value
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.parser.ast.expression.Expression as MSExpression

abstract class Component(parent: ComplexString?) : Value<String>(parent) {
    class Plain(private val value: String, parent: ComplexString?) : Component(parent) {
        override fun get() = value to ExecutionResult.ContinueExecution
    }

    class Variable(private val name: String, parent: ComplexString?) : Component(parent) {
        override fun get(): Pair<String, ExecutionResult> {
            val value = (parent!!.getSymbol(name)?.toString()
                ?: throw MiniScriptException.RuntimeError("Symbol '$name' doesn't exist."))

            return value to ExecutionResult.ContinueExecution
        }
    }

    class Expression(private val expression: MSExpression<*>, parent: ComplexString?) : Component(parent) {
        override fun get(): Pair<String, ExecutionResult> {
            return expression.get().first.toString() to ExecutionResult.ContinueExecution
        }
    }
}