#!/bin/bash

native-image --no-fallback --allow-incomplete-classpath --initialize-at-build-time=org.slf4j.impl.SimpleLogger,org.slf4j.LoggerFactory,org.slf4j.impl.StaticLoggerBinder,org.minidns -jar build/libs/imisu.jar build/libs/imisu