package me.honkling.miniscript.parser.ast.prototype

import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.function.Parameter
import me.honkling.miniscript.pass.Pass

class Class(
    name: String?,
    var superClass: Class?,
    val methods: List<Function>,
    fields: List<Field>,
    parent: Node<*>?
) : Prototype(name, fields, parent) {
    init {
        for (method in methods)
            method.parameters.add(0, Parameter("this", Type.Dictionary, null, false, method))
    }

    override fun accept(pass: Pass) {
        pass.visitClass(this)
    }
}