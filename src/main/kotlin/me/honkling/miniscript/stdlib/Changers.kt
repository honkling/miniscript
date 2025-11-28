package me.honkling.miniscript.stdlib

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Operator.*

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
            else -> throw MiniScriptException.RuntimeError("Unknown operator $op")
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
            else -> throw MiniScriptException.RuntimeError("Unknown operator $op")
        }
    }

    miniScript.registerChanger<String, Double, String>(Multiply) { lhs, rhs, op ->
        val string = lhs as? String ?: rhs as String
        val double = rhs as? Double ?: lhs as Double

        if (double.mod(1.0) != 0.0)
            throw MiniScriptException.RuntimeError("Can't repeat a string a non-integer amount of times")

        string.repeat(double.toInt())
    }

    miniScript.registerChanger<String, Any, String>(Plus) { lhs, rhs, op ->
        lhs.toString() + rhs.toString()
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
            throw MiniScriptException.RuntimeError("Expected integer index for array")

        lhs[rhs.toInt()]!!
    }
}