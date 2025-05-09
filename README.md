# MiniScript

> [!WARNING]
> MiniScript is currently extremely experimental. Behavior may change at any time,
> features may be added or removed at any time. Expect no consistency.

MiniScript is a scripting language written in Kotlin. It's primarily built for Minecraft, so it can be used to rapidly develop servers.
However, MiniScript does remain platform agnostic and is not necessarily locked to running on Minecraft.

There currently isn't any language documentation available, but you can view the [test scripts](https://github.com/honkling/miniscript/tree/develop/src/test/resources) I'm using in development.

# Features

This is not necessarily a complete list, and can be expected to morph as does the scope of the project.
Feel free to suggest new features in the [Issues](https://github.com/honkling/miniscript/issues) tab.

- [x] Functions
- [x] Variables
- [x] Foreach/While Loops
- [x] Break/Continue
- [x] Primitives, arrays, dictionaries
- [x] If statements
- [x] Arithmetic
- [ ] Ternary expressions
- [ ] Classes/structs
- [ ] Core standard library
  - [ ] Networking (http, tcp, ws)
  - [ ] Persistent data storage
  - [ ] Reflection
  - [ ] Files
  - [ ] Locale (time/date, language)
  - [ ] Primitive utilities
- [ ] Minecraft plugin implementation
  - [ ] Skeleton implementation
  - [ ] Standard library for Minecraft
