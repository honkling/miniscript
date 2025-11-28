package me.honkling.miniscript.parser.ast.stack

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.SymbolHolder
import me.honkling.miniscript.parser.ast.SymbolTable

class Frame : SymbolHolder {
    override val symbolTable = SymbolTable()
}