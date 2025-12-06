package me.honkling.miniscript.parser.ast.stack

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.SymbolHolder
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.prototype.ClassInstance

class Frame(val classInstance: ClassInstance? = null) : SymbolHolder {
    override val symbolTable = SymbolTable()
}