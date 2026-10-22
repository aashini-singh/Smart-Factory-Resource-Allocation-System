#!/bin/sh
# Compile and run the Smart Factory Resource Allocation System (Linux / macOS)
cd "$(dirname "$0")" || exit 1
rm -rf out
javac -d out -sourcepath src src/smartfactory/main/FactoryApp.java || { echo "Compilation failed."; exit 1; }
java -cp out smartfactory.main.FactoryApp
