package(default_visibility = ["//visibility:public"])

java_library(
  name = "channelslib",
  srcs = glob(["src/**/*.java", "src/*.java"]),
  deps = [
    "@snowblossom//client:client",
    "@snowblossom//lib:lib",
    "@duckutil//:duckutil_lib",
    "@duckutil//:duckutil_jsonrpc_lib",
    "@junit_junit//jar",
    "@bcprov//jar",
    "@bcpkix//jar",
    "@weupnp//jar",
    ":protolib",
    "@netty_tcnative//jar",
    "@build_stack_rules_proto//java:grpc_netty",
    "@io_netty_netty_handler//jar",
    "@io_grpc_grpc_java//netty",
    "@com_google_protobuf//:protobuf_java",
  ],
)

java_binary(
  name = "ChannelNode",
  main_class = "snowblossom.channels.ChannelNode",
  runtime_deps = [
    ":channelslib",
  ],
)

proto_library(
  name = "protosrc",
  srcs = glob(["protolib/*.proto", "protolib/**/*.proto"]),
  visibility = [
    "//visibility:public",
  ],
  deps = ["@snowblossom//protolib:protosrc"]
)


load("@build_stack_rules_proto//java:java_grpc_library.bzl", "java_grpc_library")


java_grpc_library(
  name = "protolib",
  deps = [":protosrc"],
)

java_test(
    name = "hash_math_test",
    srcs = ["test/HashMathTest.java"],
    test_class = "channels.HashMathTest",
    size="small",
    deps = [
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      ":channelslib",
      "@com_google_protobuf//:protobuf_java",
    ],
)

java_test(
    name = "cert_gen_test",
    srcs = ["test/CertGenTest.java"],
    test_class = "channels.CertGenTest",
    size="medium",
    deps = [
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      "@io_netty_netty_handler//jar",
      "@io_grpc_grpc_java//netty",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "channel_sigutil_test",
    srcs = ["test/ChannelSigUtilTest.java"],
    test_class = "channels.ChannelSigUtilTest",
    size="medium",
    deps = [
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "dht_test",
    srcs = ["test/DHTTest.java"],
    test_class = "channels.DHTTest",
    size="medium",
    deps = [
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "channel_peer_test",
    srcs = ["test/ChannelPeerTest.java", "test/TestUtil.java"],
    test_class = "channels.ChannelPeerTest",
    size="medium",
    deps = [
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "channel_data_test",
    srcs = ["test/ChannelDataTest.java", "test/TestUtil.java"],
    test_class = "channels.ChannelDataTest",
    size="small",
    deps = [
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)



java_test(
    name = "channel_test",
    srcs = ["test/ChannelTest.java"],
    test_class = "channels.ChannelTest",
    size="small",
    deps = [
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "channel_validation_test",
    srcs = ["test/ChannelValidationTest.java", "test/TestUtil.java"],
    test_class = "channels.ChannelValidationTest",
    size="medium",
    deps = [
      "@junit_junit//jar",
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

