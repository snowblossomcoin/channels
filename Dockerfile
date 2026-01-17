FROM l.gcr.io/google/bazel AS build

RUN add-apt-repository ppa:openjdk-r/ppa
RUN apt-get update
RUN apt-get install -y --no-install-recommends openjdk-17-jdk-headless

COPY MODULE.bazel /channels/MODULE.bazel
COPY MODULE.bazel.lock /channels/MODULE.bazel.lock
COPY BUILD /channels/BUILD
COPY src /channels/src
COPY protolib /channels/protolib
COPY maven_install.json /channels/maven_install.json
WORKDIR /channels
RUN bazel build :ChannelNode_deploy.jar

FROM debian:stable AS run
RUN apt-get update
RUN apt-get install -y --no-install-recommends default-jre-headless
COPY --from=build /channels/bazel-bin/ChannelNode_deploy.jar /channels/
COPY docker/node.conf /channels/node.conf

CMD java -Xmx4g -jar /channels/ChannelNode_deploy.jar /channels/node.conf



