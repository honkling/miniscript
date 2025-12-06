package me.honkling.miniscript.parser.ast.prototype

import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.function.Parameter
import me.honkling.miniscript.pass.Pass

class Class(
    name: String?,
    var superClass: Class?,
    val superArgs: List<Expression<*>>,
    val methods: List<Function>,
    val constructorParameters: List<Parameter>,
    fields: MutableList<Field>,
    parent: Node<*>?
) : Prototype(name, fields, parent) {
    fun instantiate(arguments: List<Expression<*>>): ClassInstance {
        val instance = superClass?.instantiate(superArgs)
            ?: ClassInstance(this)

        instance.classRef = this

        for ((index, parameter) in constructorParameters.withIndex()) {
            val defaultValue = parameter.defaultValue?.get()?.first
            val value = arguments.getOrNull(index)?.get()?.first ?: defaultValue
                ?: continue

            if (parameter.name!! !in instance.fields || instance.fields[parameter.name] == defaultValue)
                instance.fields[parameter.name] = value
        }

        return instance
    }

    fun tryResolveFunction(name: Any?): Function? {
        return methods.find { it.name == name }
            ?: superClass?.tryResolveFunction(name)
    }

    override fun accept(pass: Pass) {
        pass.visitClass(this)
    }
}