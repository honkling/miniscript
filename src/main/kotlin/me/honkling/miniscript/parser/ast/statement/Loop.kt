package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.pass.Pass

abstract class Loop(val block: Block, parent: Block) : Statement(parent) {
    class ForEach(
        val identifier: String,
        val expression: Expression<*>,
        block: Block,
        parent: Block
    ) : Loop(block, parent) {
        override fun execute(): ExecutionResult {
            val values = expression.get().first

            if (values !is Iterable<*>)
                throw MiniScriptException.RuntimeError("Expected an iterable value")

            for (value in values) {
                val frame = block.pushToStack()
                frame.symbolTable[identifier] = value
                val result = block.execute(false).second
                block.popFromStack()

                when (result) {
                    ExecutionResult.Return -> return result
                    ExecutionResult.BreakLoop -> break
                    else -> {}
                }
            }

            return ExecutionResult.ContinueExecution
        }
    }

    class While(
        val expression: Expression<*>,
        block: Block,
        parent: Block
    ) : Loop(block, parent) {
        override fun execute(): ExecutionResult {
            var result = expression.get().first

            if (result !is Boolean)
                throw MiniScriptException.RuntimeError("Expected boolean for while statement")

            while (result == true) {
                when (block.execute().second) {
                    ExecutionResult.Return -> {
                        return ExecutionResult.Return
                    }
                    ExecutionResult.BreakLoop -> break
                    else -> {}
                }

                result = expression.get().first

                if (result !is Boolean)
                    throw MiniScriptException.RuntimeError("Expected boolean for while statement")
            }

            return ExecutionResult.ContinueExecution
        }
    }

    override fun accept(pass: Pass) {
        pass.visitLoop(this)
    }
}