package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.pass.Pass

class Break(parent: Block) : Statement(parent) {
    override fun execute(): ExecutionResult {
        return ExecutionResult.BreakLoop
    }

    override fun accept(pass: Pass) {
        pass.visitBreak(this)
    }
}