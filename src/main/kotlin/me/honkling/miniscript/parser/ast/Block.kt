package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.stack.Frame
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.parser.ast.statement.Statement
import me.honkling.miniscript.pass.Pass

open class Block(
    val miniScript: MiniScript,
    val statements: MutableList<Node<*>>,
    parent: Node<*>?,
) : Node<Node<*>?>(parent) {
    var returnValue: Any? = null

    fun execute(pushStack: Boolean = true): Pair<Any?, ExecutionResult> {
        if (pushStack) pushToStack()
        returnValue = null

        for ((index, statement) in statements.withIndex()) {
            val isLast = index + 1 == statements.size

            when (statement) {
                is Expression<*> -> {
                    val result = statement.get()

                    if (isLast)
                        return result
                }
                is Statement -> {
                    val result = statement.execute()

                    if (result != ExecutionResult.ContinueExecution) {
                        if (pushStack) popFromStack()
                        return null to result
                    }
                }
                else -> throw MiniScriptException.RuntimeError("Expected a statement or expression in block")
            }
        }

        if (pushStack) popFromStack()
        return null to ExecutionResult.ContinueExecution
    }

    fun pushToStack(): Frame {
        val frame = Frame()
        miniScript.environment.executionStack += frame
        return frame
    }

    fun popFromStack() {
        miniScript.environment.executionStack.removeLast()
    }

    override fun accept(pass: Pass) {
        pass.visitBlock(this)
    }
}

class SymbolTable : HashMap<String, Any?>()

interface SymbolHolder {
    val symbolTable: SymbolTable
}