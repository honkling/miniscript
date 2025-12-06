package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.statement.ExecutionResult

abstract class Expression<T>(location: Location?, parent: Node<*>?) : Node<Node<*>?>(location, parent) {
    abstract fun get(): Pair<T, ExecutionResult>
}