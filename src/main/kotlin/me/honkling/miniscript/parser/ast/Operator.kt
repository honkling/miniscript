package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.lexer.TokenType

enum class Operator(
    val token: TokenType,
    val precedence: Int,
    val isRightAssociated: Boolean
) {
    And(TokenType.And, 0, false),
    Or(TokenType.Or, 0, false),
    Equals(TokenType.Equals, 1, false),
    NotEquals(TokenType.NotEquals, 1, false),
    GreaterThan(TokenType.GreaterThan, 1, false),
    GreaterEquals(TokenType.GreaterEquals, 1, false),
    LessThan(TokenType.LessThan, 1, false),
    LessEquals(TokenType.LessEquals, 1, false),
    Plus(TokenType.Plus, 2, false),
    Minus(TokenType.Minus, 2, false),
    Multiply(TokenType.Multiply, 3, false),
    Divide(TokenType.Divide, 3, false),
    Period(TokenType.Period, 4, false)
}

fun operator(type: TokenType)
    = Operator.entries.find { it.token == type }