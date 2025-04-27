import type { Token } from "../../../lexer/token";
import { operators } from "../../../lib/operators";
import type { Pass } from "../../../pass/pass";
import type { Block } from "../../node/block";
import type { Dependent, Expression } from "../../node/expression";
import { ExecutionResult, Statement } from "../../node/statement";
import { Arithmetic } from "../expression/arithmetic";

export class Assignment extends Statement {
    constructor(
        public name: string,
        public expression: Dependent<any> | null,
        public token: Token,
        parent: Block
    ) {
        super(parent);
    }

    public execute(): ExecutionResult {
        const left = this.parent!.getSymbol(this.name);
        const right = this.expression?.get();

        switch (this.token.type) {
            case "Increment":
                if (typeof left !== "number")
                    throw new Error("Cannot increment non-number", left);

                this.parent!.setSymbol(this.name, left + 1);
                break;
            case "Decrement":
                if (typeof left !== "number")
                    throw new Error("Cannot decrement non-number", left);

                this.parent!.setSymbol(this.name, left - 1);
                break;
            case "Assign":
                this.parent!.setSymbol(this.name, right);
                break;
            case "DivideAssign":
            case "MultiplyAssign":
            case "MinusAssign":
            case "PlusAssign":
                this.parent!.setSymbol(this.name, Arithmetic.evaluate(left, right, operators[this.token.type]!));
                break;
        }

        return ExecutionResult.ContinueEvaluation;
    }

    public visit(pass: Pass): void {
        pass.visitAssignment(this);
    }
}