import { type Token } from "../lexer/token";
import { TokenStream } from "../lexer/stream";
import { Block } from "../ast/node/block";
import type { Standalone } from "../ast/node/statement";
import type { Logger } from "../diagnostic/logger";
import { Assignment } from "../ast/tree/statement/assignment";
import { Location } from "../diagnostic/location";
import { Value } from "../ast/node/value";
import { ComponentString, StringComponent } from "../ast/node/string";
import type { Dependent, ExpressionParent } from "../ast/node/expression";
import { Lexer } from "../lexer/lexer";
import { ValueExpression } from "../ast/tree/expression/value";
import { Type } from "../ast/node/type";
import { Parameter } from "../ast/tree/function/parameter";
import { Lambda } from "../ast/node/lambda";
import { FunctionCall } from "../ast/tree/duplex/functionCall";
import { FunctionDeclaration } from "../ast/tree/statement/functionDeclaration";
import { Function } from "../ast/tree/function/function";
import { Return } from "../ast/tree/statement/return";
import { Loop } from "../ast/tree/statement/loop";
import type { Node } from "../ast/node/node";
import { operators, type Operator } from "../lib/operators";
import { Arithmetic } from "../ast/tree/expression/arithmetic";
import { tokenToString } from "typescript";
import { Condition } from "../ast/tree/statement/condition";
import { Continue } from "../ast/tree/statement/continue";
import { Break } from "../ast/tree/statement/break";

export class ParseError extends Error {}

export class Parser {
    private stream: TokenStream;

    constructor(
        public logger: Logger,
        tokens: Token[],
        location: Location = new Location(0, 1, 1)
    ) {
        this.stream = new TokenStream(tokens, location);
    }

    public parse(): Block {
        return this.parseBlock(null, false);
    }

    private parseLambda(parent: ExpressionParent | null): Lambda {
        this.stream.consume().expect("OpenBrace");
        let hasParameters = false;
        const parameters = [];

        let token: Token;
        let i = 0;
        while ((token = this.stream.peek(i++)).type !== "CloseBrace")
            if (token.type === "Arrow") {
                hasParameters = true;
                break;
            }

        if (hasParameters)
            while (this.stream.peek().type !== "Arrow") {
                const name = this.stream.consume().expect("Identifier", "Expected parameter name");
                let defaultValue: Dependent<any> | null = null;
                let type: Type<any> | null = null;

                if (this.stream.peek().type === "Colon") {
                    this.stream.consume();
                    type = this.parseType(null);
                }

                if (this.stream.peek().type === "Assign") {
                    this.stream.consume();
                    defaultValue = this.parseExpression(null);
                }

                const parameter = new Parameter(name.raw, type, defaultValue, null);

                if (type)
                    type.parent = parameter;
                
                if (defaultValue)
                    defaultValue.parent = parameter;

                parameters.push(parameter);
            }

        const block = this.parseBlock(null, false);
        this.stream.consume().expect("CloseBrace");
        const lambda = new Lambda(parameters, block, parent);
        block.parent = lambda;

        for (const parameter of parameters)
            parameter.parent = lambda;

        return lambda;
    }

    private parseBlock(parent: Node<any> | null, expectBraces: boolean = true): Block {
        let hasBraces = true;

        if (expectBraces) {
            if (this.stream.peek().type !== "OpenBrace")
                hasBraces = false;
            else this.stream.consume();
        }

        const statements: Standalone[] = [];
        const block = new Block(statements, parent);

        while (!this.stream.isEnd() && this.stream.peek().type !== "CloseBrace") {
            try {
                const statement = this.parseStatement(block);
                statements.push(statement);

                if (!hasBraces)
                    break;
            } catch (e) {
                if (e instanceof ParseError)
                    this.recover();
                else throw e;
            }
        }

        if (expectBraces && hasBraces)
            this.stream.consume().expect("CloseBrace");

        return block;
    }

    private parseStatement(block: Block): Standalone {
        const token = this.stream.peek();

        switch (token.type) {
            case "Return":
                return this.parseReturn(block);
            case "Function":
                return this.parseFunctionDeclaration(block);
            case "Identifier":
                return this.parseIdentifierStatement(block);
            case "ForEach":
                return this.parseForEach(block);
            case "While":
                return this.parseWhile(block);
            case "If":
                return this.parseCondition(block);
            case "Continue":
                this.stream.consume();
                return new Continue(block);
            case "Break":
                this.stream.consume();
                return new Break(block);
            default: {
                this.logger.error("Unexpected statement", this.location());
                throw new ParseError();
            }
        }
    }

    private parseCondition(parent: Block): Condition {
        this.stream.consume().expect("If");
        this.stream.consume().expect("OpenParen");

        const expression = this.parseExpression(null);
        this.stream.consume().expect("CloseParen");
        const block = this.parseBlock(null);

        const statement = new Condition(expression, block, parent);
        expression.parent = statement;
        block.parent = statement;
        return statement;
    }

