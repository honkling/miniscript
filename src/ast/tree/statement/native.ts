import type { Pass } from "../../../pass/pass";
import type { Block } from "../../node/block";
import { ExecutionResult, Statement } from "../../node/statement";

export class Native extends Statement {
    constructor(
        public func: (native: Native) => void,
        parent: Block | null
    ) {
        super(parent);
    }

    public execute(): ExecutionResult {
        this.func(this);
        return ExecutionResult.ContinueEvaluation;
    }

    public visit(pass: Pass): void {
        //pass.visitNative(this);
    }
}