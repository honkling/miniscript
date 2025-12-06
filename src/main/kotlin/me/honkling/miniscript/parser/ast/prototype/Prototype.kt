package me.honkling.miniscript.parser.ast.prototype

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.parser.ast.Node

abstract class Prototype(
    val name: String?,
    val fields: List<Field>,
    location: Location,
    parent: Node<*>?
) : Node<Node<*>?>(location, parent)