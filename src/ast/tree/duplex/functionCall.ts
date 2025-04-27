import type { Pass } from "../../../pass/pass";
import { Block } from "../../node/block";
import { Duplex } from "../../node/duplex";
import type { Dependent, Expression, ExpressionParent } from "../../node/expression";
import { Lambda } from "../../node/lambda";
import type { Statement } from "../../node/statement";
import type { ComponentString } from "../../node/string";
import { Function } from "../function/function";

export class FunctionCall extends Duplex<any> {
    constructor(
        public name: string,
        public args: Dependent<any>[],
        public lambda: Lambda | null,
        parent: Block | ExpressionParent | null
    ) {
        super(parent);
    }

    public get(): any {
        const block = this.getParent<Block>(Block);

        if (!block)
            throw new Error("Couldn't find a block parent");

        const func = block.getSymbol(this.name);
        const args = [];

        for (const arg of this.args)
            args.push(arg.get());

        if (func instanceof Function)
            return func.lambda.execute(args, this.lambda);
        else if (func instanceof Lambda)
            return func.execute(args, this.lambda);
        else throw new Error(`Tried to call a non-function object '${this.name}'`);
    }

    public visit(pass: Pass): void {
        pass.visitFunctionCall(this);
    }
}