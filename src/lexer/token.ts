import type { Location } from "../diagnostic/location";
import { ParseError } from "../parser/parser";

export const TokenType = {
    EndOfInput: { identifier: "EndOfInput" },
    Identifier: { identifier: "Identifier" },
    Whitespace: { identifier: "Whitespace" },

    Function: { identifier: "Function", value: "func" },
    For: { identifier: "For", value: "for" },
    ForEach: { identifier: "ForEach", value: "foreach" },
    If: { identifier: "If", value: "if" },
    While: { identifier: "While", value: "while" },
    StringType: { identifier: "StringType", value: "string" },
    NumberType: { identifier: "NumberType", value: "number" },
    BooleanType: { identifier: "BooleanType", value: "boolean" },
    VoidType: { identifier: "VoidType", value: "void" },
    AnyType: { identifier: "AnyType", value: "any" },
    Return: { identifier: "Return", value: "return" },
    Continue: { identifier: "Continue", value: "continue" },
    Break: { identifier: "Break", value: "break" },

    Number: { identifier: "Number" },
    Boolean: { identifier: "Boolean" },
    String: { identifier: "String" },

    OpenParen: { identifier: "OpenParen", value: "(" },
    CloseParen: { identifier: "CloseParen", value: ")" },
    OpenBracket: { identifier: "OpenBracket", value: "[" },
    CloseBracket: { identifier: "CloseBracket", value: "]" },
    OpenBrace: { identifier: "OpenBrace", value: "{" },
    CloseBrace: { identifier: "CloseBrace", value: "}" },
    And: { identifier: "And", value: "&&" },
    Or: { identifier: "Or", value: "||" },
    Colon: { identifier: "Colon", value: ":" },
    Comma: { identifier: "Comma", value: "," },
    Period: { identifier: "Period", value: "." },
    Arrow: { identifier: "Arrow", value: "->" },
    Equals: { identifier: "Equals", value: "==" },
    Assign: { identifier: "Assign", value: "=" },
    NotEquals: { identifier: "NotEquals", value: "!=" },
    GreaterEquals: { identifier: "GreaterEquals", value: ">=" },
    LessEquals: { identifier: "LessEquals", value: "<=" },
    GreaterThan: { identifier: "GreaterThan", value: ">" },
    LessThan: { identifier: "LessThan", value: "<" },
    PlusAssign: { identifier: "PlusAssign", value: "+=" },
    MinusAssign: { identifier: "MinusAssign", value: "-=" },
    MultiplyAssign: { identifier: "MultiplyAssign", value: "*=" },
    DivideAssign: { identifier: "DivideAssign", value: "/=" },
    Increment: { identifier: "Increment", value: "++" },
    Decrement: { identifier: "Decrement", value: "--" },
    Plus: { identifier: "Plus", value: "+" },
    Minus: { identifier: "Minus", value: "-" },
    Multiply: { identifier: "Multiply", value: "*" },
    Divide: { identifier: "Divide", value: "/" },
}

export type TokenTypeData = { identifier: string, value?: string };

export class Token<T = null> {
    constructor(
        public type: keyof typeof TokenType,
        public raw: string,
        public location: Location,
        public value: T
    ) {}

    public expect(
        type: keyof typeof TokenType,
        message: string = "Expected '{0}', found '{1}'"
    ): this {
        const expected = typeAsString(type);
        const actual = this.asString();

        if (this.type !== type)
            throw new ParseError(message.replace(/\{0}/g, expected)
                .replace(/\{1}/g, actual));

        return this;
    }

    public asString(): string {
        switch (this.type) {
            case "Boolean":
            case "Number": {
                return (this.value as any).toString();
            }
        }

        const data = TokenType[this.type] as TokenTypeData;
        return data.value ?? this.type;
    }
}

export function typeAsString(type: keyof typeof TokenType) {
    const data = TokenType[type] as TokenTypeData;
    return data.value ?? type;
}