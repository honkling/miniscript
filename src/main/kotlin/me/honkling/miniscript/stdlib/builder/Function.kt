package me.honkling.miniscript.stdlib.builder

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.function.Parameter
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.parser.ast.statement.NativeStatement

fun Environment.function(
    name: String,
    returnType: Type<*>? = null,
    vararg parameters: Pair<String, Type<*>>,
    executionBlock: NativeStatement.() -> Unit
): Function {
    val block = Block(miniScript, mutableListOf(), null, null)
    block.statements += NativeStatement(executionBlock, block)

    val parameters = parameters.map { Parameter(it.first, it.second, null, false, emptyLocation, null) }
    val function = Function(name, parameters.toMutableList(), returnType, block, false, emptyLocation, this)
    parameters.forEach { it.parent = function }
    block.parent = function

    return function
}