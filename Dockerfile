FROM l.gcr.io/google/bazel as build

RUN add-apt-repository ppa:openjdk-r/ppa
RUN apt-get update
RUN apt-get install -y --no-install-recommends openjdk-17-jdk-headless

COPY WORKSPACE /channels/WORKSPACE
COPY BUILD /channels/BUILD
COPY src /channels/src
COPY protolib /channels/protolib
COPY maven_install.json /channels/maven_install.json
WORKDIR /channels
RUN bazel build :ChannelNode_deploy.jar

FROM openjdk:17-slim as run
COPY --from=build /channels/bazel-bin/ChannelNode_deploy.jar /channels/
COPY docker/node.conf /channels/node.conf

CMD java -Xmx4g -jar /channels/ChannelNode_deploy.jar /channels/node.conf



