package me.honkling.miniscript.parser.ast.string

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Value

class ComplexString(val components: MutableList<Component>, parent: Node<*>?) : Value<String>(parent) {
    override fun get(): String {
        val builder = StringBuilder()

        for (component in components)
            builder.append(component.get())

        return builder.toString()
    }
}