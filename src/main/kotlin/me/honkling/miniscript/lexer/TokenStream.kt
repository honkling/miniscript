package me.honkling.miniscript.lexer

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.Logger
import me.honkling.miniscript.diagnostic.MiniScriptException

class TokenStream(
    private val tokens: List<Token>,
    internal val logger: Logger,
    private val location: Location = logger.location
) {
    fun peek(): Token {
        if (location.index >= tokens.size) {
            logger.error("Reached end of file unexpectedly")
            throw MiniScriptException.ParseError()
        }

        return tokens[location.index]
    }

    fun consume(): Token {
        if (location.index >= tokens.size) {
            logger.error("Reached end of file unexpectedly")
            throw MiniScriptException.ParseError()
        }

        val token = tokens[location.index++]
        location.line = token.location.line
        location.column = token.location.column
        return token
    }

    fun branch(): TokenStream {
        return TokenStream(tokens, logger, location.clone())
    }

    fun merge(stream: TokenStream) {
        val location = stream.location
        this.location.index = location.index
        this.location.line = location.line
        this.location.column = location.column
    }

    fun isEnd(): Boolean {
        return location.index >= tokens.size || tokens[location.index].type == TokenType.EndOfInput
    }
}