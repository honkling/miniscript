package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.pass.Pass

class ClassDeclaration(val `class`: Class, val superClass: Expression<*>?, parent: Node<*>?) : Expression<Class>(parent) {
    override fun get(): Class {
        if (superClass != null) {
            val value = superClass.get()

            if (value !is Class)
                throw MiniScriptException.RuntimeError("Expected super class")

            `class`.superClass = value
        }

        if (parent is Block && `class`.name != null)
            (parent as Block).miniScript.environment.lastFrame.symbolTable[`class`.name] = `class`

        return `class`
    }

    override fun accept(pass: Pass) {
        pass.visitClassDeclaration(this)
    }
}