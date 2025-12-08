package me.honkling.miniscript

import me.honkling.miniscript.diagnostic.DiagnosticType
import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.Logger
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.lexer.Lexer
import me.honkling.miniscript.lexer.TokenStream
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.parser.Parser
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.stdlib.registerChangers
import me.honkling.miniscript.stdlib.registerStdlibArrays
import me.honkling.miniscript.stdlib.registerStdlibLocale
import me.honkling.miniscript.stdlib.registerStdlibLogging
import me.honkling.miniscript.stdlib.registerStdlibLoops
import me.honkling.miniscript.stdlib.registerStringHandlers
import me.honkling.miniscript.parser.ast.prototype.Class as MSClass
import java.io.File
import kotlin.reflect.KClass

typealias StringHandlerBlock<T> = (value: T) -> String
typealias ChangerBlock<F, S, R> = (lhs: F, rhs: S, op: Operator) -> R
data class Changer<R>(
    val validOperators: Set<Operator>,
    val priority: Int,
    val block: ChangerBlock<Any?, Any?, R>
)

class MiniScript internal constructor(configuration: MiniScriptConfiguration) {
    val hasStandardLibrary = configuration.hasStandardLibrary
    val environment = Environment(this)
    val changers = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableList<Changer<*>>>>()
    val stringHandlers = mutableMapOf<KClass<*>, StringHandlerBlock<*>>()

    init {
        if (hasStandardLibrary) {
//            evaluateResource("stdlib/files.mini")
            evaluateResource("stdlib/arrays.mini", ::registerStdlibArrays)
            evaluateResource("stdlib/strings.mini")
            evaluateResource("stdlib/logging.mini", ::registerStdlibLogging)
            evaluateResource("stdlib/loops.mini", ::registerStdlibLoops)
            evaluateResource("stdlib/locale.mini", ::registerStdlibLocale)
            environment.resolveReferences()

            registerChangers(this)
            registerStringHandlers(this)
        }
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
        classOne: MSClass,
        classTwo: MSClass,
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
        `class`: MSClass,
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

    protected fun evaluateResource(name: String, registrar: (MiniScript, SymbolTable) -> Unit = { _, _ -> }) {
        val resource = MiniScript::class.java.getResource("/$name")?.toURI()
            ?: throw IllegalStateException("Couldn't find resource '$name'")

        val file = File(resource)
        evaluateInternal(file, true)
        registrar(this, environment.lastFrame.symbolTable)
    }

    private fun evaluateInternal(file: File, internalMode: Boolean) {
        val location = Location()
        val logger = Logger(location)
        val lexer = Lexer(file.readText(), logger, internalMode = internalMode)
        val tokens = lexer.lex().filter { it.type != TokenType.Whitespace }

        location.reset()
        val parser = Parser(this, TokenStream(tokens, logger, location))
        val block = parser.parse()
        block.parent = environment

        val diagnostics = logger.diagnostics.filter { it.type.ordinal > DiagnosticType.Info.ordinal }
        val diagnosticSize = diagnostics.size
        val word = if (diagnosticSize == 1) "diagnostic" else "diagnostics"

        val errorCount = logger.diagnostics.count { it.type == DiagnosticType.Error }
        if (!internalMode || errorCount > 0) {
            println(if (internalMode)
                "Found $diagnosticSize $word while initalizing MiniScript's standard library(${file.name})"
            else "Found $diagnosticSize $word.")

            for (diagnostic in logger.diagnostics) {
                val location = diagnostic.location
                println("${diagnostic.type.name.lowercase()} (${location.line}:${location.column}): ${diagnostic.message}")
            }
        }

        if (diagnosticSize > 0)
            return

//        if (internalMode) {
//            environment.statements.clear()
//            environment.statements += block.statements
//            block.statements.forEach {
//                if (it is Statement)
//                    it.parent = environment
//                else if (it is Expression<*>)
//                    it.parent = environment
//            }
//        }

        try {
            block.execute(!internalMode)
        } catch (exception: MiniScriptException.RuntimeError) {
            val location = exception.node?.location?.let { "(${it.line}:${it.column})" }
            System.err.println("runtime error$location: ${exception.message}")
            throw exception
        }
    }

    fun evaluate(file: File) {
        evaluateInternal(file, false)
    }
}

class MiniScriptConfiguration {
    var hasStandardLibrary = true
}

fun miniScript(block: MiniScriptConfiguration.() -> Unit = {}): MiniScript {
    val configurator = MiniScriptConfiguration()
    block(configurator)
    return MiniScript(configurator)
}