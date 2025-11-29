package me.honkling.miniscript

import me.honkling.miniscript.diagnostic.DiagnosticType
import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.Logger
import me.honkling.miniscript.lexer.Lexer
import me.honkling.miniscript.lexer.TokenStream
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.parser.Parser
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.stdlib.registerChangers
import me.honkling.miniscript.stdlib.registerStdlibLocale
import me.honkling.miniscript.stdlib.registerStdlibLogging
import me.honkling.miniscript.stdlib.registerStdlibLoops
import java.io.File
import kotlin.reflect.KClass

typealias ChangerBlock<F, S, R> = (lhs: F, rhs: S, op: Operator) -> R
data class Changer<R>(
    val validOperators: Set<Operator>,
    val block: ChangerBlock<Any?, Any?, R>
)

class MiniScript internal constructor(configuration: MiniScriptConfiguration) {
    val hasStandardLibrary = configuration.hasStandardLibrary
    val environment = Environment(this)
    val changers = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableList<Changer<*>>>>()

    init {
        registerChangers(this)

        if (hasStandardLibrary) {
//            evaluateResource("stdlib/files.mini")
            evaluateResource("stdlib/logging.mini", ::registerStdlibLogging)
            evaluateResource("stdlib/loops.mini", ::registerStdlibLoops)
            evaluateResource("stdlib/locale.mini", ::registerStdlibLocale)
        }
    }

    inline fun <reified F, reified S, reified R> registerChanger(
        vararg validOperators: Operator,
        noinline block: ChangerBlock<Any?, Any?, R>
    ) {
        val first = changers.getOrPut(F::class, ::mutableMapOf)
        val second = first.getOrPut(S::class, ::mutableListOf)
        second += Changer<R>(validOperators.toSet(), block)
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

        if (!internalMode) {
            println("Found $diagnosticSize $word.")

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

        block.execute(!internalMode)
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