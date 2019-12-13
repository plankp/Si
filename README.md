# Si [![Build Status](https://www.travis-ci.org/plankp/Si.svg?branch=master)](https://www.travis-ci.org/plankp/Si)

Don't worry! This project has nothing to do with actual Silicon...

## What is this?

This is a programming language. Name is a pun of C.

In the past, my projects were either purely translated, interpreted, or compiled.
This time, I want to explore something similar to `constexpr` in C++.
In other words, add constructs to allow code to be executed during compile time.
I also want to experiment more with side effect free functions.

## How will it look?

This project is still very much in the prototype phase, therefore it's hard to say.
Whatever is in the `spec/` directory is how it looks at the moment.

## How to build?

Use the gradle wrapper:

* `./gradlew test` to test
* `./gradlew build` to build
* `./gradlew assemble` to create a distributable zip or tarball
* `./gradlew run` to run, must supply arguments via `--args` (see [How to use?](#how-to-use))

Requires at least JDK 8 to be installed

## How to use?

```
usage: Si [options...] file
options:
 -h, --help         Print this help message
 -o <file>          Write output to <file>
 --stdout           Write output to standard output stream
 --emit-ir          Emit internal representation (default)
 --emit-c99         Emit C99 code
 -e <func>          Specifies the entry point, must have signature ()int8
 -t                 Premature optimize code
```

:arrow_up: _The output you will get by running `-h` or `--help`_

Also, if you invoke through the gradle wrapper, you would do (***notice the leading space***):

```bash
# This compiles ./spec/funcs.si to C99 code and prints it
./gradlew run --args ' --emit-c99 --stdout ./spec/funcs.si'
```

## What license?

MPL 2.0