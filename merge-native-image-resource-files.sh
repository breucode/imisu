#!/bin/bash

./gradlew clean shadowJar
java -agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/ -jar build/libs/imisu.jar