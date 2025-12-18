package me.honkling.miniscript.stdlib

import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.expression.NativeExpression
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.parser.ast.statement.ExecutionResult
import me.honkling.miniscript.stdlib.builder.`class`

fun registerStdlibArrays(environment: Environment) {
    val stringType = Type.Class(
        NativeExpression { environment.stringClass to ExecutionResult.ContinueExecution },
        environment
    )

    val arrayType = Type.Class(
        NativeExpression { environment.arrayClass to ExecutionResult.ContinueExecution },
        environment
    )

    environment.setSymbol("Array", environment.`class`("Array", "data" to Type.Array(Type.Any)) {
        field("size", Type.Number)

        init {
            val `this` = getSymbol("this") as ClassInstance
            val data = `this`.fields["data"] as List<*>
            `this`.fields["size"] = data.size.toDouble()
        }

        function("insert", Type.Void, "index" to Type.Number, "value" to Type.Any) {
            val `this` = getSymbol("this") as ClassInstance
            val index = getSymbol("index") as Double
            val value = getSymbol("value")
            val data = `this`.fields["data"] as MutableList<Any?>

            data.add(index.toInt(), value)
        }

        function("remove", Type.Void, "value" to Type.Any) {
            val `this` = getSymbol("this") as ClassInstance
            val value = getSymbol("value")!!
            val data = `this`.fields["data"] as MutableList<*>

            data.remove(value)
        }

        function("remove_at", Type.Void, "index" to Type.Number) {
            val `this` = getSymbol("this") as ClassInstance
            val index = getSymbol("index") as Double
            val data = `this`.fields["data"] as MutableList<*>

            data.removeAt(index.toInt())
        }

        function("remove_first", Type.Void) {
            val `this` = getSymbol("this") as ClassInstance
            val data = `this`.fields["data"] as MutableList<*>

            data.removeFirst()
        }

        function("remove_last", Type.Void) {
            val `this` = getSymbol("this") as ClassInstance
            val data = `this`.fields["data"] as MutableList<*>

            data.removeLast()
        }

        function("filter", arrayType, "block" to Type.Function(Type.Boolean, "it" to stringType)) {
            val `this` = getSymbol("this") as ClassInstance
            val block = getSymbol("block") as Function
            val data = `this`.fields["data"] as List<*>

            parent.returnValue = environment.arrayClass.instantiate(data.filter {
                block.call(it!!) as? Boolean ?: false
            })
        }

        function("map", arrayType, "block" to Type.Function(Type.Any, "it" to stringType)) {
            val `this` = getSymbol("this") as ClassInstance
            val block = getSymbol("block") as Function
            val data = `this`.fields["data"] as List<*>

            parent.returnValue = environment.arrayClass.instantiate(data.map {
                block.call(it!!)
            })
        }

        function("join", stringType, "delimiter" to stringType) {
            val delimiterObject = getSymbol("delimiter") as ClassInstance
            val `this` = getSymbol("this") as ClassInstance
            val data = `this`.fields["data"] as List<*>

            val delimiter = ((delimiterObject.fields["value"] as ClassInstance).fields["data"] as List<Char>).joinToString("")
            val returnValue = data.joinToString(delimiter) { environment.stringifyValue(it) }

            val array = environment.arrayClass.instantiate(returnValue.toMutableList())
            parent.returnValue = environment.stringClass.instantiate(array)
        }

        function("to_string", stringType) {
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
}