import type { Pass } from "../../pass/pass";
import type { Block } from "./block";
import { Expression, type Dependent } from "./expression";
import type { Lambda } from "./lambda";
import { Node } from "./node";
import type { ComponentString } from "./string";

export type Dict<T> = { [key: string]: T };

export abstract class Value<I, E = I> extends Node<any> {
    public static Simple = class<T> extends Value<T, T> {
        public get(_: Block): T {
            return this.value;
        }
    }

    public static Number = class extends Value.Simple<number> {}
    public static Boolean = class extends Value.Simple<boolean> {}
    public static Lambda = class extends Value.Simple<Lambda> {}
    public static Literal = class extends Value.Simple<string> {}

    public static Dictionary = class extends Value<Dict<Dependent<any>>, Dict<any>> {
        public get(block: Block): Dict<any> {
            const dict: Dict<any> = {};

            for (const [key, value] of Object.entries(this.value))
                dict[key] = value.get();

            return dict;
        }
    }

    public static Array = class extends Value<Dependent<any>[], any[]> {
        public get(block: Block): any[] {
            const values = [];

            for (const value of this.value)
                values.push(value.get());

            return values;
        }
    }

    public static String = class extends Value<ComponentString, string> {
        public get(block: Block): string {
            let string = "";

            for (const component of this.value.components)
                string += component.get(block);

            return string;
        }
    }

    public static Identifier = class extends Value<string, any> {
        public get(block: Block) {
            return block.getSymbol(this.value);
        }
    }
    
    constructor(public value: I) {
        super(null);
    }

    public abstract get(block: Block): E;

    public visit(pass: Pass): void {
        pass.visitValue(this);
    }
}