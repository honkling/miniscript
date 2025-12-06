package me.honkling.miniscript.parser.ast.prototype

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.pass.Pass

class Field(
    val name: String,
    val type: Type<*>,
    val value: Expression<*>?,
    location: Location,
    parent: Prototype?
) : Node<Prototype?>(location, parent) {
    override fun accept(pass: Pass) {
        pass.visitField(this)
    }
}