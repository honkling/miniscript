package me.honkling.miniscript.stdlib

import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.expression.NativeExpression
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.stdlib.builder.`class`

fun registerStdlibArrays(environment: Environment) {
    val stringClassRef = NativeExpression { environment.stringClass to ExecutionResult.ContinueExecution }

    environment.setSymbol("Array", environment.`class`("Array", "data" to Type.Array(Type.Any)) {
        field("size", Type.Number)

        init {
            val `this` = getSymbol("this") as ClassInstance
            val data = `this`.fields["data"] as List<*>
            `this`.fields["size"] = data.size.toDouble()
        }

        function("to_string", Type.Class(stringClassRef, environment)) {
            val arrayClass = environment.arrayClass
            val stringClass = environment.stringClass
            val `this` = getSymbol("this") as ClassInstance
            val builder = StringBuilder("[")
            val data = `this`.fields["data"] as List<*>

            for ((index, value) in data.withIndex()) {
                builder.append(environment.stringifyValue(value))

                if (index + 1 < data.size)
                    builder.append(", ")
            }

            val array = arrayClass.instantiate(builder.append("]").toString().toMutableList())
            parent.returnValue = stringClass.instantiate(array)
        }
    })

//    val sizeof = symbols["sizeof"] as Function
//    sizeof.block = nativeBlock(miniScript, sizeof) {
//        val values = getSymbol("values") as List<*>
//        parent.returnValue = values.size.toDouble()
//    }
}