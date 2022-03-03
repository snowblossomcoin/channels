load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")


git_repository(
    name = "rules_jvm_external",
    remote = "https://github.com/bazelbuild/rules_jvm_external",
    commit = "ec2c5617b339844312d4adef4400dcc2ccb73c4f",
    shallow_since = "1614596935 +0000"
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

git_repository(
  name = "build_stack_rules_proto",
  remote = "https://github.com/fireduck64/rules_proto",
  commit = "3e0b10c45c5e15b3ee17b3aa8a7ffe6e16b018cc",
  shallow_since = "1614632955 -0800"
)

load("@build_stack_rules_proto//:deps.bzl", "io_grpc_grpc_java")
load("@build_stack_rules_proto//java:deps.bzl", "java_proto_compile")

io_grpc_grpc_java()
java_proto_compile()

load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

#grpc_java_repositories(omit_com_google_protobuf = True)
grpc_java_repositories()

load("@build_stack_rules_proto//java:deps.bzl", "java_grpc_library")

java_grpc_library()

maven_install(
    artifacts = [
        "com.google.protobuf:protobuf-java:3.5.1",
        "org.rocksdb:rocksdbjni:6.28.2",
        "junit:junit:4.12",
        "commons-codec:commons-codec:1.11",
        "org.apache.commons:commons-math3:3.6.1",
        "io.netty:netty-tcnative-boringssl-static:2.0.28.Final",
        "org.bouncycastle:bcprov-jdk15on:1.69",
        "org.bouncycastle:bcpkix-jdk15on:1.69",
        "org.bouncycastle:bcprov-jdk15to18:1.69",
        "com.thetransactioncompany:jsonrpc2-server:1.11",
        "net.minidev:json-smart:2.3",
        "com.lambdaworks:scrypt:1.4.0",
        "com.google.zxing:javase:3.4.0",
        "org.slf4j:slf4j-nop:1.7.25",
        "org.bitcoinj:bitcoinj-core:0.15.10",
  			"org.bitlet:weupnp:0.1.4",
        "io.netty:netty-handler:4.1.34.Final",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven.google.com",
    ],
    maven_install_json = "//:maven_install.json",
)
# After updating run:
# 
# bazel run @unpinned_maven//:pin
#
# See: https://github.com/bazelbuild/rules_jvm_external

load("@maven//:defs.bzl", "pinned_maven_install")
pinned_maven_install()

git_repository(
  name = "snowblossom",
  remote = "https://github.com/snowblossomcoin/snowblossom",
	commit = "88585336a6414f2965619de73218be3d1692bbd0",
  shallow_since = "1646110372 -0800"
)

git_repository(
  name = "duckutil",
  remote = "https://github.com/fireduck64/duckutil",
  commit = "61ed88b7f2dc5618fdb964e891500a08d49d6b2a",
  shallow_since = "1625642124 -0700",
)


