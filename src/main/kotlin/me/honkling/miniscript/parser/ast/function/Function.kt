package me.honkling.miniscript.parser.ast.function

import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.pass.Pass
import kotlin.collections.set

class Function(
    val name: String?,
    val parameters: MutableList<Parameter>,
    val returnType: Type<*>?,
    var block: Block?,
    val isLambda: Boolean,
    parent: Node<*>?
) : Node<Node<*>?>(parent) {
    fun call(vararg arguments: Any, lambda: Function? = null, symbols: Map<String, Any?> = emptyMap()): Any? {
        val parameterSize = parameters.size
        val argumentSize = arguments.size + if (lambda == null) 0 else 1
        if ((!isLambda && parameterSize != argumentSize) || parameterSize > argumentSize) {
            val form = if (parameterSize == 1) "argument" else "s"
            throw MiniScriptException.RuntimeError("Expected $parameterSize $form, found $argumentSize")
        }

        val frame = block!!.pushToStack()
        val oldArguments = mutableListOf<Any?>()

        if (isLambda && parameters.isEmpty() && arguments.isNotEmpty())
            frame.symbolTable["it"] = arguments[0]
        else for ((index, argument) in arguments.withIndex()) {
            val parameter = parameters[index]
            val oldValue = frame.symbolTable[parameter.name!!]
            frame.symbolTable[parameter.name] = argument
            oldArguments += oldValue
        }

        if (lambda != null) {
            val blockArgument = parameters.lastOrNull { it.type is Type.Function }
                ?: throw MiniScriptException.RuntimeError("Passed block to function without a block parameter")

            frame.symbolTable[blockArgument.name!!] = lambda
        }

        for ((key, value) in symbols)
            frame.symbolTable[key] = value

        block!!.execute(false)

//        for ((index, argument) in oldArguments.withIndex()) {
//            val parameter = parameters[index]
//            frame.symbolTable[parameter.name!!] = argument
//        }

        block!!.popFromStack()
        return block!!.returnValue
    }

    override fun accept(pass: Pass) {
        pass.visitFunction(this)
    }
}