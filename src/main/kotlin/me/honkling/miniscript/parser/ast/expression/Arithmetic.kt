package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.Changer
import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.miniScript
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.pass.Pass
import kotlin.collections.get
import kotlin.reflect.KClass

class Arithmetic(
    val left: Expression<*>,
    val right: Expression<*>,
    val operator: Operator,
    location: Location,
    parent: Node<*>?
) : Expression<Any?>(location, parent), Assignable {
    override fun get(): Pair<Any?, ExecutionResult> {
        return getWithLeftSide().second to ExecutionResult.ContinueExecution
    }

    fun getWithLeftSide(): Pair<Any, Any?> {
        val block = getBlockParent() ?: throw MiniScriptException.RuntimeError("Couldn't find block", this)
        val left = this.left.get().first
            ?: throw MiniScriptException.RuntimeError("Expected value", this)

        if (operator == Operator.And) {
            if (left !is Boolean)
                throw MiniScriptException.RuntimeError("Expected boolean for AND comparison", this)

            if (!left)
                return left to false
        } else if (operator == Operator.Or) {
            if (left !is Boolean)
                throw MiniScriptException.RuntimeError("Expected boolean for OR comparison", this)

            if (left)
                return left to true
        }

        val right = this.right.get().first
            ?: throw MiniScriptException.RuntimeError("Expected value", this)

        return left to evaluateArithmetic(block.miniScript, this, left, right, operator)
    }

    override fun canAssign(): Boolean {
        return operator == Operator.Period
    }

    override fun set(operator: TokenType, value: Any?) {
        val left = left.get().first
        val right = right.get().first

        if (left is MutableList<*>) {
            if (right !is Double)
                throw MiniScriptException.RuntimeError("Expected integer index for array", this)

            left as MutableList<Any?>
            left[right.toInt()] = value
        } else if (left is MutableMap<*, *>) {
            left as MutableMap<Any?, Any?>
            left[right] = value
        } else if (left is ClassInstance)
            left.fields[right] = value
        else if (left is ClassInstance.SuperInstance)
            left.instance.fields[right] = value
    }

    override fun accept(pass: Pass) {
        pass.visitArithmetic(this)
    }
}

fun evaluateArithmetic(miniScript: MiniScript, node: Arithmetic, left: Any?, right: Any?, operator: Operator): Any? {
    val leftClass = left?.let { it::class } ?: Any::class
    val rightClass = right?.let { it::class } ?: Any::class

    val changers = tryGetChangers(miniScript, leftClass, rightClass, operator)
        ?: throw MiniScriptException.RuntimeError("Invalid operator $operator for '$left' and '$right'", node)

    for (changer in changers) {
        try {
            return changer.block(left, right, operator)
        } catch (_: MiniScriptException.WrongChanger) {}
    }

    return null
}

fun tryGetChangers(miniScript: MiniScript, leftClass: KClass<*>, rightClass: KClass<*>, op: Operator, tryVariant: Boolean = true): List<Changer<*>>? {
    val changers = miniScript.changers

    changers[leftClass]?.get(rightClass)
        ?.filter { op in it.validOperators }
        ?.let { return it }

    if (tryVariant)
        return tryGetChangers(miniScript, rightClass, leftClass, op, false)
            ?: tryGetChangers(miniScript, Any::class, rightClass, op, false)
            ?: tryGetChangers(miniScript, rightClass, Any::class, op, false)
            ?: tryGetChangers(miniScript, leftClass, Any::class, op, false)
            ?: tryGetChangers(miniScript, Any::class, leftClass, op, false)
            ?: tryGetChangers(miniScript, Any::class, Any::class, op, false)

    return null
}