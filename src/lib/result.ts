export class Result<T> {
    protected constructor(
        public isSuccess: boolean,
        private value: T | Error
    ) {}

    public static success<T>(value: T): Result<T> {
        return new Result(true, value);
    }

    public static failure<T>(error: Error): Result<T> {
        return new Result<T>(false, error);
    }

    public getOrThrow(): T {
        if (this.isSuccess)
            return this.value as T;

        throw new Error("Tried to fetch value for errorful result");
    }

    public errorOrThrow(): Error {
        if (this.isSuccess)
            throw new Error("Tried to fetch error for valueful result");

        return this.value as Error;
    }
}