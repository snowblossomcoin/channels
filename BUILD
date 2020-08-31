package(default_visibility = ["//visibility:public"])

java_library(
  name = "channelslib",
  srcs = glob(["src/**/*.java", "src/*.java"]),
  deps = [
    "@snowblossom//client:client",
    "@snowblossom//lib:lib",
    "@snowblossom//node",
    "@snowblossom//iceleaf-ui:iceleaf",
    "@duckutil//:duckutil_lib",
    "@duckutil//:webserver_lib",
    "@duckutil//:duckutil_jsonrpc_lib",
    "@io_grpc_grpc_java//netty",
    "@maven//:junit_junit",
    "@maven//:net_minidev_json_smart",
    "@maven//:org_bouncycastle_bcpkix_jdk15on",
    "@maven//:org_bouncycastle_bcprov_jdk15on",
    "@maven//:org_bitlet_weupnp",
    "@maven//:io_netty_netty_handler",
    "@com_google_protobuf//:protobuf_java_util",
    ":protolib",
  ],
)

java_binary(
  name = "ChannelNode",
  main_class = "snowblossom.channels.ChannelNode",
  runtime_deps = [
    ":channelslib",
  ],
)

java_binary(
  name = "JsonTest",
  main_class = "snowblossom.channels.JsonTest",
  runtime_deps = [
    ":channelslib",
  ],
)


java_binary(
  name = "ChannelIceLeaf",
  main_class = "snowblossom.channels.iceleaf.ChannelIceLeaf",
  resources = [ "@snowblossom//iceleaf-ui:resources" ],
  runtime_deps = [
    ":channelslib",
  ]
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
      "@snowblossom//lib:lib",
      "@com_google_protobuf//:protobuf_java",
      ":channelslib",
    ],
)

java_test(
    name = "time_sem_test",
    srcs = ["test/TimeSemTest.java"],
    test_class = "channels.TimeSemTest",
    size="small",
    deps = [
      ":channelslib",
    ],
)

java_test(
    name = "cert_gen_test",
    srcs = ["test/CertGenTest.java"],
    test_class = "channels.CertGenTest",
    size="medium",
    deps = [
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      "@io_grpc_grpc_java//netty",
      "@maven//:io_netty_netty_handler",
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
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "sync_test",
    srcs = ["test/SyncTest.java"],
    test_class = "channels.SyncTest",
    size="medium",
    deps = [
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "cipher_channel_test",
    srcs = ["test/CipherChannelTest.java"],
    test_class = "channels.CipherChannelTest",
    size="medium",
    deps = [
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      "@com_google_protobuf//:protobuf_java_util",
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
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "web_server_test",
    srcs = ["test/WebServerTest.java", "test/TestUtil.java"],
    test_class = "channels.WebServerTest",
    size="medium",
    deps = [
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
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)

java_test(
    name = "chunk_map_util_test",
    srcs = ["test/ChunkMapUtilsTest.java"],
    test_class = "channels.ChunkMapUtilsTest",
    size="medium",
    deps = [
      "@snowblossom//lib:lib",
      "@snowblossom//client:client",
      "@duckutil//:duckutil_lib",
      ":channelslib",
      ":protolib",
    ],
)
java_test(
    name = "multipart_upload_test",
    srcs = ["test/MultipartUploadTest.java"],
    test_class = "channels.MultipartUploadTest",
    size="medium",
    deps = [
      "@snowblossom//lib:lib",
      ":channelslib",
      ":protolib",
    ],
)

