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
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.stdlib.*
import java.io.File

typealias StringHandlerBlock<T> = (value: T) -> String
typealias ChangerBlock<F, S, R> = (lhs: F, rhs: S, op: Operator) -> R
data class Changer<R>(
    val validOperators: Set<Operator>,
    val priority: Int,
    val leftClass: Class?,
    val rightClass: Class?,
    val block: ChangerBlock<Any?, Any?, R>
)

class MiniScript internal constructor(configuration: MiniScriptConfiguration) {
    val hasStandardLibrary = configuration.hasStandardLibrary
    val environment = Environment(this)


    init {
        if (hasStandardLibrary) {
            registerStdlibArrays(environment)
            evaluateResource("stdlib/strings.mini")
            evaluateResource("stdlib/logging.mini", ::registerStdlibLogging)
            evaluateResource("stdlib/loops.mini", ::registerStdlibLoops)
            evaluateResource("stdlib/locale.mini", ::registerStdlibLocale)
            evaluateResource("stdlib/files.mini", ::registerStdlibFiles)
            environment.resolveReferences()

            registerChangers(environment)
            registerStringHandlers(environment)
        }
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