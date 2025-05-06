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
        when (op) {
            Plus -> lhs + rhs
            Minus -> lhs - rhs
            Multiply -> lhs * rhs
            Divide -> lhs / rhs
            else -> throw MiniScriptException.RuntimeError("Unknown operator $op")
        }
    }

    miniScript.registerChanger<Double, Double, Boolean>(GreaterThan, GreaterEquals, LessThan, LessEquals) { lhs, rhs, op ->
        when (op) {
            GreaterThan -> lhs > rhs
            GreaterEquals -> lhs >= rhs
            LessThan -> lhs < rhs
            LessEquals -> lhs <= rhs
            else -> throw MiniScriptException.RuntimeError("Unknown operator $op")
        }
    }

    miniScript.registerChanger<String, Double, String>(Multiply) { lhs, rhs, op ->
        if (rhs.mod(1.0) != 0.0)
            throw MiniScriptException.RuntimeError("Can't repeat a string a non-integer amount of times")

        lhs.repeat(rhs.toInt())
    }

    miniScript.registerChanger<String, Any, String>(Plus) { lhs, rhs, op ->
        lhs + rhs.toString()
    }


    miniScript.registerChanger<ArrayList<*>, ArrayList<*>, MutableList<*>>(Plus, Minus) { lhs, rhs, op ->
        if (op == Plus)
            mutableListOf(*lhs.toTypedArray(), *rhs.toTypedArray())
        else {
            val clone = mutableListOf(*lhs.toTypedArray())
            clone.removeAll(rhs)
            clone
        }
    }

    miniScript.registerChanger<LinkedHashMap<*, *>, Any?, Any?>(Period) { lhs, rhs, op ->
        lhs[rhs]
    }

    miniScript.registerChanger<ArrayList<*>, Double, Any>(Period) { lhs, rhs, op ->
        lhs[rhs.toInt()]!!
    }
}