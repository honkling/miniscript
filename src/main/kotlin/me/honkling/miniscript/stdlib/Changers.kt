package me.honkling.miniscript.stdlib

import me.honkling.miniscript.Changer
import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.diagnostic.MiniScriptException
import me.honkling.miniscript.parser.ast.Operator
import me.honkling.miniscript.parser.ast.Operator.*
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.prototype.FunctionReference
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.parser.ast.string.ComplexString
import kotlin.reflect.KClass

fun registerChangers(environment: Environment) {
    val stringClass = environment.stringClass
    val arrayClass = environment.arrayClass

    environment.registerChanger<Boolean, Unit, Boolean>(LogicalNOT) { lhs, rhs, op ->
        !(lhs as Boolean)
    }

    environment.registerChanger<Any, Any, Boolean>(Equals, NotEquals) { lhs, rhs, op ->
        if (op == Equals) environment.areValuesEqual(lhs, rhs)
        else !environment.areValuesEqual(lhs, rhs)
    }

    environment.registerChanger<Double, Double, Double>(Plus, Minus, Multiply, Divide) { lhs, rhs, op ->
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

    environment.registerChanger<Double, Double, Boolean>(GreaterThan, GreaterEquals, LessThan, LessEquals) { lhs, rhs, op ->
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

    environment.registerClassChanger<Double, ClassInstance>(stringClass, Multiply) { lhs, rhs, op ->
        val stringInstance = lhs as? ClassInstance ?: rhs as ClassInstance
        val double = rhs as? Double ?: lhs as Double

        if (double.mod(1.0) != 0.0)
            throw MiniScriptException.RuntimeError("Can't repeat a string a non-integer amount of times", null)

        val string = ((stringInstance.fields["value"] as ClassInstance).fields["data"] as List<Char>).joinToString("")
        stringClass.instantiate(arrayClass.instantiate(string.repeat(double.toInt())))
    }

    environment.registerClassChanger<Any, ClassInstance>(stringClass, Plus) { lhs, rhs, op ->
        val classInstance = lhs as? ClassInstance ?: rhs as ClassInstance
        val leftSideIsClass = classInstance == lhs
        val otherValue = if (leftSideIsClass) rhs else lhs
        val stringValue = ((classInstance.fields["value"] as ClassInstance).fields["data"] as List<Char>).joinToString("")
        val otherString = if (otherValue is ClassInstance && otherValue.classRef == stringClass)
            ((otherValue.fields["value"] as ClassInstance).fields["data"] as List<Char>).joinToString("")
        else otherValue.toString()

        val charArray = arrayClass.instantiate((
                if (leftSideIsClass) stringValue + otherString
                else otherString + stringValue
        ).toMutableList())
        stringClass.instantiate(charArray)
    }

    environment.registerClassChanger<ClassInstance>(arrayClass, arrayClass, Plus, Minus) { lhs, rhs, op ->
        val leftArray = lhs.fields["data"] as MutableList<Any>
        val rightArray = rhs.fields["data"] as MutableList<Any>

        if (op == Plus)
            arrayClass.instantiate(mutableListOf(*leftArray.toTypedArray(), *rightArray.toTypedArray()))
        else {
            val clone = mutableListOf(*leftArray.toTypedArray())
            clone.removeAll(rightArray)
            arrayClass.instantiate(clone)
        }
    }

    environment.registerClassChanger<Any?, ClassInstance>(arrayClass, Plus, Minus, priority = -10) { lhs, rhs, op ->
        val array = lhs as? ClassInstance ?: rhs as ClassInstance
        val other = if (array === lhs) rhs else lhs
        val data = array.fields["data"] as MutableList<*>

        val clone = mutableListOf(*data.toTypedArray())
        if (op == Plus)
            clone += other
        else clone -= other

        arrayClass.instantiate(clone)
    }

    environment.registerChanger<LinkedHashMap<*, *>, Any?, Any?>(Period) { lhs, rhs, op ->
        lhs as LinkedHashMap<*, *>
        lhs[rhs]
    }

    environment.registerClassChanger<Double, Any>(arrayClass, Period) { lhs, rhs, op ->
        lhs as ClassInstance
        rhs as Double

        val data = lhs.fields["data"] as MutableList<*>

        if (rhs.mod(1.0) != 0.0)
            throw MiniScriptException.RuntimeError("Expected integer index for array", null)

        data[rhs.toInt()]!!
    }

    environment.registerChanger<ClassInstance, Any?, Any?>(Period) { lhs, rhs, op ->
        lhs as ClassInstance

        lhs.classRef.tryResolveFunction(rhs)?.let { FunctionReference(lhs, it) }
            ?: (lhs.fields[rhs] as? Function)?.let { FunctionReference(lhs, it) }
            ?: lhs.fields[rhs]
    }

    environment.registerChanger<ClassInstance.SuperInstance, Any?, Any?>(Period) { lhs, rhs, op ->
        lhs as ClassInstance.SuperInstance

        lhs.instance.classRef.superClass?.tryResolveFunction(rhs)?.let { FunctionReference(lhs.instance, it) }
            ?: lhs.instance.fields[rhs]
    }
}

fun tryGetChangers(
    miniScript: MiniScript,
    lhs: Any?,
    rhs: Any?,
    leftClass: KClass<*>,
    rightClass: KClass<*>,
    op: Operator,
    tryVariant: Boolean = true
): List<Changer<*>>? {
    val changers = miniScript.environment.changers

    changers[leftClass]?.get(rightClass)
        ?.sortedBy { it.priority }
        ?.filter {
            op in it.validOperators
                    && (it.leftClass == null || (lhs is ClassInstance && it.leftClass == lhs.classRef))
                    && (it.rightClass == null || (rhs is ClassInstance && it.rightClass == rhs.classRef))
        }
        ?.ifEmpty { null }
        ?.let { return it }

    if (tryVariant)
        return tryGetChangers(miniScript, lhs, rhs, leftClass, Any::class, op, false)
            ?: tryGetChangers(miniScript, lhs, rhs, Any::class, rightClass, op, false)
            ?: tryGetChangers(miniScript, lhs, rhs, rightClass, leftClass, op, false)
            ?: tryGetChangers(miniScript, lhs, rhs, rightClass, Any::class, op, false)
            ?: tryGetChangers(miniScript, lhs, rhs, Any::class, leftClass, op, false)
            ?: tryGetChangers(miniScript, lhs, rhs, Any::class, Any::class, op, false)

    return null
}