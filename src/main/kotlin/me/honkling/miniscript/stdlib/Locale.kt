package me.honkling.miniscript.stdlib

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.statement.nativeBlock
import java.time.Instant

fun registerStdlibLocale(miniScript: MiniScript, symbols: SymbolTable) {
    val clock = symbols["clock"] as Function
    clock.block = nativeBlock(miniScript, clock) {
        parent.returnValue = Instant.now().toEpochMilli().toDouble()
    }
}