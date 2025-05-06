package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.pass.Pass

fun nativeBlock(miniScript: MiniScript, function: Function, runnable: Native.() -> Unit): Block {
    val block = Block(miniScript, mutableListOf(), function)
    val statement = Native(runnable, block)
    block.statements += statement
    return block
}

class Native(val block: Native.() -> Unit, parent: Block) : Statement(parent) {
    override fun execute(): ExecutionResult {
        block(this)
        return ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitNative(this)
    }
}