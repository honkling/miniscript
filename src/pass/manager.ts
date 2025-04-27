import type { Node } from "../ast/node/node";
import type { Pass } from "./pass";

export class PassManager {
    private passes: Pass[] = [];

    public register(pass: Pass) {
        if (this.passes.includes(pass))
            throw new Error("Pass is already registered");

        this.passes.push(pass);
    }

    public unregister(pass: Pass) {
        if (!this.passes.includes(pass))
            throw new Error("Pass isn't registered");

        this.passes = this.passes.filter((x) => x != pass);
    }

    public visit(node: Node<any>) {
        for (const pass of this.passes)
            pass.visit(node);
    }
}