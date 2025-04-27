import type { Pass } from "../../../pass/pass";
import type { Dependent } from "../../node/expression";
import type { Lambda } from "../../node/lambda";
import { Node } from "../../node/node";
import type { Type } from "../../node/type";

export class Parameter extends Node<Type<Lambda> | Lambda> {
    constructor(
        public name: string,
        public type: Type<any> | null,
        public defaultValue: Dependent<any> | null,
        parent: Lambda | Type<Lambda> | null
    ) {
        super(parent);
    }

    public visit(pass: Pass): void {
        pass.visitParameter(this);
    }
}