package me.honkling.miniscript.parser.ast.prototype

import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.pass.Pass

class Field(
    val name: String,
    val type: Type<*>,
    val value: Expression<*>?,
    parent: Prototype?
) : Node<Prototype?>(parent) {
    override fun accept(pass: Pass) {
        pass.visitField(this)
    }
}