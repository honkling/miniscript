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
    init {
        for (method in methods) {
            method.parameters.add(0, Parameter("super", Type.Dictionary, null, false, method))
            method.parameters.add(0, Parameter("this", Type.Dictionary, null, false, method))
        }

        fields += Field("super", Type.Dictionary, null, null)
    }

    fun instantiate(arguments: List<Expression<*>>): MutableMap<Any?, Any?> {
        val instance = superClass?.instantiate(superArgs)
            ?: mutableMapOf()

        val superRef = mutableMapOf(*instance.entries.map { it.key to it.value }
            .toTypedArray())

        for (method in methods)
            instance[method.name!!] = method

        for (field in fields)
            field.value?.let { instance[field.name] = it.get().first }

        for ((index, parameter) in constructorParameters.withIndex()) {
            val value = (arguments.getOrNull(index) ?: parameter.defaultValue)
                ?.get()?.first ?: continue

            instance[parameter.name!!] = value
        }

        instance["super"] = superRef
        return instance
    }

    override fun accept(pass: Pass) {
        pass.visitClass(this)
    }
}