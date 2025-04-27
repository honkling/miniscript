import type { Pass } from "../../../pass/pass";
import type { Block } from "../../node/block";
import type { Dependent, Expression } from "../../node/expression";
import { ExecutionResult, Statement } from "../../node/statement";

export class Return extends Statement {
    constructor(public value: Dependent<any>, parent: Block | null) {
        super(parent);
    }

    public execute(): ExecutionResult {
        this.parent!.returnValue = this.value.get();
        return ExecutionResult.Return;
    }

    public visit(pass: Pass): void {
        pass.visitReturn(this);
    }
}