package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.pass.Pass

class ClassDeclaration(val `class`: Class, val superClass: Expression<*>?, location: Location, parent: Node<*>?) : Expression<Class>(location, parent) {
    override fun get(): Pair<Class, ExecutionResult> {
        if (superClass != null) {
            val value = superClass.get().first

            if (value !is Class)
                throw MiniScriptException.RuntimeError("Expected super class", this)

            `class`.superClass = value
        }

        if (`class`.name != null)
            getBlockParent()?.miniScript?.environment?.lastFrame?.symbolTable[`class`.name] = `class`

        return `class` to ExecutionResult.ContinueExecution
    }

    override fun accept(pass: Pass) {
        pass.visitClassDeclaration(this)
    }
}