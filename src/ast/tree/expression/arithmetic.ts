import type { Token } from "../../../lexer/token";
import type { Operator } from "../../../lib/operators";
import type { Pass } from "../../../pass/pass";
import { Expression, type Dependent, type ExpressionParent } from "../../node/expression";

export class Arithmetic extends Expression<any> {
    constructor(
        public left: Dependent<any>,
        public right: Dependent<any>,
        public operator: Operator,
        parent: ExpressionParent | null
    ) {
        super(parent);
    }

    public static evaluate(left: any, right: any, operator: Operator): any {
        switch (operator.identifier) {
            case "Period": {
                if (Array.isArray(left))
                    if (typeof right === "number")
                        return left[right];
                    else throw new Error("Arrays only accept number indices.");
                else if (left.constructor === Object)
                    return left[right];

                throw new Error("Invalid object access.");
            }
            case "And":
            case "Or":
                const isAnd = operator.identifier === "And";

                if (typeof left !== "boolean" || typeof right !== "boolean")
                    throw new Error(`Cannot compare a ${typeof left} with a ${typeof right} using logical ${isAnd ? "AND" : "OR"}, expected two booleans`);

                return isAnd ? (left && right) : (left || right);
            case "Plus":
            case "PlusAssign":
            case "Minus":
            case "MinusAssign":
                const isPlus = operator.identifier === "Plus";

                if (typeof left === "string" || typeof right === "string")
                    if (isPlus)
                        return left + right;
                    else throw new Error("A string cannot be subtracted from");

                if (typeof left === "number" && typeof right === "number") {
                    const value = isPlus ? right : -right;
                    return left + value;
                } else {
                    const leftIsArray = Array.isArray(left);
                    const rightIsArray = Array.isArray(right);

                    if (leftIsArray && rightIsArray)
                        return [...left, ...right];
                    else if (leftIsArray && !rightIsArray)
                        return [...left, right];
                    else if (!leftIsArray && rightIsArray)
                        return [left, ...right];
                    else throw new Error(`'${left}' and '${right}' can not be ${isPlus ? "added" : "subtracted"}`);
                }
            case "Multiply":
            case "MultiplyAssign":
                if (typeof left === "number" && typeof right === "number")
                    return left * right;

                throw new Error("Unsupported operation");
            case "Divide":
            case "DivideAssign":
                if (typeof left === "number" && typeof right === "number")
                    return left / right;

                throw new Error("Unsupported operation");
            case "Equals": return left === right;
            case "NotEquals": return left !== right;
            case "GreaterThan": return left > right;
            case "GreaterEquals": return left >= right;
            case "LessThan": return left < right;
            case "LessEquals": return left <= right;
            default: throw new Error("Unsupported operation");
        }
    }

    public get() {
        const left = this.left.get();

        if (this.operator.identifier === "Or" && left)
            return true;
        
        if (this.operator.identifier === "And" && !left)
            return false;

        const right = this.right.get();

        return Arithmetic.evaluate(left, right, this.operator);
    }

    public visit(pass: Pass): void {
        pass.visitArithmetic(this);
    }
}