package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.pass.Pass

fun nativeBlock(miniScript: MiniScript, function: Function, runnable: NativeStatement.() -> Unit): Block {
    val block = Block(miniScript, mutableListOf(), null, function)
    val statement = NativeStatement(runnable, block)
    block.statements += statement
    return block
}

class NativeStatement(val block: NativeStatement.() -> Unit, parent: Block) : Statement(null, parent) {
    override fun execute(): ExecutionResult {
        block(this)
        return ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitNativeStatement(this)
    }
}