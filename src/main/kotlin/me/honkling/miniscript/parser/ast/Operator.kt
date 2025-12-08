package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.lexer.TokenType

enum class Operator(
    val token: TokenType,
    val precedence: Int,
    val isRightAssociated: Boolean,
    val isArithmetic: Boolean = true
) {
    LogicalNOT(TokenType.Bang, 0, false, true),
    And(TokenType.And, 1, false),
    Or(TokenType.Or, 1, false),
    Equals(TokenType.Equals, 1, false),
    NotEquals(TokenType.NotEquals, 2, false),
    GreaterThan(TokenType.GreaterThan, 2, false),
    GreaterEquals(TokenType.GreaterEquals, 2, false),
    LessThan(TokenType.LessThan, 2, false),
    LessEquals(TokenType.LessEquals, 2, false),
    Plus(TokenType.Plus, 3, false),
    Minus(TokenType.Minus, 3, false),
    Multiply(TokenType.Multiply, 4, false),
    Divide(TokenType.Divide, 4, false),
    Period(TokenType.Period, 5, false)
}

fun operator(type: TokenType)
    = Operator.entries.find { it.token == type }