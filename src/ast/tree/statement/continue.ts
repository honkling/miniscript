import type { Pass } from "../../../pass/pass";
import type { Block } from "../../node/block";
import { ExecutionResult, Statement } from "../../node/statement";

export class Continue extends Statement {
    constructor(parent: Block | null) {
        super(parent);
    }

    public execute(): ExecutionResult {
        return ExecutionResult.ContinueLoop;
    }

    public visit(pass: Pass): void {
        pass.visitContinue(this);
    }
}