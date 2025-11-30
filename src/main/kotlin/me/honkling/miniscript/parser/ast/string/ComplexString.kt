package me.honkling.miniscript.parser.ast.string

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Value
import me.honkling.miniscript.parser.ast.statement.ExecutionResult

class ComplexString(val components: MutableList<Component>, parent: Node<*>?) : Value<String>(parent) {
    override fun get(): Pair<String, ExecutionResult> {
        val builder = StringBuilder()

        for (component in components)
            builder.append(component.get().first)

        return builder.toString() to ExecutionResult.ContinueExecution
    }
}