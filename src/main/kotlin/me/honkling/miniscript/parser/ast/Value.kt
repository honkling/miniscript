package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.parser.ast.expression.Assignable
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.expression.NativeExpression
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.prototype.FunctionReference
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.parser.ast.statement.NativeStatement
import me.honkling.miniscript.pass.Pass
import me.honkling.miniscript.stdlib.tryGetChangers
import me.honkling.miniscript.parser.ast.function.Function as MSFunction

abstract class Value<T>(location: Location, parent: Node<*>?) : Expression<T>(location, parent) {
    open class Simple<T : Any>(private val value: T, location: Location, parent: Node<*>?) : Value<T>(location, parent) {
        override fun get() = value to ExecutionResult.ContinueExecution
    }

    class Character(value: Char, location: Location, parent: Node<*>?) : Simple<Char>(value, location, parent)
    class Number(value: Double, location: Location, parent: Node<*>?) : Simple<Double>(value,  location, parent)
    class Boolean(value: kotlin.Boolean, location: Location, parent: Node<*>?) : Simple<kotlin.Boolean>(value,  location, parent)
    class Function(value: MSFunction, location: Location, parent: Node<*>?) : Simple<MSFunction>(value, location, parent)
    class MemberAccess(value: String, location: Location, parent: Node<*>?) : Simple<String>(value, location, parent)
    class Spread(val expression: Expression<*>, location: Location, parent: Node<*>?) : Value<ClassInstance>(location, parent) {
        override fun get(): Pair<ClassInstance, ExecutionResult> {
            val result = expression.get()
            val list = result.first as? ClassInstance
                ?: throw MiniScriptException.RuntimeError("Can't spread a non-list", this)

            if (list.classRef != expression.getBlockParent()!!.miniScript.environment.arrayClass)
                throw MiniScriptException.RuntimeError("Can't spread a non-list", this)

            return list to result.second
        }
    }

    class Super(location: Location, parent: Node<*>?) : Value<ClassInstance.SuperInstance?>(location, parent) {
        override fun get(): Pair<ClassInstance.SuperInstance?, ExecutionResult> {
            val thisRef = parent?.getSymbol("this") as? ClassInstance
                ?: return null to ExecutionResult.ContinueExecution

            return ClassInstance.SuperInstance(thisRef) to ExecutionResult.ContinueExecution
        }
    }

    class Variable(private val value: String, location: Location, parent: Node<*>?) : Value<Any?>(location, parent), Assignable {
        override fun get(): Pair<Any?, ExecutionResult> {
            val value = parent?.getSymbol(value)

            if (value is MSFunction)
                return FunctionReference(null, value) to ExecutionResult.ContinueExecution

            return value to ExecutionResult.ContinueExecution
        }

        override fun canAssign(): kotlin.Boolean {
            return true
        }

        override fun set(type: TokenType, value: Any?) {
            val miniScript = getBlockParent()!!.miniScript
            val currentValue = get().first

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
                        else -> throw IllegalStateException("Invalid operator for variable")
                    }

                    val value = value
                        ?: 1.0

                    val changers = tryGetChangers(miniScript, currentValue!!, value, currentValue::class, value::class, operator)
                        ?: throw MiniScriptException.RuntimeError("Couldn't find changer", this)

                    for (changer in changers) {
                        try {
                            val newValue = changer.block(currentValue, value, operator)
                            parent?.setSymbol(this.value, newValue)
                        } catch (_: MiniScriptException.WrongChanger) {}
                    }
                }
                else -> throw MiniScriptException.RuntimeError("Invalid operator ${type.name}", this)
            }
        }
    }

    class Array(private val elements: List<Expression<*>>, location: Location, parent: Node<*>?) : Value<ClassInstance>(location, parent) {
        override fun get(): Pair<ClassInstance, ExecutionResult> {
            val values = mutableListOf<Any?>()

            for (element in elements)
                element.get().first?.let { values += it }

            val arrayClass = getBlockParent()!!.miniScript.environment.arrayClass
            return arrayClass.instantiate(values) to ExecutionResult.ContinueExecution
        }
    }

    class Dictionary(private val elements: Map<String, Expression<*>>, location: Location, parent: Node<*>?) : Value<MutableMap<Any?, Any?>>(location, parent) {
        override fun get(): Pair<MutableMap<Any?, Any?>, ExecutionResult> {
            val values = mutableMapOf<Any?, Any?>()

            for ((key, value) in elements)
                value.get().first?.let { values[key] = it }

            return values to ExecutionResult.ContinueExecution
        }
    }

    override fun accept(pass: Pass) {
        pass.visitValue(this)
    }
}