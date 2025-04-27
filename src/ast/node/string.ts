import type { Pass } from "../../pass/pass";
import type { Block } from "./block";
import type { Dependent, Expression } from "./expression";
import { Node } from "./node";

export class ComponentString extends Node<any> {
    public components: StringComponent<any, any>[] = [];

    constructor(parent: any | null) {
        super(parent);
    }

    public addComponent(component: StringComponent<any, any>) {
        this.components.push(component);
    }

    public visit(pass: Pass): void {
        pass.visitComponentString(this);
    }
}

export abstract class StringComponent<I, E> extends Node<ComponentString> {
    public static Simple = class<T> extends StringComponent<T, T> {
        public get(_: Block): T {
            return this.value;
        }
    }

    public static Raw = class extends StringComponent.Simple<string> {}
    public static Identifier = class extends StringComponent<string, any> {
        public get(block: Block): any {
            return block.getSymbol(this.value);
        }
    }

    public static Expression = class<T> extends StringComponent<Dependent<T>, T> {
        public get(block: Block): any {
            return this.value.get();
        }
    }

    constructor(public value: I, parent: ComponentString) {
        super(parent);
    }

    public abstract get(block: Block): E;

    public visit(pass: Pass): void {
        pass.visitStringComponent(this);
    }
}