package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.pass.Pass
import me.honkling.miniscript.stdlib.tryGetChangers

class UnaryOperator(
    val expression: Expression<*>,
    val operator: Operator,
    location: Location,
    parent: Node<*>?
) : Expression<Any?>(location, parent) {
    override fun get(): Pair<Any?, ExecutionResult> {
        val (value, executionResult) = expression.get()

        if (value == null)
            throw MiniScriptException.RuntimeError("Expected unary value", this)

        val miniScript = getBlockParent()!!.miniScript
        val changers = tryGetChangers(miniScript, value, Unit, value::class, Unit::class, operator)
            ?: throw MiniScriptException.RuntimeError("Invalid operator $operator for '$value'", this)

        for (changer in changers) {
            try {
                return changer.block(value, Unit, operator) to executionResult
            } catch (_: MiniScriptException.WrongChanger) {}
        }

        return null to executionResult
    }

    override fun accept(pass: Pass) {
        pass.visitUnary(this)
    }
}