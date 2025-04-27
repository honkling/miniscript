import type { Pass } from "../../../pass/pass";
import { Block } from "../../node/block";
import { Expression, type ExpressionParent } from "../../node/expression";
import type { Value } from "../../node/value";

export class ValueExpression extends Expression<any> {
    constructor(public value: Value<any>, parent: ExpressionParent | null) {
        super(parent);
    }

    public get() {
        const block = this.parent!.getParent<Block>(Block);

        if (!block)
            throw new Error("Couldn't find parent block for value");

        return this.value.get(block);
    }

    public visit(pass: Pass): void {
        pass.visitValue(this.value);
    }
}