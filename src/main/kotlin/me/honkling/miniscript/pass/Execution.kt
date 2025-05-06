package me.honkling.miniscript.pass

import me.honkling.miniscript.parser.ast.Block

class Execution : Pass() {
    override fun visitBlock(node: Block) {
        node.execute()
    }
}