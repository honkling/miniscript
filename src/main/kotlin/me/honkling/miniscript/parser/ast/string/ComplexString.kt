package me.honkling.miniscript.parser.ast.string

import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Value
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.expression.NativeExpression
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.statement.ExecutionResult

class ComplexString(val components: MutableList<Component>, location: Location, parent: Node<*>?) : Value<ClassInstance>(location, parent) {
    override fun get(): Pair<ClassInstance, ExecutionResult> {
        val builder = StringBuilder()

        for (component in components)
            builder.append(component.get().first)

        val environment = getBlockParent()!!.miniScript.environment
        val stringClass = environment.stringClass
        val arrayClass = environment.arrayClass

        val array = arrayClass.instantiate(builder.toString().toMutableList())
        return stringClass.instantiate(array) to ExecutionResult.ContinueExecution
    }
}