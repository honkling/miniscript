package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.pass.Pass

class If(val expression: Expression<*>, val block: Block, val elseBlock: Block?, parent: Block) : Statement(parent) {
    override fun execute(): ExecutionResult {
        val value = expression.get()

        if (value !is Boolean)
            throw MiniScriptException.RuntimeError("Expected boolean for if statement")

        return if (value) block.execute()
        else elseBlock?.execute()
            ?: ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitIf(this)
    }
}