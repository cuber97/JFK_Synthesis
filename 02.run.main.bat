@REM Make sure java.exe is JDK, not JRE, so tools.jar is available!
java.exe -classpath "lib\javaparser-core-3.4.0.jar;out\production\Synthesis" pl.edu.wat.Main
java.exe -classpath "out\production\Synthesis" ClassAltered
