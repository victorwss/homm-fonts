# homm-fonts
Tool to work with fonts of the Heroes of Might and Magic games.

This tool exports the games .FNT files to a .PNG files (also usable with different image formats). A companion TXT file containing metadata (mostly data about the spacing around each character) is also produced. It also might be used to import the image file back into the PNG file.

There is a RunWorker.bat file to compile and run it. Runing it without command line parameters output the usage instructions. Refer to those for complete details about how to use this tool.

Finally, this tool was developed in Java 13 (although it probably runs in Java 10). As long as you have a properly installed working JDK, the RunWorker.bat is responsible for compiling the program, running it and cleaning up all the stuff without you having to further worry about Java.