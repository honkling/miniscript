package me.honkling.miniscript.stdlib

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.statement.nativeBlock

fun registerStdlibLoops(miniScript: MiniScript, symbols: SymbolTable) {
    val repeat = symbols["repeat"] as Function
    repeat.block = nativeBlock(miniScript, repeat) {
        val count = getSymbol("count") as Double
        val function = getSymbol("block") as Function

        if (count.mod(1.0) != 0.0)
            throw MiniScriptException.RuntimeError("Can't repeat a non-integer amount of times", this)

        for (i in 0..<count.toLong())
            function.call(i.toDouble())
    }
}