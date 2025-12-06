package me.honkling.miniscript.parser.ast.statement

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.pass.Pass

class FunctionDeclaration(
    val function: Function,
    location: Location,
    parent: Block
) : Statement(location, parent) {
    override fun execute(): ExecutionResult {
        parent.miniScript.environment.lastFrame.symbolTable[function.name!!] = function
        return ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitFunctionDeclaration(this)
    }
}