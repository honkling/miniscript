import type { Pass } from "../../../pass/pass";
import type { Block } from "../../node/block";
import type { Dependent } from "../../node/expression";
import { ExecutionResult, Statement } from "../../node/statement";

export class Condition extends Statement {
    constructor(
        public expression: Dependent<boolean>,
        public block: Block,
        parent: Block | null
    ) {
        super(parent);
    }

    public execute(): ExecutionResult {
        if (this.expression.get()) {
            const result = this.block.execute();

            if (result !== ExecutionResult.ContinueEvaluation)
                return result;
        }

        return ExecutionResult.ContinueEvaluation;
    }

    public visit(pass: Pass): void {
        pass.visitCondition(this);
    }
}