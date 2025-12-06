package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node

abstract class Statement(location: Location?, parent: Block) : Node<Block>(location, parent) {
    abstract fun execute(): ExecutionResult
}

enum class ExecutionResult {
    ContinueExecution,
    ContinueLoop,
    BreakLoop,
    Return
}