package me.honkling.miniscript.pass

import me.honkling.miniscript.parser.ast.Block
import me.honkling.miniscript.parser.ast.Node
import me.honkling.miniscript.parser.ast.Type
import me.honkling.miniscript.parser.ast.Value
import me.honkling.miniscript.parser.ast.expression.Arithmetic
import me.honkling.miniscript.parser.ast.expression.FunctionCall
import me.honkling.miniscript.parser.ast.expression.ClassDeclaration
import me.honkling.miniscript.parser.ast.function.Function
import me.honkling.miniscript.parser.ast.function.Parameter
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.prototype.Field
import me.honkling.miniscript.parser.ast.stack.Environment
import me.honkling.miniscript.parser.ast.statement.Assignment
import me.honkling.miniscript.parser.ast.statement.Break
import me.honkling.miniscript.parser.ast.statement.Continue
import me.honkling.miniscript.parser.ast.statement.FunctionDeclaration
import me.honkling.miniscript.parser.ast.statement.If
import me.honkling.miniscript.parser.ast.statement.Loop
import me.honkling.miniscript.parser.ast.statement.Native
import me.honkling.miniscript.parser.ast.statement.Return

abstract class Pass {
    open fun visit(node: Node<*>) {
        node.accept(this)
    }

    open fun visitEnvironment(environment: Environment) {}
    open fun visitBlock(node: Block) {}
    open fun visitValue(node: Value<*>) {}
    open fun visitType(node: Type<*>) {}
    open fun visitNative(node: Native) {}
    open fun visitLoop(node: Loop) {}
    open fun visitReturn(node: Return) {}
    open fun visitContinue(node: Continue) {}
    open fun visitBreak(node: Break) {}
    open fun visitIf(node: If) {}
    open fun visitAssignment(node: Assignment) {}
    open fun visitFunctionDeclaration(node: FunctionDeclaration) {}
    open fun visitFunctionCall(node: FunctionCall) {}
    open fun visitFunction(node: Function) {}
    open fun visitParameter(node: Parameter) {}
    open fun visitClassDeclaration(node: ClassDeclaration) {}
    open fun visitClass(node: Class) {}
    open fun visitField(node: Field) {}
    open fun visitArithmetic(node: Arithmetic) {}
}