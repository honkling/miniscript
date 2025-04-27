import type { Block } from "../ast/node/block";
import { Duplex } from "../ast/node/duplex";
import { Expression } from "../ast/node/expression";
import type { Node } from "../ast/node/node";
import { Statement } from "../ast/node/statement";
import { Pass } from "./pass";

export class ExecutionPass extends Pass {
    public visit(node: Node<any>): void {
        if (node instanceof Statement)
            this.visitStatement(node);
        else if (node instanceof Expression)
            this.visitExpression(node);
        else if (node instanceof Duplex)
            this.visitDuplex(node);
        else node.visit(this);
    }

    public visitBlock(block: Block): void {
        block.execute();
    }

    public visitStatement(statement: Statement): void {
        console.log("Yes");
    }
}