    private parseWhile(parent: Block): Loop {
        this.stream.consume().expect("While");
        this.stream.consume().expect("OpenParen");

        const expression = this.parseExpression(null);
        this.stream.consume().expect("CloseParen");

        const block = this.parseBlock(null);
        const statement = new Loop.While(expression, block, parent);
        expression.parent = statement;
        block.parent = statement;
        return statement;
    }

    private parseForEach(parent: Block): Loop {
        this.stream.consume().expect("ForEach");
        this.stream.consume().expect("OpenParen");

        const name = this.stream.consume().expect("Identifier", "Expected variable name for loop, found '{1}'");
        this.stream.consume().expect("Colon");
        const value = this.parseExpression(null);
        this.stream.consume().expect("CloseParen");
        const block = this.parseBlock(null);

        const statement = new Loop.ForEach(name.raw, value, block, parent);
        value.parent = statement;
        block.parent = statement;
        return statement;
    }

    private parseReturn(block: Block): Return {
        this.stream.consume().expect("Return");
        const value = this.parseExpression(null);
        const statement = new Return(value, block);
        value.parent = statement;
        return statement;
    }

    public parseFunctionDeclaration(parent: Block, expectBlock: boolean = true): FunctionDeclaration {
        this.stream.consume().expect("Function");
        const name = this.stream.consume().expect("Identifier", "Expected function name, found '{1}'");
        this.stream.consume().expect("OpenParen");
        const parameters: Parameter[] = [];
        let returnType: Type<any> | null = null;

        while (this.stream.peek().type !== "CloseParen") {
            const name = this.stream.consume().expect("Identifier", "Expected function parameter name, found '{1}'");
            this.stream.consume().expect("Colon");
            const type = this.parseType(null);
            let defaultValue: Dependent<any> | null = null;

            if (this.stream.peek().type === "Assign") {
                this.stream.consume();
                defaultValue = this.parseExpression(null);
            }

            const parameter = new Parameter(name.raw, type, defaultValue, null);
            type.parent = parameter;

            if (defaultValue)
                defaultValue.parent = parameter;

            if (this.stream.peek().type !== "CloseParen")
                this.stream.consume().expect("Comma", "Expected ')' or ',', found '{1}'");

            parameters.push(parameter);
        }

        this.stream.consume().expect("CloseParen");

        if (this.stream.peek().type !== "OpenBrace") {
            if (this.stream.peek().type !== "EndOfInput" && expectBlock) {
                this.stream.consume().expect("Arrow", "Expected '->' or '{', found '{1}'");
                returnType = this.parseType(null);
            }
        }

        const block = expectBlock ? this.parseBlock(null) : new Block([], null);

        const lambda = new Lambda(parameters, block, null);
        const func = new Function(name.raw, lambda, returnType, null);
        block.parent = lambda;
        lambda.parent = func;
        
        if (returnType)
            returnType.parent = func;

        for (const parameter of lambda.parameters)
            parameter.parent = lambda;

        const declaration = new FunctionDeclaration(func, parent);
        func.parent = declaration;
        return declaration;
    }

    private parseType(parent: any): Type<any> {
        const token = this.stream.consume();
        let type: Type<any>;

        switch (token.type) {
            case "StringType": {
                type = new Type.String(parent);
                break;
            }    
            case "NumberType": {
                type = new Type.Number(parent);
                break;
            }
            case "BooleanType": {
                type = new Type.Boolean(parent);
                break;
            }
            case "VoidType": {
                type = new Type.Void(parent);
                break;
            }
            case "AnyType": {
                type = new Type.Any(parent);
                break;
            }
            case "OpenParen": {
                const parameters: Parameter[] = []; 
                let i = 1;

                while (this.stream.peek().type !== "CloseParen") {
                    let name = `arg${i}`;

                    if (this.stream.peek().type === "Identifier") {
                        name = this.stream.consume().raw;
                        this.stream.consume().expect("Colon");
                    }

                    const type = this.parseType(parent);
                    let defaultValue = null;

                    if (this.stream.peek().type === "Assign")
                        defaultValue = this.parseExpression(null)

                    const parameter = new Parameter(name, type, defaultValue, null);
                    parameters.push(parameter);
                }

                this.stream.consume().expect("CloseParen");
                this.stream.consume().expect("Arrow");
                const returnType = this.parseType(null);
                const lambda = new Type.Lambda(parameters, returnType, null);
                returnType.parent = lambda;
                for (const parameter of parameters)
                    parameter.parent = lambda;

                type = lambda;
                break;
            }
            default: {
                this.logger.error(`Expected type, found '${token.asString()}'`, this.location());
                throw new ParseError();
            }
        }

        if (this.stream.peek().type === "OpenBracket") {
            this.stream.consume();
            this.stream.consume().expect("CloseBracket");
            type.isArray = true;
        }

        return type;
    }

