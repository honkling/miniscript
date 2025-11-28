package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.Changer
import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.miniScript
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.pass.Pass
import kotlin.collections.get
import kotlin.reflect.KClass

class Arithmetic(
    val left: Expression<*>,
    val right: Expression<*>,
    val operator: Operator,
    parent: Node<*>?
) : Expression<Any?>(parent), Assignable {
    override fun get(): Any? {
        return getWithLeftSide().second
    }

    fun getWithLeftSide(): Pair<Any, Any?> {
        val block = getBlockParent() ?: throw MiniScriptException.RuntimeError("Couldn't find block")
        val left = this.left.get()
            ?: throw MiniScriptException.RuntimeError("Expected value")

        if (operator == Operator.And) {
            if (left !is Boolean)
                throw MiniScriptException.RuntimeError("Expected boolean for AND comparison")

            if (left == false)
                return left to false
        } else if (operator == Operator.Or) {
            if (left !is Boolean)
                throw MiniScriptException.RuntimeError("Expected boolean for OR comparison")

            if (left == true)
                return left to true
        }

        val right = this.right.get() ?: throw MiniScriptException.RuntimeError("Expected value")

        return left to evaluateArithmetic(block.miniScript, left, right, operator)
    }

    override fun canAssign(): Boolean {
        return operator == Operator.Period
    }

    override fun set(operator: TokenType, value: Any?) {
        val left = left.get()
        val right = right.get()

        if (left is MutableList<*>) {
            if (right !is Double)
                throw MiniScriptException.RuntimeError("Expected integer index for array")

            left as MutableList<Any?>
            left[right.toInt()] = value
        } else if (left is MutableMap<*, *>) {
            left as MutableMap<Any?, Any?>
            left[right] = value
        }
    }

    override fun accept(pass: Pass) {
        pass.visitArithmetic(this)
    }
}

fun evaluateArithmetic(miniScript: MiniScript, left: Any?, right: Any?, operator: Operator): Any? {
    val leftClass = left?.let { it::class } ?: Any::class
    val rightClass = right?.let { it::class } ?: Any::class

    val changer = tryGetChanger(miniScript, leftClass, rightClass, operator)
        ?: throw MiniScriptException.RuntimeError("Invalid operator $operator for '$left' and '$right'")

    return changer.block(left, right, operator)
}

fun tryGetChanger(miniScript: MiniScript, leftClass: KClass<*>, rightClass: KClass<*>, op: Operator, tryVariant: Boolean = true): Changer<*>? {
    val changers = miniScript.changers

    changers[leftClass]?.get(rightClass)
        ?.find { op in it.validOperators }
        ?.let { return it }

    if (tryVariant)
        return tryGetChanger(miniScript, rightClass, leftClass, op, false)
            ?: tryGetChanger(miniScript, Any::class, rightClass, op, false)
            ?: tryGetChanger(miniScript, rightClass, Any::class, op, false)
            ?: tryGetChanger(miniScript, leftClass, Any::class, op, false)
            ?: tryGetChanger(miniScript, Any::class, leftClass, op, false)
            ?: tryGetChanger(miniScript, Any::class, Any::class, op, false)

    return null
}