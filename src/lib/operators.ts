import { TokenType } from "../lexer/token";

export type Operator = {
    precedence: number;
    rightAssociated: boolean;
    identifier: keyof typeof TokenType;
};

export const operators: { [key in keyof typeof TokenType]?: Operator } = {
    "And": { identifier: "And", precedence: 0, rightAssociated: false },
    "Or": { identifier: "Or", precedence: 0, rightAssociated: false },
    "Equals": { identifier: "Equals", precedence: 1, rightAssociated: false },
    "NotEquals": { identifier: "NotEquals", precedence: 1, rightAssociated: false },
    "GreaterThan": { identifier: "GreaterThan", precedence: 1, rightAssociated: false },
    "GreaterEquals": { identifier: "GreaterEquals", precedence: 1, rightAssociated: false },
    "LessThan": { identifier: "LessThan", precedence: 1, rightAssociated: false },
    "LessEquals": { identifier: "LessEquals", precedence: 1, rightAssociated: false },
    "Plus": { identifier: "Plus", precedence: 2, rightAssociated: false },
    "Minus": { identifier: "Minus", precedence: 2, rightAssociated: false },
    "Multiply": { identifier: "Multiply", precedence: 3, rightAssociated: false },
    "Divide": { identifier: "Divide", precedence: 3, rightAssociated: false },
    "Period": { identifier: "Period", precedence: 4, rightAssociated: false }
};