    private parseExpression(parent: ExpressionParent | null): Dependent<any> {
        return this.parseArithmetic(this.parsePrimaryExpression(null), 0, parent);
    }

    private parseArithmetic(lhs: Dependent<any>, minPrecedence: number, parent: ExpressionParent | null): Dependent<any> {
        let lookahead = this.stream.peek();
        let operatorInfo: Operator | undefined;

        while ((operatorInfo = operators[lookahead.type]) && operatorInfo.precedence >= minPrecedence) {
            const operator = lookahead;
            this.stream.consume();

            let rhs = this.parsePrimaryExpression(null);

            lookahead = this.stream.peek();

            let lookaheadOperator = operators[lookahead.type];
            let greaterPrecedence = lookaheadOperator && lookaheadOperator.precedence > operatorInfo.precedence;
            while (lookaheadOperator && (greaterPrecedence || (lookaheadOperator.rightAssociated
                && lookaheadOperator.precedence === operatorInfo.precedence))) {
                    rhs = this.parseArithmetic(rhs, operatorInfo.precedence + (greaterPrecedence ? 1 : 0), null);
                    lookahead = this.stream.peek();
                    lookaheadOperator = operators[lookahead.type];
                    greaterPrecedence = lookaheadOperator && lookaheadOperator.precedence > operatorInfo.precedence;
                }

            const oldLhs = lhs;
            lhs = new Arithmetic(oldLhs, rhs, operators[operator.type]!, null);
            oldLhs.parent = lhs;
            rhs.parent = lhs;
        }

        lhs.parent = parent;
        return lhs;
    }

    private parsePrimaryExpression(parent: ExpressionParent | null): Dependent<any> {
        let expression = this.parseOneExpression(parent);

        switch (this.stream.peek().type) {
            case "Period": {
                this.stream.consume();
                const name = this.stream.consume().expect("Identifier", "Expected member name, found '{1}'");
                const expr = new ValueExpression(new Value.Identifier(name.raw), null);
                const oldExpr = expression;
                expression = new Arithmetic(expression, expr, operators.Period!, null);
                oldExpr.parent = expression;
                expr.parent = expression;
                break;
            }
            case "OpenBracket": {
                this.stream.consume();
                const expr = this.parseExpression(null);
                this.stream.consume().expect("CloseBracket", "Expected closing bracket, found '{1}'");
                const oldRhs = expression;
                expression = new Arithmetic(expression, expr, operators.Period!, null);
                oldRhs.parent = expression;
                expr.parent = expression;
                break;
            }
        }

        return expression;
    }

    private parseOneExpression(parent: ExpressionParent | null): Dependent<any> {
        const token = this.stream.peek();

        if (token.type === "OpenParen") {
            this.stream.consume();
            const expr = this.parseExpression(parent);
            this.stream.consume().expect("CloseParen", "Expected closing parentheses, found '{1}'")
            return expr;
        }

        if (this.stream.peek().type === "Identifier" && this.stream.peek(1).type === "OpenParen")
            return this.parseFunctionCall(parent);

        const value = this.parseValue(null);
        const expression = new ValueExpression(value, parent);
        value.parent = expression;
        return expression;
    }

    private parseIdentifierStatement(block: Block): Standalone {
        const next = this.stream.peek(1);

        switch (next.type) {
            case "PlusAssign":
            case "MinusAssign":
            case "MultiplyAssign":
            case "DivideAssign":
            case "Increment":
            case "Decrement":
            case "Assign": return this.parseAssignment(block);
            case "OpenParen": return this.parseFunctionCall(block);
            default: {
                this.logger.error(`Unexpected token, expected one of '=', '+=', '-=', '*=', '/=', or '(', found '${next.asString()}'`, this.location());
                throw new ParseError();
            }
        }
    }

    private parseFunctionCall(parent: Block | ExpressionParent | null): FunctionCall {
        const name = this.stream.consume().expect("Identifier");
        this.stream.consume().expect("OpenParen");
        const args: Dependent<any>[] = [];
        let lambda: Lambda | null = null;

        while (this.stream.peek().type !== "CloseParen") {
            const expression = this.parseExpression(null);

            if (this.stream.peek().type !== "CloseParen")
                this.stream.consume().expect("Comma");

            args.push(expression);
        }

        this.stream.consume().expect("CloseParen");
        
        if (this.stream.peek().type === "OpenBrace")
            lambda = this.parseLambda(null);

        const duplex = new FunctionCall(name.raw, args, lambda, parent);
        
        if (lambda)
            lambda.parent = duplex;

        for (const arg of args)
            arg.parent = duplex;

        return duplex;
    }

