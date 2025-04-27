import { Block } from "../ast/node/block";
import type { Lambda } from "../ast/node/lambda";
import type { Function } from "../ast/tree/function/function";
import { Native } from "../ast/tree/statement/native";
import { Logger } from "../diagnostic/logger";
import { Lexer } from "../lexer/lexer";
import { Parser } from "../parser/parser";

export function registerFunctions(block: Block) {
    const repeatFunc = createNativeFunction("func repeat(count: number, block: (index: number) -> void)", repeat);
    block.symbolTable.set("repeat", repeatFunc);

    const printFunc = createNativeFunction("func print(obj: any)", print);
    block.symbolTable.set("print", printFunc);
}

function print(native: Native) {
    const block = native.getParent<Block>(Block)!;
    const obj = block.getSymbol("obj");
    console.log(obj);
}

function repeat(native: Native) {
    const block = native.getParent<Block>(Block)!;
    const count = block.getSymbol("count") as number;
    const runnable = block.getSymbol("block") as Lambda;
    
    for (let i = 0; i < count; i++)
        runnable.execute([i], null);
}

export function createNativeFunction(header: string, runnable: (native: Native) => void): Function {
    const parent = new Block([], null);
    const logger = new Logger();
    const lexer = new Lexer(header);
    const tokens = lexer.lex(logger).getOrThrow();
    const parser = new Parser(logger, tokens);
    const native = new Native(runnable, null);
    const block = new Block([native], parent);
    const { func } = parser.parseFunctionDeclaration(parent, false);
    native.parent = block;
    func.lambda.block = block;
    block.parent = func.lambda;
    return func;
}