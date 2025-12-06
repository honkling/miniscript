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
        val value = if (message is ClassInstance && message.classRef == miniScript.environment.stringClass)
            (message.fields["value"] as List<Char>).joinToString("")
        else message

        println(value)
    }
}