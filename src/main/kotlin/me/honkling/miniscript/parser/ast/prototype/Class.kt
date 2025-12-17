package me.honkling.miniscript.parser.ast.prototype

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.parser.ast.Block
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
    val initializer: Block?,
    val constructorParameters: List<Parameter>,
    fields: MutableList<Field>,
    location: Location,
    parent: Node<*>?
) : Prototype(name, fields, location, parent) {
    fun instantiate(arguments: List<Expression<*>>): ClassInstance {
        val values = mutableListOf<Any>()

        for ((index, parameter) in constructorParameters.withIndex()) {
            val value = arguments.getOrNull(index)?.get()?.first
                ?: parameter.defaultValue?.get()?.first
                ?: continue

            values += value
        }

        return instantiate(*values.toTypedArray())
    }

    fun instantiate(vararg arguments: Any): ClassInstance {
        val instance = superClass?.instantiate(superArgs)
            ?: ClassInstance(this)

        instance.classRef = this

        for (field in fields) {
            val value = field.value?.get()?.first
                ?: continue

            instance.fields[field.name] = value
        }

        for ((index, parameter) in constructorParameters.withIndex())
            instance.fields[parameter.name] = arguments[index]

        if (initializer != null) {
            val frame = initializer.pushToStack()
            frame.symbolTable["this"] = instance
            initializer.execute(false)
            initializer.popFromStack()
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