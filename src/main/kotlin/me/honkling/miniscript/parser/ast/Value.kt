package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.parser.ast.expression.Assignable
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.expression.tryGetChanger
import me.honkling.miniscript.parser.ast.function.Function as MSFunction
import me.honkling.miniscript.pass.Pass

abstract class Value<T>(parent: Node<*>?) : Expression<T>(parent) {
    open class Simple<T : Any>(private val value: T, parent: Node<*>?) : Value<T>(parent) {
        override fun get() = value
    }

    class Number(value: Double, parent: Node<*>?) : Simple<Double>(value,  parent)
    class Boolean(value: kotlin.Boolean, parent: Node<*>?) : Simple<kotlin.Boolean>(value,  parent)
    class Function(value: MSFunction, parent: Node<*>?) : Simple<MSFunction>(value, parent)
    class MemberAccess(value: String, parent: Node<*>?) : Simple<String>(value, parent)
    class Variable(private val value: String, parent: Node<*>?) : Value<Any?>(parent), Assignable {
        override fun get(): Any? {
            return parent?.getSymbol(value)
        }

        override fun canAssign(): kotlin.Boolean {
            return true
        }

        override fun set(type: TokenType, value: Any?) {
            val miniScript = getBlockParent()!!.miniScript
            val currentValue = get()

            when (type) {
                TokenType.Assign -> parent?.setSymbol(this.value, value)
                TokenType.PlusAssign, TokenType.MinusAssign,
                TokenType.MultiplyAssign, TokenType.DivideAssign,
                TokenType.Increment, TokenType.Decrement -> {
                    val operator = when (type) {
                        TokenType.PlusAssign, TokenType.Increment -> Operator.Plus
                        TokenType.MinusAssign, TokenType.Decrement -> Operator.Minus
                        TokenType.MultiplyAssign -> Operator.Multiply
                        TokenType.DivideAssign -> Operator.Divide
                        else -> throw MiniScriptException.RuntimeError()
                    }

                    val value = value
                        ?: 1.0

                    val changer = tryGetChanger(miniScript, currentValue!!::class, value::class, operator)
                        ?: throw MiniScriptException.RuntimeError("Couldn't find changer")

                    val newValue = changer.block(currentValue, value, operator)
                    parent?.setSymbol(this.value, newValue)
                }
                else -> throw MiniScriptException.RuntimeError("Invalid operator ${type.name}")
            }
        }
    }

    class Array(private val elements: List<Expression<*>>, parent: Node<*>?) : Value<List<Any?>>(parent) {
        override fun get(): List<Any?> {
            val values = mutableListOf<Any?>()

            for (element in elements)
                element.get()?.let { values += it }

            return values
        }
    }

    class Dictionary(private val elements: Map<String, Expression<*>>, parent: Node<*>?) : Value<MutableMap<Any?, Any?>>(parent) {
        override fun get(): MutableMap<Any?, Any?> {
            val values = mutableMapOf<Any?, Any?>()

            for ((key, value) in elements)
                value.get()?.let { values[key] = it }

            return values
        }
    }

    override fun accept(pass: Pass) {
        pass.visitValue(this)
    }
}