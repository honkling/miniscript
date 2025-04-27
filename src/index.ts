import { Lexer } from "./lexer/lexer";
import { readFileSync } from "fs";
import { join } from "path";
import { Logger } from "./diagnostic/logger";
import { DiagnosticType } from "./diagnostic/diagnostic";
import { Parser } from "./parser/parser";
import type { Block } from "./ast/node/block";
import { ExecutionPass } from "./pass/execution";
import { PassManager } from "./pass/manager";
import { registerFunctions } from "./environment";

const input = readFileSync(join(__dirname, "../test/dicts.mini"), "utf8");
const lexer = new Lexer(input);
const logger = new Logger();
const lexResult = lexer.lex(logger);
let block: Block | null = null;

if (lexResult.isSuccess) {
    const tokens = lexResult.getOrThrow();
    // console.log(tokens);
    const parser = new Parser(logger, tokens);
    block = parser.parse();
}

console.log(`Found ${logger.diagnostics.length} diagnostic(s)`);
for (const { type, message, location } of logger.diagnostics)
    console.log(`${DiagnosticType[type].toLowerCase()}(${location.line}:${location.column}): ${message}`);

if (block && logger.diagnostics.length <= 0) {
    registerFunctions(block);
    
    const passManager = new PassManager();
    passManager.register(new ExecutionPass());
    passManager.visit(block);
}