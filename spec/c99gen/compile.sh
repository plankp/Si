#!/bin/sh

# Basically compiles every C file in the current directory in C99 mode
# Tries in gcc then clang

CFLAGS="-std=c99 -Og"

CC=$(which gcc)
if [ "0" != $? ]; then
    CC=$(which clang)
    if [ "0" != $? ]; then
        echo "Cannot find either gcc or clang... exiting"
        exit 1
    fi
fi

# Assume no code errored
errored=0
for f in ./*.c; do
    echo "Compiling $f with $CC"
    $CC -c $CFLAGS $f
    if [ "0" != $? ]; then
        errored=$(($?))
    fi
done

if [  0 -eq $(($errored)) ]; then
    exit 2
fi