package me.honkling.miniscript.stdlib

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.miniScript
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.prototype.ClassInstance

fun registerStringHandlers(miniScript: MiniScript) {
    miniScript.registerStringHandler<ClassInstance> { value ->
        val toString = value.classRef.methods.find {
            val returnsString = it.returnType is Type.Class && it.returnType.reference.get() == miniScript.environment.stringClass
            it.name == "to_string" && it.parameters.isEmpty() && returnsString
        }

        if (value.classRef == miniScript.environment.stringClass)
            (value.fields["value"] as List<Char>).joinToString("")
        else if (toString != null) {
            val stringInstance = toString.call()
            (value.fields["value"] as List<Char>).joinToString("")
        } else {
            val fields = miniScript.stringifyValue(value.fields)
            "${value.classRef.name}$fields"
        }
    }

    miniScript.registerStringHandler<LinkedHashMap<*, *>> { value ->
        val builder = StringBuilder("{")
        val entries = value.entries.map { (key, value) ->
            val stringKey = miniScript.stringifyValue(key)
            stringKey to value
        }.sortedBy { it.first }

        for ((index, entry) in entries.withIndex()) {
            val (k, v) = entry
            val stringValue = miniScript.stringifyValue(v)
            builder.append("$k=$stringValue")

            if (index + 1 < entries.size)
                builder.append(", ")
        }

        builder.append("}").toString()
    }

    miniScript.registerStringHandler<ArrayList<*>> { value ->
        val builder = StringBuilder("[")

        for ((index, element) in value.withIndex()) {
            builder.append(miniScript.stringifyValue(element))

            if (index + 1 < value.size)
                builder.append(", ")
        }

        builder.append("]").toString()
    }

    miniScript.registerStringHandler<Double> { value ->
        (if (value % 1 == 0.0) value.toInt() else value).toString()
    }
}