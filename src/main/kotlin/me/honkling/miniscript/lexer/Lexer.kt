package me.honkling.miniscript.lexer

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.Logger
import me.honkling.miniscript.diagnostic.MiniScriptException

private const val newLine = '\n'
private const val escape = '\\'
private const val doubleQuotes = '"'
private const val singleQuotes = '\''
private const val period = '.'

class Lexer(
    private val input: String,
    private val logger: Logger,
    private val location: Location = logger.location,
    private val internalMode: Boolean = false
) {
    fun lex(): List<Token> {
        val tokens = mutableListOf<Token>()

        while (!isEnd())
            try {
                tokens += lexToken()
            } catch (_: MiniScriptException.LexError) {}

        tokens += Token(TokenType.EndOfInput, "", location.clone(), logger)
        return tokens
    }

    private fun lexToken(): Token {
        val character = input[location.index]

        if (character == singleQuotes)
            return lexCharacter()

        if (character == doubleQuotes)
            return lexString()

        if (character.isDigit())
            return lexNumber()

        if (character.isWhitespace())
            return lexGreedyTokenByPredicate(TokenType.Whitespace, Char::isWhitespace)

        if (character.isLetter() || character == '_') {
            val start = location.clone()
            val raw = lexGreedyStringByPredicate { char, index ->
                (index == 0 && (char.isLetter() || char == '_')) || char.isLetterOrDigit() || char == '_'
            }

            val isTrue = raw == "true"
            if (isTrue || raw == "false")
                return Token.WithValue(TokenType.Boolean, raw, start, logger, isTrue)

            for (type in TokenType.entries)
                if (type.value == raw)
                    return Token(type, raw, start, logger)

            return Token(TokenType.Identifier, raw, start, logger)
        }

        for (type in TokenType.entries)
            if (input.startsWith(type.value ?: continue, location.index))
                return Token(
                    if (type == TokenType.Native && !internalMode)
                        TokenType.Identifier
                    else type,
                    type.value,
                    advance(type.value.length),
                    logger
                )

        advance(1)
        logger.error("Unexpected character found during lexing")
        throw MiniScriptException.LexError()
    }

    private fun lexCharacter(): Token.WithValue<Char> {
        val start = location.clone()
        val raw = StringBuilder()

        raw.append(input[advance(1).index])
        val consumedCharacter = input.getOrNull(advance(1).index)
        raw.append(consumedCharacter)

        val character = when (consumedCharacter) {
            escape -> {
                val character = input.getOrNull(advance(1).index)
                raw.append(character)
                character
            }
            else -> consumedCharacter
        }

        if (character == null) {
            logger.error("Expected a character, found nothing")
            throw MiniScriptException.LexError()
        }

        val endingCharacter = input.getOrNull(advance(1).index)
        if (endingCharacter != singleQuotes) {
            logger.error("Expected \"'\", found ${endingCharacter?.let { "'$it'" } ?: "nothing"}")
            throw MiniScriptException.LexError()
        }

        raw.append(endingCharacter)
        return Token.WithValue(TokenType.Character, raw.toString(), start, logger, character)
    }

    private fun lexNumber(): Token.WithValue<Double> {
        val start = location.clone()
        val raw = StringBuilder()
        var hasPeriod = false
        var divisor = 0.1
        var value = 0.0

        while (!isEnd() && (input[location.index].isDigit() || (!hasPeriod && input[location.index] == period))) {
            val character = input[location.index++]

            raw.append(character)

            if (character == period) {
                hasPeriod = true
                continue
            }

            val digitValue = character.digitToInt()

            if (!hasPeriod) {
                value *= 10
                value += digitValue
            } else {
                value += digitValue * divisor
                divisor /= 10
            }
        }

        return Token.WithValue(TokenType.Number, raw.toString(), start, logger, value)
    }

    private fun lexString(): Token.WithValue<String> {
        val start = location.clone()
        val value = StringBuilder()
        val raw = StringBuilder("\"")
        advance(1)

        while (true) {
            val character = input.getOrNull(advance(1).index)

            if (isEnd() || character == newLine) {
                val representation = "end of " + (character?.let { "line" } ?: "file")
                logger.error("Expected string to close, found $representation")
                throw MiniScriptException.LexError()
            }

            raw.append(character)

            when (character) {
                doubleQuotes -> break
                escape -> {
                    val escapedCharacter = input.getOrNull(advance(1).index)

                    if (escapedCharacter == null) {
                        logger.error("Expected character to escape, found end of file")
                        throw MiniScriptException.LexError()
                    }

                    raw.append(escapedCharacter)

                    if (escapedCharacter == 'n')
                        value.append("\n")
                    else if (escapedCharacter == 't')
                        value.append("\t")
                    else value.append(escapedCharacter)
                }
                else -> value.append(character)
            }
        }

        return Token.WithValue(TokenType.String, raw.toString(), start, logger, value.toString())
    }

    private fun lexGreedyTokenByPredicate(type: TokenType, predicate: (Char) -> Boolean)
        = lexGreedyTokenByPredicate(type) { char, _ -> predicate(char) }

    private fun lexGreedyTokenByPredicate(type: TokenType, predicate: (Char, Int) -> Boolean): Token {
        val start = location.clone()
        return Token(type, lexGreedyStringByPredicate(predicate), start, logger)
    }

    private fun lexGreedyStringByPredicate(predicate: (Char) -> Boolean)
            = lexGreedyStringByPredicate { char, _ -> predicate(char) }

    private fun lexGreedyStringByPredicate(predicate: (Char, Int) -> Boolean): String {
        val raw = StringBuilder()

        while (!isEnd() && predicate(input[location.index], location.index))
            raw.append(input[advance(1).index])

        return raw.toString()
    }
    
    private fun advance(length: Int): Location {
        val start = location.clone()
        
        repeat(length) {
            val character = input[location.index++]
            
            if (character == '\n') {
                location.line++
                location.column = 1
            } else location.column++
        }
        
        return start
    }

    private fun isEnd()
        = location.index >= input.length
}