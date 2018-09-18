package(default_visibility = ["//visibility:public"])

load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_library")

java_library(
  name = "channelslib",
  srcs = glob(["src/**/*.java", "src/*.java"]),
  deps = [
    "@snowblossom//client:client",
    "@snowblossom//lib:lib",
    "@duckutil//:duckutil_lib",
    "@duckutil//:duckutil_jsonrpc_lib",
    "@org_pubref_rules_protobuf//java:grpc_compiletime_deps",
    "@junit_junit//jar",
    "@snowblossom//protolib",
    "@bcprov//jar",
    "@bcpkix//jar",
    ":protolib",
    "@netty_tcnative//jar",
  ],
)

java_binary(
  name = "ChannelNode",
  main_class = "snowblossom.channels.ChannelNode",
  runtime_deps = [
    ":channelslib",
  ],
)

java_proto_library(
  name = "protolib",
  protos = glob(["protolib/*.proto", "protolib/**/*.proto"]),
  with_grpc = True,
  deps = [
  ],
  verbose = 1,
)

java_test(
    name = "hash_math_test",
    srcs = ["test/HashMathTest.java"],
    test_class = "channels.HashMathTest",
    size="small",
    deps = [
      "@org_pubref_rules_protobuf//java:grpc_compiletime_deps",
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      ":channelslib",
    ],
)

java_test(
    name = "cert_gen_test",
    srcs = ["test/CertGenTest.java"],
    test_class = "channels.CertGenTest",
    size="medium",
    deps = [
      "@org_pubref_rules_protobuf//java:grpc_compiletime_deps",
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

