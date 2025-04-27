import type { Pass } from "../../pass/pass";
import type { Parameter } from "../tree/function/parameter";
import type { Block } from "./block";
import type { Lambda } from "./lambda";
import { Node } from "./node";

export class Type<T> extends Node<any> {
    public static Void = class extends Type<void> {}
    public static Any = class extends Type<any> {}
    public static String = class extends Type<string> {}
    public static Number = class extends Type<number> {}
    public static Boolean = class extends Type<boolean> {}
    public static Lambda = class extends Type<Lambda> {
        constructor(
            public parameters: Parameter[],
            public returnType: Type<any>,
            parent: any
        ) {
            super(parent);
        }
    }

    public isArray: boolean = false;

    public visit(pass: Pass): void {
        pass.visitType(this);
    }
}