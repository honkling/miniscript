package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.lexer.TokenType

interface Assignable {
    fun canAssign(): Boolean
    fun set(operator: TokenType, value: Any?)
}