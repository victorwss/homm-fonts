@echo off
del *.class 2> NUL
javac FontWorker.java
java FontWorker %*
del *.class
@echo on