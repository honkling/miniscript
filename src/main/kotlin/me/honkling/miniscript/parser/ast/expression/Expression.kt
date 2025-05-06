package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.parser.ast.Node

abstract class Expression<T>(parent: Node<*>?) : Node<Node<*>?>(parent) {
    abstract fun get(): T
}