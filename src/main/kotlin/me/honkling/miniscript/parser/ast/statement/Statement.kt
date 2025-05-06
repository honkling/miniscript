package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node

abstract class Statement(parent: Block) : Node<Block>(parent) {
    abstract fun execute(): ExecutionResult
}

enum class ExecutionResult {
    ContinueExecution,
    ContinueLoop,
    BreakLoop,
    Return
}