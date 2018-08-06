#!/bin/bash

tag=$(git rev-parse --short HEAD)
mvn clean install -DskipTests && \
aws s3 cp measure/target/measure-0.2.0-incubating-SNAPSHOT.jar s3://jike-data-warehouse/jars/griffin-measure.jar && \
aws s3 cp measure/src/main/resources/env.json s3://jike-data-warehouse/configurations/griffin/env.json && \
docker build -t "804775010343.dkr.ecr.cn-north-1.amazonaws.com.cn/griffin:$tag" --force-rm . && \
docker tag "804775010343.dkr.ecr.cn-north-1.amazonaws.com.cn/griffin:$tag" "804775010343.dkr.ecr.cn-north-1.amazonaws.com.cn/griffin:latest" && \
aws ecr get-login --no-include-email | bash && \
docker push "804775010343.dkr.ecr.cn-north-1.amazonaws.com.cn/griffin:$tag" && \
docker push "804775010343.dkr.ecr.cn-north-1.amazonaws.com.cn/griffin:latest"