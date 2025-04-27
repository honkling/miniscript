import { Location } from "../diagnostic/location";
import type { Token } from "./token";

export class TokenStream {
    public tokens: Token<any>[]

    constructor(
        tokens: Token<any>[],
        public location: Location = new Location(0, 1, 1)
    ) {
        this.tokens = tokens.filter((t) => t.type !== "Whitespace");
    }

    public consume(): Token<any> {
        const token = this.tokens[this.location.index++];

        if (!token)
            throw new Error("End of token stream");

        this.location.line = token.location.line;
        this.location.column = token.location.column;
        return token;
    }

    public peek(offset: number = 0): Token<any> {
        return this.tokens[this.location.index + offset];
    }

    public isEnd(): boolean {
        return this.location.index + 1 >= this.tokens.length;
    }
}