#!/bin/bash

native-image --no-fallback --allow-incomplete-classpath --initialize-at-build-time=org.minidns,org.slf4j -jar build/libs/imisu-0.0.1-all.jar build/libs/imisu-0.0.1-all