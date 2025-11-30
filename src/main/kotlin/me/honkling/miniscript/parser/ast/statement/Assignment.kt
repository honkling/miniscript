package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.expression.Assignable
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.pass.Pass

class Assignment(
    val reference: Assignable,
    val expression: Expression<*>?,
    val operator: TokenType,
    parent: Block
) : Statement(parent) {
    override fun execute(): ExecutionResult {
        reference.set(operator, expression?.get()?.first)
        return ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitAssignment(this)
    }
}