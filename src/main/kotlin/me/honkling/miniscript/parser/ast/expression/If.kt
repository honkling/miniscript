package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.pass.Pass

class If(val expression: Expression<*>, val block: Block, val elseBlock: Block?, location: Location, parent: Node<*>?) : Expression<Any?>(location, parent) {
    override fun get(): Pair<Any?, ExecutionResult> {
        val value = expression.get().first

        if (value !is Boolean)
            throw MiniScriptException.RuntimeError("Expected boolean for if statement", this)

        return if (value) block.execute()
        else elseBlock?.execute()
            ?: (null to ExecutionResult.ContinueExecution)
    }

    override fun accept(pass: Pass) {
        pass.visitIf(this)
    }
}