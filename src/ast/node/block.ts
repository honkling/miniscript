import { SymbolTable } from "../../lib/symbolTable";
import type { Pass } from "../../pass/pass";
import type { Dependent } from "./expression";
import { Lambda } from "./lambda";
import { Node } from "./node";
import { ExecutionResult, Statement, type Standalone } from "./statement";
import type { Value } from "./value";

export class Block extends Node<Node<any>> {
    public symbolTable = new SymbolTable();
    public returnValue: any | null = null;

    constructor(public statements: Standalone[], parent: Node<any> | null) {
        super(parent);
    }

    public execute(): ExecutionResult {
        for (const statement of this.statements) {
            const result = statement.execute();

            if (statement instanceof Statement && result !== ExecutionResult.ContinueEvaluation)
                return result as ExecutionResult;
        }

        return ExecutionResult.ContinueEvaluation;
    }

    public visit(pass: Pass): void {
        pass.visitBlock(this);
    }

    public getSymbol(name: string): any | null {
        if (this.symbolTable.has(name))
            return this.symbolTable.get(name) ?? null;
        else if (this.parent instanceof Block)
            return this.parent.getSymbol(name);
        else return this.parent?.getParent<Block>(Block)?.getSymbol(name) ?? null;
    }

    public setSymbol(name: string, value: any) {
        if (!this.setSymbolInternal(name, value))
            this.symbolTable.set(name, value);
    }

    private setSymbolInternal(name: string, value: any): boolean {
        if (this.symbolTable.has(name)) {
            this.symbolTable.set(name, value);
            return true;
        } else if (this.parent instanceof Block)
            return this.parent.setSymbolInternal(name, value)
        else return this.parent?.getParent<Block>(Block)?.setSymbolInternal(name, value) ?? false;
    }
}