package me.honkling.miniscript.parser.ast.stack

import me.honkling.miniscript.Changer
import me.honkling.miniscript.ChangerBlock
import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.StringHandlerBlock
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.pass.Pass
import kotlin.collections.set
import kotlin.reflect.KClass

class Environment(val miniScript: MiniScript) : Node<Nothing?>(null, null) {
    val executionStack = mutableListOf(Frame())
    val lastFrame get() = executionStack.last()

    val changers = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableList<Changer<*>>>>()
    val stringHandlers = mutableMapOf<KClass<*>, StringHandlerBlock<*>>()
    lateinit var stringClass: Class; private set

    fun resolveReferences() {
        val symbolTable = executionStack[0].symbolTable
        stringClass = symbolTable["String"] as Class
    }

    inline fun <reified F, reified S, reified R> registerChanger(
        vararg validOperators: Operator,
        priority: Int = 0,
        noinline block: ChangerBlock<Any?, Any?, R>
    ) {
        val first = changers.getOrPut(F::class, ::mutableMapOf)
        val second = first.getOrPut(S::class, ::mutableListOf)
        second += Changer(validOperators.toSet(), priority, block)
    }

    inline fun <reified R> registerClassChanger(
        classOne: Class,
        classTwo: Class,
        vararg validOperators: Operator,
        priority: Int = 0,
        noinline block: ChangerBlock<ClassInstance, ClassInstance, R>
    ) {
        val first = changers.getOrPut(ClassInstance::class, ::mutableMapOf)
        val second = first.getOrPut(ClassInstance::class, ::mutableListOf)
        second += Changer(validOperators.toSet(), priority) { lhs, rhs, op ->
            lhs as ClassInstance
            rhs as ClassInstance

            if ((lhs.classRef != classOne && lhs.classRef != classTwo) || (rhs.classRef != classOne && rhs.classRef != classTwo))
                throw MiniScriptException.WrongChanger()

            block(lhs, rhs, op)
        }
    }

    inline fun <reified O, reified R> registerClassChanger(
        `class`: Class,
        vararg validOperators: Operator,
        priority: Int = 0,
        noinline block: ChangerBlock<Any?, Any?, R>
    ) {
        val first = changers.getOrPut(ClassInstance::class, ::mutableMapOf)
        val second = first.getOrPut(O::class, ::mutableListOf)
        second += Changer(validOperators.toSet(), priority) { lhs, rhs, op ->
            val classInstance = lhs as? ClassInstance ?: rhs as ClassInstance

            if (classInstance.classRef != `class`)
                throw MiniScriptException.WrongChanger()

            block(lhs, rhs, op)
        }
    }

    inline fun <reified T : Any> registerStringHandler(noinline block: StringHandlerBlock<T>) {
        stringHandlers[T::class] = block
    }

    fun <T> stringifyValue(value: T): String {
        if (value == null)
            return "null"

        val stringHandler = stringHandlers[value::class] as StringHandlerBlock<T>?
            ?: return value.toString()

        return stringHandler.invoke(value)
    }

    override fun accept(pass: Pass) {
        pass.visitEnvironment(this)
    }
}