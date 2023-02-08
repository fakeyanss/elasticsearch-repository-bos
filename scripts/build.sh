#!/usr/bin/env bash

cd $(git rev-parse --show-toplevel)

echo "Compile plugin repository-bos..."

mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -T 4 

echo "Build es docker image..."

docker build --platform linux/amd64 -f build/Dockerfile -t elasticsearch-with-repo-bos:7.6.2 .