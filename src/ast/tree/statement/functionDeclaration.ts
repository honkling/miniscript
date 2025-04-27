import type { Pass } from "../../../pass/pass";
import type { Block } from "../../node/block";
import { ExecutionResult, Statement } from "../../node/statement";
import type { Function } from "../function/function";

export class FunctionDeclaration extends Statement {
    constructor(public func: Function, parent: Block) {
        super(parent);
    }

    public execute(): ExecutionResult {
        this.parent!.symbolTable.set(this.func.name, this.func);
        return ExecutionResult.ContinueEvaluation;
    }

    public visit(pass: Pass): void {
        pass.visitFunctionDeclaration(this);
    }
}