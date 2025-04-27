import type { Pass } from "../../pass/pass";

export abstract class Node<P extends Node<any> | null> {
    constructor(public parent: P | null) {}

    public getParent<T>(constructor: Function): T | null {
        if (this.parent === null)
            return null;

        if (this.parent instanceof constructor)
            return this.parent as T;

        return this.parent.getParent<T>(constructor);
    }

    public abstract visit(pass: Pass): void;
}