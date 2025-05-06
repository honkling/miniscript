package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.pass.Pass

class Continue(parent: Block) : Statement(parent) {
    override fun execute(): ExecutionResult {
        return ExecutionResult.ContinueLoop
    }

    override fun accept(pass: Pass) {
        pass.visitContinue(this)
    }
}