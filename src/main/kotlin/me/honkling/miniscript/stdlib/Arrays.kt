package me.honkling.miniscript.stdlib

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.statement.nativeBlock

fun registerStdlibArrays(miniScript: MiniScript, symbols: SymbolTable) {
    val sizeof = symbols["sizeof"] as Function
    sizeof.block = nativeBlock(miniScript, sizeof) {
        val values = getSymbol("values") as List<*>
        parent.returnValue = values.size.toDouble()
    }
}