package me.honkling.miniscript.parser.ast.expression

import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.pass.Pass

open class NativeExpression<T>(val block: () -> Pair<T, ExecutionResult>) : Expression<T>(null, null) {
    override fun get(): Pair<T, ExecutionResult> {
        return block()
    }

    override fun accept(pass: Pass) {
        pass.visitNativeExpression(this)
    }
}