import type { Block } from "../ast/node/block";
import type { Duplex } from "../ast/node/duplex";
import type { Expression } from "../ast/node/expression";
import type { Lambda } from "../ast/node/lambda";
import type { Node } from "../ast/node/node";
import type { Statement } from "../ast/node/statement";
import type { ComponentString, StringComponent } from "../ast/node/string";
import type { Type } from "../ast/node/type";
import type { Value } from "../ast/node/value";
import type { FunctionCall } from "../ast/tree/duplex/functionCall";
import type { Arithmetic } from "../ast/tree/expression/arithmetic";
import type { Function } from "../ast/tree/function/function";
import type { Parameter } from "../ast/tree/function/parameter";
import type { Assignment } from "../ast/tree/statement/assignment";
import { Break } from "../ast/tree/statement/break";
import type { Condition } from "../ast/tree/statement/condition";
import type { Continue } from "../ast/tree/statement/continue";
import type { FunctionDeclaration } from "../ast/tree/statement/functionDeclaration";
import type { Loop } from "../ast/tree/statement/loop";
import type { Return } from "../ast/tree/statement/return";

export abstract class Pass {
    public visit(node: Node<any>) {
        node.visit(this);
    }
    public visitBlock(block: Block) {}
    public visitStatement(statement: Statement) {}
    public visitExpression(expression: Expression<any>) {}
    public visitDuplex(duplex: Duplex<any>) {}
    public visitValue(value: Value<any>) {}
    public visitFunction(func: Function) {}
    public visitParameter(parameter: Parameter) {}
    public visitFunctionDeclaration(declaration: FunctionDeclaration) {}
    public visitForEachLoop(loop: Loop) {}
    public visitFunctionCall(call: FunctionCall) {}
    public visitWhileLoop(loop: Loop) {}
    public visitReturn(ret: Return) {}
    public visitArithmetic(arithmetic: Arithmetic) {}
    public visitContinue(statement: Continue) {}
    public visitBreak(statement: Break) {}
    public visitType(type: Type<any>) {}
    public visitLambda(lambda: Lambda) {}
    public visitCondition(condition: Condition) {}
    public visitAssignment(assignment: Assignment) {}
    public visitComponentString(string: ComponentString) {}
    public visitStringComponent(component: StringComponent<any, any>) {}
}