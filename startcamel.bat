echo off
start "hnclient-v2" /d hnclient-v2 cmd /c "mvn compile camel:run"
start "mock-hnsecure" /d mock-hnsecure cmd /c "mvn compile camel:run"
echo Wait for the Camel programs to finish starting, then press any key to send a message
pause
start "mock-point-of-service" /d mock-point-of-service cmd /k "mvn compile exec:java"