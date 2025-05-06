package me.honkling.miniscript.parser

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.lexer.Lexer
import me.honkling.miniscript.lexer.Token
import me.honkling.miniscript.lexer.TokenStream
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.Value
import me.honkling.miniscript.parser.ast.expression.Arithmetic
import me.honkling.miniscript.parser.ast.expression.Assignable
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.expression.FunctionCall
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.function.Parameter
import me.honkling.miniscript.parser.ast.operator
import me.honkling.miniscript.parser.ast.statement.Assignment
import me.honkling.miniscript.parser.ast.statement.Break
import me.honkling.miniscript.parser.ast.statement.Continue
import me.honkling.miniscript.parser.ast.statement.FunctionDeclaration
import me.honkling.miniscript.parser.ast.statement.If
import me.honkling.miniscript.parser.ast.statement.Loop
import me.honkling.miniscript.parser.ast.statement.Return
import me.honkling.miniscript.parser.ast.string.ComplexString
import me.honkling.miniscript.parser.ast.string.Component

class Parser(
    private val miniScript: MiniScript,
    private val stream: TokenStream
) {
    private val logger = stream.logger

    fun parse(): Block {
        return parseBlock(null, false)
    }

    fun parseBlock(parent: Node<*>?, expectBraces: Boolean = true): Block {
        var hasBraces = true

        if (expectBraces) {
            if (stream.peek().type != TokenType.OpenBrace)
                hasBraces = false
            else stream.consume()
        }

        val block = Block(miniScript, mutableListOf<Node<*>>(), parent)

        while (!stream.isEnd() && stream.peek().type != TokenType.CloseBrace) {
            try {
                block.statements += parseStatement(block)

                if (!hasBraces)
                    break
            } catch (_: MiniScriptException.ParseError) {
                recover()
            }
        }

        if (expectBraces && hasBraces)
            stream.consume().expect(TokenType.CloseBrace)

        return block
    }

    fun parseStatement(parent: Block): Node<*> {
        val token = stream.peek()

        return when (token.type) {
            TokenType.Native, TokenType.Function -> parseFunctionDeclaration(parent)
            TokenType.Identifier -> parseIdentifierExecutable(parent)
            TokenType.Return -> parseReturn(parent)
            TokenType.Continue -> parseContinue(parent)
            TokenType.Break -> parseBreak(parent)
            TokenType.ForEach -> parseForEach(parent)
            TokenType.While -> parseWhile(parent)
            TokenType.If -> parseIf(parent)
            else -> {
                logger.error("Expected a statement, found ${token.asString()}")
                throw MiniScriptException.ParseError()
            }
        }
    }

    fun parseIf(parent: Block): If {
        stream.consume().expect(TokenType.If)
        stream.consume().expect(TokenType.OpenParen)
        val expression = parseExpression(null)
        stream.consume().expect(TokenType.CloseParen)
        val block = parseBlock(null)
        val elseBlock = if (stream.peek().type == TokenType.Else) {
            stream.consume()
            parseBlock(null)
        } else null

        val statement = If(expression, block, elseBlock, parent)
        expression.parent = statement
        elseBlock?.parent = statement
        block.parent = statement
        return statement
    }

    fun parseWhile(parent: Block): Loop.While {
        stream.consume().expect(TokenType.While)
        stream.consume().expect(TokenType.OpenParen)
        val expression = parseExpression(null)
        stream.consume().expect(TokenType.CloseParen)
        val block = parseBlock(null)

        val statement = Loop.While(expression, block, parent)
        expression.parent = statement
        block.parent = statement
        return statement
    }

    fun parseIdentifierExecutable(parent: Block): Node<*> {
        val start = logger.location.clone()
        val reference = parseExpression(null)
        val token = stream.peek()

        return when (token.type) {
            TokenType.Assign, TokenType.PlusAssign,
             TokenType.MinusAssign, TokenType.MultiplyAssign,
             TokenType.DivideAssign, TokenType.Increment,
             TokenType.Decrement -> {
                 if (reference !is Assignable) {
                     logger.error("Left side cannot be assigned to", start)
                     throw MiniScriptException.ParseError()
                 }

                 parseAssignment(reference, parent)
            }
            else -> {
                reference.parent = parent
                return reference
            }
        }
    }

    fun parseExpression(parent: Node<*>?): Expression<*> {
        return parseArithmetic(parsePrimary(null), 0, parent)
    }

    fun parseArithmetic(lhs: Expression<*>, minPrecedence: Int, parent: Node<*>?): Expression<*> {
        var lhs = lhs
        var lookahead = stream.peek()
        var operatorInfo = operator(lookahead.type)

        while (operatorInfo != null && operatorInfo.precedence >= minPrecedence) {
            val operator = lookahead
            stream.consume()

            var rhs = parsePrimary(null)

            lookahead = stream.peek()
            var lookaheadOperator = operator(lookahead.type)
            var greaterPrecedence = lookaheadOperator != null && lookaheadOperator.precedence > operatorInfo.precedence

            while (lookaheadOperator != null && (greaterPrecedence || (lookaheadOperator.isRightAssociated
                    && lookaheadOperator.precedence == operatorInfo.precedence))) {
                rhs = parseArithmetic(rhs, operatorInfo.precedence + if (greaterPrecedence) 1 else 0, null)
                lookahead = stream.peek()
                lookaheadOperator = operator(lookahead.type)
                greaterPrecedence = lookaheadOperator != null && lookaheadOperator.precedence > operatorInfo.precedence
            }

            val oldLhs = lhs
            lhs = Arithmetic(oldLhs, rhs, operator(operator.type)!!, null)
            oldLhs.parent = lhs
            rhs.parent = lhs
            operatorInfo = operator(lookahead.type)
        }

        lhs.parent = parent
        return lhs
    }

    fun parsePrimary(parent: Node<*>?): Expression<*> {
        var expression = parseOneExpression(parent)

        while (true)
            when (stream.peek().type) {
                TokenType.OpenParen -> {
                    expression = parseFunctionCall(expression, parent)
                }
                TokenType.OpenBracket -> {
                    stream.consume()
                    val expr = parseExpression(null)
                    stream.consume().expect(TokenType.CloseBracket)
                    val oldRhs = expression
                    expression = Arithmetic(expression, expr, Operator.Period, null)
                    oldRhs.parent = expression
                    expr.parent = expression
                }
                TokenType.Period -> {
                    stream.consume()
                    val name = stream.consume().expect(TokenType.Identifier, "Expected member name, found {1}")
                    val expr = Value.MemberAccess(name.raw, null)
                    val oldRhs = expression
                    expression = Arithmetic(expression, expr, Operator.Period, null)
                    oldRhs.parent = expression
                    expr.parent = expression
                }
                else -> break
            }

        return expression
    }

    fun parseOneExpression(parent: Node<*>?): Expression<*> {
        if (stream.peek().type == TokenType.OpenParen) {
            stream.consume()
            val expr = parseExpression(parent)
            stream.consume().expect(TokenType.CloseParen)
            return expr
        }

        return parseValue(parent)
    }

    fun parseForEach(parent: Block): Loop.ForEach {
        stream.consume().expect(TokenType.ForEach)
        stream.consume().expect(TokenType.OpenParen)
        val name = stream.consume().expect(TokenType.Identifier, "Expected an identifier, found {1}")
        stream.consume().expect(TokenType.Colon)
        val expression = parseExpression(null)
        stream.consume().expect(TokenType.CloseParen)
        val block = parseBlock(null)

        val loop = Loop.ForEach(name.raw, expression, block, parent)
        expression.parent = loop
        block.parent = loop
        return loop
    }

    fun parseAssignment(reference: Assignable, parent: Block): Assignment {
        val operator = stream.consume()
        val value = if (operator.type == TokenType.Increment || operator.type == TokenType.Decrement) null
            else parseExpression(null)

        val assignment = Assignment(reference, value, operator.type, parent)
        (reference as Node<Node<*>?>).parent = assignment
        value?.parent = assignment
        return assignment
    }

    fun parseFunctionCall(expression: Expression<*>, parent: Node<*>?): FunctionCall {
        stream.consume().expect(TokenType.OpenParen)
        val arguments = mutableListOf<Expression<*>>()

        while (stream.peek().type != TokenType.CloseParen) {
            arguments += parseExpression(null)

            if (stream.peek().type != TokenType.CloseParen)
                stream.consume().expect(TokenType.Comma, "Expected ',' or ')', found {1}")
        }

        stream.consume().expect(TokenType.CloseParen)
        val function = if (stream.peek().type == TokenType.OpenBrace) {
            stream.consume()
            parseFunction(null)
        } else null

        val functionCall = FunctionCall(expression, arguments, function?.get(), parent)
        arguments.forEach { it.parent = functionCall }
        expression.parent = functionCall
        function?.parent = functionCall
        return functionCall
    }

    fun parseFunctionDeclaration(parent: Block): FunctionDeclaration {
        val isNative = stream.peek().type == TokenType.Native
        if (isNative)
            stream.consume()

        stream.consume().expect(TokenType.Function)

        val name = stream.consume().expect(TokenType.Identifier, "Expected a function name, found {1}")
        val parameters = mutableListOf<Parameter>()
        stream.consume().expect(TokenType.OpenParen)

        while (stream.peek().type != TokenType.CloseParen) {
            val name = stream.consume().expect(TokenType.Identifier, "Expected a parameter name or ')', found {1}")
            stream.consume().expect(TokenType.Colon)
            val type = parseType()
            val defaultValue = if (stream.peek().type == TokenType.Assign) {
                stream.consume()
                parseExpression(null)
            } else null

            if (stream.peek().type != TokenType.CloseParen)
                stream.consume().expect(TokenType.Comma, "Expected ',' or ')', found {1}")

            val parameter = Parameter(name.raw, type, defaultValue, null)
            (defaultValue as Node<Node<*>?>?)?.parent = parameter
            parameters += parameter
        }

        stream.consume().expect(TokenType.CloseParen)
        val returnType = if (stream.peek().type == TokenType.Arrow) {
            stream.consume()
            parseType()
        } else null

        val block = if (isNative) null else parseBlock(null)
        val function = Function(parameters, returnType, block, false, null)
        val statement = FunctionDeclaration(name.raw, function, parent)

        block?.parent = function
        function.parent = statement

        for (parameter in parameters)
            parameter.parent = function

        return statement
    }

    fun parseReturn(parent: Block): Return {
        stream.consume().expect(TokenType.Return)
        val expression = parseExpression(null)
        val statement = Return(expression, parent)
        expression.parent = statement
        return statement
    }

    fun parseContinue(parent: Block): Continue {
        stream.consume().expect(TokenType.Continue)
        return Continue(parent)
    }

    fun parseBreak(parent: Block): Break {
        stream.consume().expect(TokenType.Break)
        return Break(parent)
    }

    fun parseValue(parent: Node<*>?): Value<*> {
        val token = stream.consume()

        @Suppress("UNCHECKED_CAST")
        return when (token.type) {
            TokenType.Number -> Value.Number((token as Token.WithValue<Double>).value, parent)
            TokenType.Boolean -> Value.Boolean((token as Token.WithValue<Boolean>).value, parent)
            TokenType.String -> parseString(token as Token.WithValue<String>, parent)
            TokenType.Identifier -> Value.Variable(token.raw, parent)
            TokenType.OpenBracket -> parseArray(parent)
            TokenType.OpenBrace -> {
                val branch = stream.branch()
                val next = branch.consume()
                val nextNext = branch.peek()

                if ((next.type == TokenType.Identifier && nextNext.type == TokenType.Colon) || next.type == TokenType.CloseBrace)
                    parseDictionary(parent)
                else parseFunction(parent)
            }
            else -> {
                logger.error("Expected a value, found ${token.asString()}")
                throw MiniScriptException.ParseError()
            }
        }
    }

    fun parseDictionary(parent: Node<*>?): Value.Dictionary {
        val elements = mutableMapOf<String, Expression<*>>()

        while (stream.peek().type != TokenType.CloseBrace) {
            val name = stream.consume().expect(TokenType.Identifier, "Expected dictionary key, found {1}")
            stream.consume().expect(TokenType.Colon)
            val value = parseExpression(null)

            if (stream.peek().type != TokenType.CloseBrace)
                stream.consume().expect(TokenType.Comma, "Expected ',' or '}', found {1}")

            elements[name.raw] = value
        }

        stream.consume().expect(TokenType.CloseBrace)
        val dictionary = Value.Dictionary(elements, parent)

        for (element in elements.values)
            element.parent = dictionary

        return dictionary
    }

    fun parseFunction(parent: Node<*>?): Value.Function {
        val branch = stream.branch()
        var hasParameters = false
        var level = 0

        while (!branch.isEnd())
            when (branch.consume().type) {
                TokenType.OpenBrace -> level++
                TokenType.CloseBrace -> {
                    if (level > 0)
                        level--
                    else break
                }
                TokenType.Arrow -> {
                    hasParameters = true
                    break
                }
                else -> {}
            }

        val parameters = mutableListOf<Parameter>()
        if (hasParameters) {
            while (stream.peek().type != TokenType.Arrow) {
                val name = stream.consume().expect(TokenType.Identifier, "Expected a parameter name, '}', or '->'. Found {1}")
                val type = if (stream.peek().type == TokenType.Colon) {
                    stream.consume()
                    parseType()
                } else null

                val defaultValue = if (stream.peek().type == TokenType.Assign) {
                    stream.consume()
                    parseExpression(null)
                } else null

                val parameter = Parameter(name.raw, type, defaultValue, null)
                defaultValue?.parent = parameter
                parameters += parameter

                if (stream.peek().type != TokenType.Arrow)
                    stream.consume().expect(TokenType.Colon, "Expected ',' or '->', found {1}")
            }

            stream.consume().expect(TokenType.Arrow)
        }

        val block = parseBlock(null, false)
        stream.consume().expect(TokenType.CloseBrace)
        val function = Function(parameters, null, block, true, parent)
        val value = Value.Function(function, parent)
        parameters.forEach { it.parent = function }
        block.parent = function
        function.parent = value
        return value
    }

    fun parseArray(parent: Node<*>?): Value.Array {
        val elements = mutableListOf<Expression<*>>()

        while (stream.peek().type != TokenType.CloseBracket) {
            elements += parseExpression(null)

            if (stream.peek().type != TokenType.CloseBracket)
                stream.consume().expect(TokenType.Comma, "Expected ',' or ']', found {1}")
        }

        stream.consume().expect(TokenType.CloseBracket)
        val array = Value.Array(elements, parent)
        elements.forEach { it.parent = array }
        return array
    }

    fun parseString(token: Token.WithValue<String>, parent: Node<*>?): ComplexString {
        val raw = token.value
        val string = ComplexString(mutableListOf<Component>(), parent)
        val value = StringBuilder()
        var success = true
        var index = 0

        while (index < raw.length) {
            val character = raw[index++]

            if (character == '$') {
                val next = raw[index]

                if (value.isNotEmpty()) {
                    string.components += Component.Plain(value.toString(), string)
                    value.clear()
                }

                if (next == '{') {
                    var level = 0
                    index++

                    while (index < raw.length && (raw[index] != '}' || level > 0)) {
                        val character = raw[index++]
                        value.append(character)

                        level += if (character == '{') 1
                            else -1
                    }

                    if (raw[index++] != '}') {
                        logger.error("Expected '}', found nothing")
                        throw MiniScriptException.ParseError()
                    }

                    val location = logger.location
                    val diagnosticSize = logger.diagnostics.size
                    val lexer = Lexer(value.toString(), logger, location.clone(index = location.index))
                    val tokens = lexer.lex()

                    if (logger.diagnostics.size > diagnosticSize) {
                        success = false
                        value.clear()
                        continue
                    }

                    val parser = Parser(miniScript, TokenStream(tokens, logger, location.clone(index = location.index)))
                    val expression = parser.parseExpression(null)
                    val component = Component.Expression(expression, string)
                    (expression as Node<Node<*>?>).parent = component
                    string.components += component
                    value.clear()
                    continue
                }

                while (index < raw.length && ((raw[index].isLetter() && index == 0) || raw[index].isLetterOrDigit()))
                    value.append(raw[index++])

                if (value.isEmpty()) {
                    val found = if (index >= raw.length) "nothing" else "'${raw[index]}'"
                    logger.error("Expected a variable name, found $found")
                    success = false
                    continue
                }

                string.components += Component.Variable(value.toString(), string)
                value.clear()
                continue
            }

            value.append(character)
        }

        if (value.isNotEmpty())
            string.components += Component.Plain(value.toString(), string)

        if (success)
            return string

        throw MiniScriptException.ParseError()
    }

    fun parseType(): Type<*> {
        val token = stream.consume()
        val type = when (token.type) {
            TokenType.StringType -> Type.String
            TokenType.NumberType -> Type.Number
            TokenType.BooleanType -> Type.Boolean
            TokenType.DictType -> Type.Dictionary
            TokenType.VoidType -> Type.Void
            TokenType.AnyType -> Type.Any
            TokenType.OpenParen -> {
                var isFunction = false
                val parameters = mutableListOf<Parameter>()

                while (stream.peek().type != TokenType.CloseParen) {
                    val name = if (stream.peek().type == TokenType.Identifier) {
                        val name = stream.consume()
                        stream.consume().expect(TokenType.Colon)
                        isFunction = true
                        name
                    } else null

                    val type = parseType()
                    val defaultValue = if (stream.peek().type == TokenType.Assign) {
                        stream.consume()
                        isFunction = true
                        parseExpression(null)
                    } else null

                    if (stream.peek().type != TokenType.CloseParen) {
                        stream.consume().expect(TokenType.Comma, "Expected ',' or ')', found {1}")
                        isFunction = true
                    }

                    val parameter = Parameter(name?.raw, type, defaultValue, null)
                    (defaultValue as Node<Node<*>?>?)?.parent = parameter
                    parameters += parameter
                }

                stream.consume().expect(TokenType.CloseParen)

                if (stream.peek().type != TokenType.Arrow && !isFunction)
                    return parameters[0].type!!

                stream.consume().expect(TokenType.Arrow)

                val returnType = parseType()
                val function = Type.Function(parameters, returnType)

                for (parameter in parameters)
                    parameter.parent = function

                function
            }
            else -> {
                logger.error("Expected a type, found ${token.asString()}")
                throw MiniScriptException.ParseError()
            }
        }

        if (stream.peek().type == TokenType.OpenBracket) {
            stream.consume()
            stream.consume().expect(TokenType.CloseBracket)
            return Type.Array(type)
        }

        return type
    }

    fun recover() {
        val recoveryTokens = listOf(
            TokenType.EndOfInput,
            TokenType.Function,
//            TokenType.If,
//            TokenType.For,
//            TokenType.ForEach,
//            TokenType.While,
            TokenType.CloseBrace
        )

        while (!stream.isEnd() && stream.peek().type !in recoveryTokens) {
            val token = stream.peek()

            if (token.type == TokenType.Identifier) {
                val branch = stream.branch()
                branch.consume()

                if (branch.peek().type == TokenType.Assign)
                    break
            }

            stream.consume()
        }
    }
}