import type { Pass } from "../../pass/pass";
import type { Block } from "./block";
import type { ExpressionParent } from "./expression";
import { Node } from "./node";
import { ExecutionResult, type Statement } from "./statement";
import type { ComponentString } from "./string";

export abstract class Duplex<T> extends Node<Block | ExpressionParent> {
    public abstract get(): T;

    public visit(pass: Pass): void {
        pass.visitDuplex(this);
    }

    public execute() {
        this.get();
    }
}