    private parseAssignment(block: Block): Assignment {
        const name = this.stream.consume().expect("Identifier", "Expected function name, found '{1}'");
        const token = this.stream.consume();

        if (!["Assign", "PlusAssign", "MinusAssign", "MultiplyAssign", "DivideAssign", "Increment", "Decrement"].includes(token.type)) {
            this.logger.error(`Expected one of '=', '+=', '-=', '*=', '/=', '++', or '--'. Found '${token.asString()}'`, this.location());
            throw new ParseError();
        }

        if (token.type === "Increment" || token.type === "Decrement")
            return new Assignment(name.raw, null, token, block);

        const expression = this.parseExpression(null);
        const assignment = new Assignment(name.raw, expression, token, block);
        expression.parent = assignment;
        return assignment;
    }

    private parseValue(parent: ExpressionParent | null): Value<any> {
        const token = this.stream.peek();

        switch (token.type) {
            case "Number":
                this.stream.consume();
                return new Value.Number(token.value);
            case "Boolean":
                this.stream.consume();
                return new Value.Boolean(token.value);
            case "Identifier":
                this.stream.consume();
                return new Value.Identifier(token.raw);
            case "String": {
                const string = this.parseString(token);
                const expr = new Value.String(string);
                string.parent = expr;
                return expr;
            }
            case "OpenBracket": {
                const values = this.parseArray();
                const expr = new Value.Array(values);

                for (const value of values)
                    value.parent = expr;
                
                return expr;
            }
            case "OpenBrace": {
                return new Value.Lambda(this.parseLambda(parent!));
            }
            default: {
                this.logger.error(`Expected a value, found '${token.asString()}'`, this.location());
                throw new ParseError();
            }
        }
    }

    private parseArray(): Dependent<any>[] {
        this.stream.consume().expect("OpenBracket");
        const values: Dependent<any>[] = [];

        while (this.stream.peek().type !== "CloseBracket") {
            const expression = this.parseExpression(null);
            values.push(expression);

            if (this.stream.peek().type !== "CloseBracket")
                this.stream.consume().expect("Comma", "Expected ',' or ']', found '{1}'");
        }

        this.stream.consume().expect("CloseBracket");

        return values;
    }

    private recover() {
        const recoveryTokens = ["EndOfInput", "Function", "If", "For", "ForEach", "While", "CloseBrace"];
        let token: Token;

        while (!this.stream.isEnd() && !recoveryTokens.includes((token = this.stream.peek()).type)) {
            if (token.type === "Identifier" && this.stream.peek(1).type === "Assign")
                break;

            this.stream.consume();
        }
    }

    private parseString({ value: raw }: Token<string>): ComponentString {
        this.stream.consume();
        const string = new ComponentString(null);
        let success = true;
        let value = "";
        let i = 0;

        while (i < raw.length) {
            const character = raw[i++];

            if (character === "$") {
                const next = raw[i];

                if (value !== "") {
                    string.addComponent(new StringComponent.Raw(value, string));
                    value = "";
                }

                if (next === "{") {
                    let level = 0;
                    i++;

                    while (i < raw.length && (raw[i] !== "}" || level > 0)) {
                        const character = raw[i++];
                        value += character;

                        if (character === "{")
                            level++;
                        else if (character === "}")
                            level--;
                    }

                    if (raw[i++] !== "}") {
                        this.logger.error("Expected '}' for end of expression in string, found nothing", this.location());
                        throw new ParseError();
                    }

                    const { location: { line, column } } = this.stream;
                    const lexer = new Lexer(value, new Location(0, line, column));
                    const lexResult = lexer.lex(this.logger);

                    if (!lexResult.isSuccess) {
                        success = false;
                        value = "";
                        continue;
                    }

                    const parser = new Parser(this.logger, lexResult.getOrThrow(), new Location(0, line, column));
                    const expression = parser.parseExpression(string);
                    string.addComponent(new StringComponent.Expression(expression, string));
                    value = "";
                    continue;
                }

                let char: string;

                while ((char = raw[i], i) < raw.length && ((Lexer.isLetter(raw[i]) && i === 0) || Lexer.isLetterOrDigit(char))) {
                    value += char;
                    i++;
                }

                if (value === "") {
                    const found = i >= raw.length ? "nothing" : `'${raw[i]}'`
                    this.logger.error(`Expected variable name, found ${found}`, this.location());
                    success = false;
                    continue;
                }

                string.addComponent(new StringComponent.Identifier(value, string));
                value = "";
                continue;
            }

            value += character;
        }

        if (value !== "")
            string.addComponent(new StringComponent.Raw(value, string));

        if (success)
            return string;

        throw new ParseError();
    }

    private location(): Location {
        return this.stream.location.clone();
    }
}