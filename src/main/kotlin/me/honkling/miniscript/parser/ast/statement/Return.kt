package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.pass.Pass

class Return(
    val expression: Expression<*>,
    parent: Block
) : Statement(parent) {
    override fun execute(): ExecutionResult {
        parent.returnValue = expression.get()
        return ExecutionResult.Return
    }

    override fun accept(pass: Pass) {
        pass.visitReturn(this)
    }
}