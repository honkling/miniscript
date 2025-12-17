package me.honkling.miniscript.stdlib.builder

import me.honkling.miniscript.miniScript
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.function.Parameter
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.prototype.Field
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.parser.ast.statement.NativeStatement

class ClassBuilder(
    val environment: Environment,
    val name: String,
    constructorFields: List<Pair<String, Type<*>>>
) {
    val constructorFields = constructorFields.map { Parameter(it.first, it.second, null, false, emptyLocation, null) }
    val methods = mutableListOf<Function>()
    val fields = mutableListOf<Field>()
    var initializer: Block? = null

    fun init(initializerBlock: NativeStatement.() -> Unit) {
        val block = Block(environment.miniScript, mutableListOf(), emptyLocation, null)
        block.statements += NativeStatement(initializerBlock, block)
        initializer = block
    }

    fun function(
        name: String,
        returnType: Type<*>? = null,
        vararg parameters: Pair<String, Type<*>>,
        executionBlock: NativeStatement.() -> Unit
    ) {
        val function = environment.function(name, returnType, *parameters, executionBlock = executionBlock)
        methods += function
    }

    fun field(
        name: String,
        type: Type<*>
    ) {
        fields += Field(name, type, null, emptyLocation, null)
    }

    fun build(): Class {
        val `class` = Class(name, null, emptyList(), methods, initializer, constructorFields, fields, emptyLocation, environment)
        constructorFields.forEach { it.parent = `class` }
        methods.forEach { it.parent = `class` }
        fields.forEach { it.parent = `class` }
        initializer?.parent = `class`
        return `class`
    }
}

fun Environment.`class`(name: String, vararg constructorFields: Pair<String, Type<*>>, block: ClassBuilder.() -> Unit): Class {
    val builder = ClassBuilder(this, name, constructorFields.toList())
    block(builder)
    return builder.build()
}