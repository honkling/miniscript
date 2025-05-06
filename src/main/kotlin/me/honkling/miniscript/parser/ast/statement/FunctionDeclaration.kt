package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.pass.Pass

class FunctionDeclaration(
    val name: String,
    val function: Function,
    parent: Block
) : Statement(parent) {
    override fun execute(): ExecutionResult {
        parent.symbolTable[name] = function
        return ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitFunctionDeclaration(this)
    }
}