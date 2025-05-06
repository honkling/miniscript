package me.honkling.miniscript.parser.ast

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.parser.ast.statement.Statement
import me.honkling.miniscript.pass.Pass

class Block(
    val miniScript: MiniScript,
    val statements: MutableList<Node<*>>,
    parent: Node<*>?
) : Node<Node<*>?>(parent) {
    val symbolTable = SymbolTable()
    var returnValue: Any? = null

    fun execute(): ExecutionResult {
        returnValue = null

        for (statement in statements) {
            when (statement) {
                is Expression<*> -> statement.get()
                is Statement -> {
                    val result = statement.execute()

                    if (result != ExecutionResult.ContinueExecution)
                        return result
                }
                else -> throw MiniScriptException.RuntimeError("Expected a statement or expression in block")
            }
        }

        return ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitBlock(this)
    }
}

class SymbolTable : HashMap<String, Any?>()