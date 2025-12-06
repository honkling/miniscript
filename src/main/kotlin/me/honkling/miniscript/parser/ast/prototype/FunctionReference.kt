package me.honkling.miniscript.parser.ast.prototype

import me.honkling.miniscript.parser.ast.function.Function

data class FunctionReference(
    val instance: ClassInstance?,
    val function: Function
)