import type { Pass } from "../../../pass/pass";
import type { Block } from "../../node/block";
import type { Lambda } from "../../node/lambda";
import { Node } from "../../node/node";
import type { Type } from "../../node/type";
import type { FunctionDeclaration } from "../statement/functionDeclaration";
import type { Parameter } from "./parameter";

export class Function extends Node<FunctionDeclaration> {
    constructor(
        public name: string,
        public lambda: Lambda,
        public returnType: Type<any> | null,
        parent: FunctionDeclaration | null
    ) {
        super(parent);
    }

    public visit(pass: Pass): void {
        pass.visitFunction(this);
    }
}