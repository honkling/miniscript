package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.parser.ast.statement.Statement
import me.honkling.miniscript.pass.Pass

class Break(parent: Node<*>?) : Expression<Nothing?>(parent) {
    override fun get(): Pair<Nothing?, ExecutionResult> {
        return null to ExecutionResult.BreakLoop
    }

    override fun accept(pass: Pass) {
        pass.visitBreak(this)
    }
}