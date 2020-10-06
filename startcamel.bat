echo off
start "hnclient-v2" /d hnclient-v2 cmd /k "mvn clean compile camel:run"
start "mock-hnsecure" /d mock-hnsecure cmd /k "mvn clean compile camel:run"
start "mock-point-of-service" /d mock-point-of-service cmd /k "echo Wait for Camel programs to start, then && pause && mvn clean compile exec:java"
