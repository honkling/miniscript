import type { Pass } from "../../../pass/pass";
import type { Block } from "../../node/block";
import type { Dependent, Expression } from "../../node/expression";
import { ExecutionResult, Statement } from "../../node/statement";

export abstract class Loop extends Statement {
    public static While = class extends Loop {
        constructor(
            public expression: Dependent<boolean>,
            block: Block,
            parent: Block
        ) {
            super(block, parent);
        }

        public execute(): ExecutionResult {
            while (this.expression.get()) {
                const result = this.block.execute();

                if (result === ExecutionResult.Return)
                    return result;
                else if (result === ExecutionResult.BreakLoop)
                    return ExecutionResult.ContinueEvaluation;
            }
            
            return ExecutionResult.ContinueEvaluation;
        }

        public visit(pass: Pass): void {
            pass.visitWhileLoop(this);
        }
    }

    public static ForEach = class extends Loop {
        constructor(
            public identifier: string,
            public expression: Dependent<any[]>,
            block: Block,
            parent: Block
        ) {
            super(block, parent);
        }

        public execute(): ExecutionResult {
            const values = this.expression.get();
            
            for (const value of values) {
                this.block.symbolTable.set(this.identifier, value);
                const result = this.block.execute();
                
                if (result === ExecutionResult.Return)
                    return result;
                else if (result === ExecutionResult.BreakLoop)
                    return ExecutionResult.ContinueEvaluation;
            }

            return ExecutionResult.ContinueEvaluation;
        }

        public visit(pass: Pass): void {
            pass.visitForEachLoop(this);
        }
    }

    constructor(
        public block: Block,
        parent: Block
    ) {
        super(parent);
    }
}