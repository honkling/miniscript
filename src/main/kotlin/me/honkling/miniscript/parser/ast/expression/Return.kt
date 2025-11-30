package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.parser.ast.statement.Statement
import me.honkling.miniscript.pass.Pass

class Return(
    val expression: Expression<*>,
    parent: Node<*>?
) : Expression<Nothing?>(parent) {
    override fun get(): Pair<Nothing?, ExecutionResult> {
        getParent<Function>(Function::class).block!!.returnValue = expression.get().first
        return null to ExecutionResult.Return
    }

    override fun accept(pass: Pass) {
        pass.visitReturn(this)
    }
}