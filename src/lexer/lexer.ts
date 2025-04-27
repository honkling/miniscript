import { Location } from "../diagnostic/location";
import { Logger } from "../diagnostic/logger";
import { Result } from "../lib/result";
import { Token, TokenType, type TokenTypeData } from "./token";

export class Lexer {
    private static tokenTypes
        = Object.entries(TokenType) as [keyof typeof TokenType, TokenTypeData][];

    constructor(
        public input: string,
        public location: Location = new Location(0, 1, 1)
    ) {}

    public lex(logger: Logger): Result<Token[]> {
        const tokens = [];

        while (this.location.index < this.input.length) {
            let result = this.getNextToken(logger);

            if (result)
                tokens.push(result);
            else {
                while (this.location.index < this.input.length && (result = this.getNextToken(logger)) === null)
                    this.advance();

                if (result)
                    tokens.push(result);
            }
        }

        if (logger.diagnostics.length > 0)
            return Result.failure(new Error());

        tokens.push(new Token("EndOfInput", "", this.location.clone(), null));
        return Result.success(tokens);
    }

    public getNextToken(logger: Logger): Token<any> | null {
        const start = this.location.clone();
        const input = this.input.substring(start.index);
        const character = input[0];

        // console.log("Getting new token", input);

        if (character.trim() === "") {
            const length = input.length - input.trimStart().length;
            this.advance(length);
            return new Token("Whitespace", input.substring(0, length), start, null);
        }

        if (Lexer.isLetter(character)) {
            let identifier = character;
            let char: string;

            while (Lexer.isLetterOrDigit(char = this.input.charAt(this.advance() + 1)))
                identifier += char;

            if (identifier === "true" || identifier === "false")
                return new Token<boolean>("Boolean", identifier, start, identifier === "true");

            for (const [key, value] of Lexer.tokenTypes)
                if (value.value === identifier)
                    return new Token(key, identifier, start, null);

            return new Token("Identifier", identifier, start, null);
        }

        if (Lexer.isDigit(character)) {
            let hasSeparator = false;
            let multiplier = 0.1;
            let raw = character;
            let value = this.digitToInt(character);
            let char;

            while (Lexer.isDigit(char = this.input.charAt(this.advance() + 1)) || (!hasSeparator && char === ".")) {
                if (char === ".") {
                    hasSeparator = true;
                    continue;
                }

                const digitValue = this.digitToInt(char);

                if (hasSeparator) {
                    value += digitValue * multiplier;
                    multiplier /= 10;
                } else {
                    value *= 10;
                    value += digitValue;
                }

                raw += char;
            }

            return new Token<number>("Number", raw, start, value);
        }

        if (character === "\"") {
            let raw = character;
            let char: string;
            let value = "";
            this.advance(1);

            while ((char = this.input.charAt(this.advance())) !== "\"") {
                raw += char;

                if (char === "\\") {
                    const next = this.input.charAt(this.advance());
                    raw += next;
                    value += next;
                } else {
                    value += char;
                }
            }

            return new Token<string>("String", raw, start, value);
        }

        for (const [key, value] of Lexer.tokenTypes)
            if (value.value && input.startsWith(value.value))
                return new Token(key, value.value, (this.advance(value.value.length), start), null);

        logger.error(`Unexpected character '${character}'`, this.location.clone());
        return null;
    }

    public static isLetter(character: string): boolean {
        const a = 97;
        const z = 122;
        const A = 65;
        const Z = 90;
        const code = character.charCodeAt(0);
        return (code >= a && code <= z) || (code >= A && code <= Z);
    }

    public static isDigit(character: string): boolean {
        const zero = 48;
        const nine = 57;
        const code = character.charCodeAt(0);
        return code >= zero && code <= nine;
    }

    public static isLetterOrDigit(character: string): boolean {
        return Lexer.isLetter(character) || Lexer.isDigit(character);
    }

    private digitToInt(character: string): number {
        const zero = 48;
        const code = character.charCodeAt(0);
        return code - zero;
    }

    private advance(amount: number = 1): number {
        const oldIndex = this.location.index;
        const advancement = this.input.substring(this.location.index, this.location.index + amount);
        this.location.index += amount;
        
        for (const char of advancement)
            if (char === "\n") {
                this.location.line++;
                this.location.column = 1;
            } else this.location.column++;

        return oldIndex;
    }
}