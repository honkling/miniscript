package me.honkling.miniscript.parser.ast.prototype

import me.honkling.miniscript.parser.ast.Node

abstract class Prototype(
    val name: String?,
    val fields: List<Field>,
    parent: Node<*>?
) : Node<Node<*>?>(parent)