import type { Pass } from "../../pass/pass";
import type { Function } from "../tree/function/function";
import type { Parameter } from "../tree/function/parameter";
import type { Block } from "./block";
import type { Dependent, Expression, ExpressionParent } from "./expression";
import { Node } from "./node";
import { Type } from "./type";
import type { Value } from "./value";

export class Lambda extends Node<ExpressionParent | Function> {
    constructor(
        public parameters: Parameter[],
        public block: Block,
        parent: ExpressionParent | Function | null
    ) {
        super(parent);
    }

    public execute(args: any[], block: Lambda | null): any | null {
        const argLength = args.length + (block ? 1 : 0);
        if (this.parameters.length === 0 && argLength > 0)
            this.block.symbolTable.set("it", args.length === 0 ? block : args[0]);
        else if (this.parameters.length !== argLength)
            throw new Error(`Tried to execute lambda with invalid args (expected ${this.parameters.length}, got ${argLength})`);

        for (let i = 0; i < this.parameters.length; i++) {
            const parameter = this.parameters[i];
            const argument = args[i];

            this.block.symbolTable.set(parameter.name, argument);
        }

        const lastBlock = this.parameters.find((x) => x.type instanceof Type.Any || x.type instanceof Type.Lambda);

        if (lastBlock && block)
            this.block.symbolTable.set(lastBlock.name, block);

        this.block.returnValue = null;
        this.block.execute();
        return this.block.returnValue;
    }

    public visit(pass: Pass): void {
        pass.visitLambda(this);
    }
}