export class Location {
    constructor(
        public index: number,
        public line: number,
        public column: number
    ) {}

    public clone(): Location {
        return new Location(this.index, this.line, this.column);
    }
}