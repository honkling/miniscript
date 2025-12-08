package me.honkling.miniscript.stdlib

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.statement.nativeBlock

fun registerStdlibLogging(miniScript: MiniScript, symbols: SymbolTable) {
    val print = symbols["print"] as Function
    print.block = nativeBlock(miniScript, print) {
        val message = getSymbol("message")
        println(miniScript.environment.stringifyValue(message))
    }
}