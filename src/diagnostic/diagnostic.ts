import type { Location } from "./location";

export enum DiagnosticType {
    Info,
    Warning,
    Error,
    Debug
}

export class Diagnostic {
    constructor(
        public type: DiagnosticType,
        public message: string,
        public location: Location
    ) {}
}