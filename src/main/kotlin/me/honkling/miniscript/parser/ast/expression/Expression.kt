package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.statement.ExecutionResult

abstract class Expression<T>(parent: Node<*>?) : Node<Node<*>?>(parent) {
    abstract fun get(): Pair<T, ExecutionResult>
}