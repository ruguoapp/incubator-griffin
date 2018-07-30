#!/bin/bash

mvn clean install -DskipTests && \
aws s3 cp measure/target/measure-0.2.0-incubating-SNAPSHOT.jar s3://jike-data-warehouse/jars/griffin-measure.jar && \
aws s3 cp measure/src/main/resources/env.json s3://jike-data-warehouse/configurations/griffin/env.json && \
java -jar service/target/service-0.2.0-incubating-SNAPSHOT.jar
