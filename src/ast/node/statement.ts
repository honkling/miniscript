import type { Pass } from "../../pass/pass";
import type { Block } from "./block";
import type { Duplex } from "./duplex";
import { Node } from "./node";

export type Standalone = Statement | Duplex<any>;

export abstract class Statement extends Node<Block> {
    public abstract execute(): ExecutionResult;

    public visit(pass: Pass): void {
        pass.visitStatement(this);
    }
}

export enum ExecutionResult {
    ContinueEvaluation,
    BreakLoop,
    ContinueLoop,
    Return
}