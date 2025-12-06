package me.honkling.miniscript.stdlib

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.miniScript
import me.honkling.miniscript.parser.ast.Operator.*
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.prototype.FunctionReference
import me.honkling.miniscript.parser.ast.string.ComplexString

fun registerChangers(miniScript: MiniScript) {
    miniScript.registerChanger<Any, Any, Boolean>(Equals, NotEquals) { lhs, rhs, op ->
        if (op == Equals) lhs == rhs
        else lhs != rhs
    }

    miniScript.registerChanger<Double, Double, Double>(Plus, Minus, Multiply, Divide) { lhs, rhs, op ->
        lhs as Double
        rhs as Double

        when (op) {
            Plus -> lhs + rhs
            Minus -> lhs - rhs
            Multiply -> lhs * rhs
            Divide -> lhs / rhs
            else -> throw IllegalStateException("Invalid operator")
        }
    }

    miniScript.registerChanger<Double, Double, Boolean>(GreaterThan, GreaterEquals, LessThan, LessEquals) { lhs, rhs, op ->
        lhs as Double
        rhs as Double

        when (op) {
            GreaterThan -> lhs > rhs
            GreaterEquals -> lhs >= rhs
            LessThan -> lhs < rhs
            LessEquals -> lhs <= rhs
            else -> throw IllegalStateException("Invalid operator")
        }
    }

    val stringClass = miniScript.environment.stringClass
    miniScript.registerClassChanger<Double, String>(stringClass, Multiply) { lhs, rhs, op ->
        val stringInstance = lhs as? ClassInstance ?: rhs as ClassInstance
        val double = rhs as? Double ?: lhs as Double

        if (double.mod(1.0) != 0.0)
            throw MiniScriptException.RuntimeError("Can't repeat a string a non-integer amount of times", null)

        val string = (stringInstance.fields["value"] as List<Char>).joinToString("")
        string.repeat(double.toInt())
    }

    miniScript.registerClassChanger<Any, ClassInstance>(stringClass, Plus) { lhs, rhs, op ->
        val classInstance = lhs as? ClassInstance ?: rhs as ClassInstance
        val otherValue = if (classInstance == lhs) rhs else lhs
        val stringValue = (classInstance.fields["value"] as List<Char>).joinToString("")
        val otherString = if (otherValue is ClassInstance && otherValue.classRef == stringClass)
            (otherValue.fields["value"] as List<Char>).joinToString("")
        else otherValue.toString()

        stringClass.instantiate(listOf(ComplexString.StringGetter(stringValue + otherString)))
    }

    miniScript.registerChanger<ArrayList<*>, ArrayList<*>, MutableList<*>>(Plus, Minus) { lhs, rhs, op ->
        lhs as ArrayList<*>
        rhs as ArrayList<*>

        if (op == Plus)
            mutableListOf(*lhs.toTypedArray(), *rhs.toTypedArray())
        else {
            val clone = mutableListOf(*lhs.toTypedArray())
            clone.removeAll(rhs)
            clone
        }
    }

    miniScript.registerChanger<ArrayList<*>, Any?, MutableList<*>>(Plus, Minus) { lhs, rhs, op ->
        val array = lhs as? ArrayList<*> ?: rhs as ArrayList<*>
        val other = if (array === lhs) rhs else lhs

        val clone = mutableListOf<Any?>(*array.toTypedArray())
        if (op == Plus)
            clone += other
        else clone -= other

        clone
    }

    miniScript.registerChanger<LinkedHashMap<*, *>, Any?, Any?>(Period) { lhs, rhs, op ->
        lhs as LinkedHashMap<*, *>
        lhs[rhs]
    }

    miniScript.registerChanger<ArrayList<*>, Double, Any>(Period) { lhs, rhs, op ->
        lhs as ArrayList<*>
        rhs as Double

        if (rhs.mod(1.0) != 0.0)
            throw MiniScriptException.RuntimeError("Expected integer index for array", null)

        lhs[rhs.toInt()]!!
    }

    miniScript.registerChanger<ClassInstance, Any?, Any?>(Period) { lhs, rhs, op ->
        lhs as ClassInstance

        lhs.classRef.tryResolveFunction(rhs)?.let { FunctionReference(lhs, it) }
            ?: (lhs.fields[rhs] as? Function)?.let { FunctionReference(lhs, it) }
            ?: lhs.fields[rhs]
    }

    miniScript.registerChanger<ClassInstance.SuperInstance, Any?, Any?>(Period) { lhs, rhs, op ->
        lhs as ClassInstance.SuperInstance

        lhs.instance.classRef.superClass?.tryResolveFunction(rhs)?.let { FunctionReference(lhs.instance, it) }
            ?: lhs.instance.fields[rhs]
    }
}