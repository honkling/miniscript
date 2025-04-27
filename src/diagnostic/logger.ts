import { Diagnostic, DiagnosticType } from "./diagnostic";
import { Location } from "./location";

type OptLocation = Location | [index: number, line: number, column: number];

export class Logger {
    public diagnostics: Diagnostic[] = [];

    public log(type: DiagnosticType, message: string, location: OptLocation) {
        this.diagnostics.push(new Diagnostic(type, message, this.process(location)));
    }

    public info(message: string, location: OptLocation) {
        this.log(DiagnosticType.Info, message, location);
    }

    public warning(message: string, location: OptLocation) {
        this.log(DiagnosticType.Warning, message, location);
    }

    public error(message: string, location: OptLocation) {
        this.log(DiagnosticType.Error, message, location);
    }

    public debug(message: string, location: OptLocation) {
        this.log(DiagnosticType.Debug, message, location);
    }

    private process(location: OptLocation): Location {
        if (location instanceof Location)
            return location;

        return new Location(location[0], location[1], location[2]);
    }
}