package me.honkling.miniscript.parser.ast.function

import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.pass.Pass

class Parameter(
    val name: String?,
    val type: Type<*>?,
    val defaultValue: Expression<*>?,
    parent: Node<*>?
) : Node<Node<*>?>(parent) {
    override fun accept(pass: Pass) {
        pass.visitParameter(this)
    }
}