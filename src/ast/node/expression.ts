import type { Pass } from "../../pass/pass";
import type { Parameter } from "../tree/function/parameter";
import type { Duplex } from "./duplex";
import { Node } from "./node";
import type { Statement } from "./statement";
import type { ComponentString } from "./string";
import type { Value } from "./value";

export type ExpressionParent = Statement | Value<any> | Duplex<any> | ComponentString | Parameter | Expression<any>;
export type Dependent<T> = Expression<T> | Duplex<T>;

export abstract class Expression<T> extends Node<ExpressionParent> {
    public abstract get(): T;

    public visit(pass: Pass): void {
        pass.visitExpression(this);
    }
}