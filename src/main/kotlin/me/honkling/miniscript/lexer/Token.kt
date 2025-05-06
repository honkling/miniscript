package me.honkling.miniscript.lexer

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.Logger
import me.honkling.miniscript.diagnostic.MiniScriptException

open class Token(
    val type: TokenType,
    val raw: String,
    val location: Location,
    private val logger: Logger
) {
    class WithValue<T : Any>(
        type: TokenType,
        raw: String,
        location: Location,
        logger: Logger,
        val value: T
    ) : Token(type, raw, location, logger)

    fun expect(
        type: TokenType,
        message: String = "Expected {0}, found {1}"
    ): Token {
        if (this.type != type) {
            logger.error(message.replace("{0}", asString(type))
                .replace("{1}", asString()))

            throw MiniScriptException.ParseError()
        }

        return this
    }

    fun asString(type: TokenType = this.type): String {
        val value = type.value?.let { "'$it'" } ?: type.name.lowercase()

        if (this.type != type)
            return value

        return when (type) {
            TokenType.Boolean, TokenType.Number -> "'${(this as Token.WithValue<*>).value}'"
            TokenType.EndOfInput -> "end of file"
            else -> value
        }
    }

    override fun toString(): String {
        return "Token(type=${type.name}, raw=\"${raw.replace("\"", "\\\"")}\", location=$location)"
    }
}

enum class TokenType(val value: String? = null) {
    EndOfInput,
    Identifier,
    Whitespace,

    Function("func"),
    For("for"),
    ForEach("foreach"),
    If("if"),
    Else("else"),
    While("while"),
    StringType("string"),
    NumberType("number"),
    BooleanType("boolean"),
    DictType("dict"),
    VoidType("void"),
    AnyType("any"),
    Return("return"),
    Continue("continue"),
    Break("break"),
    Native("native"),

    Number,
    Boolean,
    String,

    OpenParen("("),
    CloseParen(")"),
    OpenBracket("["),
    CloseBracket("]"),
    OpenBrace("{"),
    CloseBrace("}"),
    And("&&"),
    Or("||"),
    Colon(":"),
    Comma(","),
    Period("."),
    Arrow("->"),
    Equals("=="),
    Assign("="),
    NotEquals("!="),
    GreaterEquals(">="),
    LessEquals("<="),
    GreaterThan(">"),
    LessThan("<"),
    PlusAssign("+="),
    MinusAssign("-="),
    MultiplyAssign("*="),
    DivideAssign("/="),
    Increment("++"),
    Decrement("--"),
    Plus("+"),
    Minus("-"),
    Multiply("*"),
    Divide("/")
}