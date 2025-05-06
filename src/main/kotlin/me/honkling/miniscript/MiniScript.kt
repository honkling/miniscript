package me.honkling.miniscript

import me.honkling.miniscript.diagnostic.DiagnosticType
import me.honkling.miniscript.diagnostic.Location
import me.honkling.miniscript.diagnostic.Logger
import me.honkling.miniscript.lexer.Lexer
import me.honkling.miniscript.lexer.TokenStream
import me.honkling.miniscript.lexer.TokenType
import me.honkling.miniscript.parser.Parser
import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.expression.Expression
import me.honkling.miniscript.parser.ast.statement.Statement
import me.honkling.miniscript.pass.Execution
import me.honkling.miniscript.pass.PassManager
import me.honkling.miniscript.stdlib.registerChangers
import me.honkling.miniscript.stdlib.registerStdlibLogging
import me.honkling.miniscript.stdlib.registerStdlibLoops
import java.io.File
import kotlin.reflect.KClass

typealias ChangerBlock<F, S, R> = (lhs: F, rhs: S, op: Operator) -> R
data class Changer<F, S, R>(
    val validOperators: Set<Operator>,
    val block: ChangerBlock<Any?, Any?, R>
)

class MiniScript internal constructor(
    val hasStandardLibrary: Boolean
) {
    val environment = Block(this, mutableListOf(), null)
    val changers = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableList<Changer<*, *, *>>>>()

    init {
        registerChangers(this)

        if (hasStandardLibrary) {
            evaluateResource("stdlib/logging.mini", ::registerStdlibLogging)
            evaluateResource("stdlib/loops.mini", ::registerStdlibLoops)
        }
    }

    inline fun <reified F, reified S, reified R> registerChanger(
        vararg validOperators: Operator,
        noinline block: ChangerBlock<F, S, R>
    ) {
        val first = changers.getOrPut(F::class, ::mutableMapOf)
        val second = first.getOrPut(S::class, ::mutableListOf)
        second += Changer<F, S, R>(validOperators.toSet()) { lhs, rhs, op ->
            block(lhs as F, rhs as S, op)
        }
    }

    private fun evaluateResource(name: String, registrar: (MiniScript, SymbolTable) -> Unit) {
        val resource = MiniScript::class.java.getResource("/$name")?.toURI()
            ?: throw IllegalStateException("Couldn't find resource '$name'")

        val file = File(resource)
        evaluateInternal(file, true)
        registrar(this, environment.symbolTable)
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

        if (internalMode) {
            environment.statements.clear()
            environment.statements += block.statements
            block.statements.forEach {
                if (it is Statement)
                    it.parent = environment
                else if (it is Expression<*>)
                    it.parent = environment
            }
        }

        val passManager = PassManager()
        passManager.register(Execution())
        passManager.accept(if (internalMode) environment else block)
    }

    fun evaluate(file: File) {
        evaluateInternal(file, false)
    }
}

fun miniScript(): MiniScript {
    return MiniScript(true)
}