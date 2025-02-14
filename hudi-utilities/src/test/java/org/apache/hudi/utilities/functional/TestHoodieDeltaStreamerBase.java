/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.utilities.functional;

import org.apache.hudi.DataSourceWriteOptions;
import org.apache.hudi.common.config.TypedProperties;
import org.apache.hudi.common.testutils.HoodieTestDataGenerator;
import org.apache.hudi.hive.MultiPartKeysValueExtractor;
import org.apache.hudi.utilities.testutils.UtilitiesTestBase;

import org.apache.avro.Schema;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.streaming.kafka010.KafkaTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.Random;

public class TestHoodieDeltaStreamerBase extends UtilitiesTestBase {


  static final Random RANDOM = new Random();
  static final String PROPS_FILENAME_TEST_SOURCE = "test-source.properties";
  static final String PROPS_FILENAME_TEST_SOURCE1 = "test-source1.properties";
  static final String PROPS_INVALID_HIVE_SYNC_TEST_SOURCE1 = "test-invalid-hive-sync-source1.properties";
  static final String PROPS_INVALID_FILE = "test-invalid-props.properties";
  static final String PROPS_INVALID_TABLE_CONFIG_FILE = "test-invalid-table-config.properties";
  static final String PROPS_FILENAME_TEST_INVALID = "test-invalid.properties";
  static final String PROPS_FILENAME_TEST_CSV = "test-csv-dfs-source.properties";
  static final String PROPS_FILENAME_TEST_PARQUET = "test-parquet-dfs-source.properties";
  static final String PROPS_FILENAME_TEST_JSON_KAFKA = "test-json-kafka-dfs-source.properties";
  static final String PROPS_FILENAME_TEST_MULTI_WRITER = "test-multi-writer.properties";
  static final String FIRST_PARQUET_FILE_NAME = "1.parquet";
  static String PARQUET_SOURCE_ROOT;
  static String JSON_KAFKA_SOURCE_ROOT;
  static final int PARQUET_NUM_RECORDS = 5;
  static final int CSV_NUM_RECORDS = 3;
  static final int JSON_KAFKA_NUM_RECORDS = 5;
  String kafkaCheckpointType = "string";
  // Required fields
  static final String TGT_BASE_PATH_PARAM = "--target-base-path";
  static final String TGT_BASE_PATH_VALUE = "s3://mybucket/blah";
  static final String TABLE_TYPE_PARAM = "--table-type";
  static final String TABLE_TYPE_VALUE = "COPY_ON_WRITE";
  static final String TARGET_TABLE_PARAM = "--target-table";
  static final String TARGET_TABLE_VALUE = "test";
  static final String BASE_FILE_FORMAT_PARAM = "--base-file-format";
  static final String BASE_FILE_FORMAT_VALUE = "PARQUET";
  static final String SOURCE_LIMIT_PARAM = "--source-limit";
  static final String SOURCE_LIMIT_VALUE = "500";
  static final String ENABLE_HIVE_SYNC_PARAM = "--enable-hive-sync";
  static final String HOODIE_CONF_PARAM = "--hoodie-conf";
  static final String HOODIE_CONF_VALUE1 = "hoodie.datasource.hive_sync.table=test_table";
  static final String HOODIE_CONF_VALUE2 = "hoodie.datasource.write.recordkey.field=Field1,Field2,Field3";
  static final Logger LOG = LogManager.getLogger(TestHoodieDeltaStreamerBase.class);
  public static KafkaTestUtils testUtils;
  protected static String topicName;

  protected static int testNum = 1;

  @BeforeAll
  public static void initClass() throws Exception {
    UtilitiesTestBase.initClass(true);
    PARQUET_SOURCE_ROOT = dfsBasePath + "/parquetFiles";
    JSON_KAFKA_SOURCE_ROOT = dfsBasePath + "/jsonKafkaFiles";
    testUtils = new KafkaTestUtils();
    testUtils.setup();
    topicName = "topic" + testNum;

    // prepare the configs.
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/base.properties", dfs, dfsBasePath + "/base.properties");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/base.properties", dfs, dfsBasePath + "/config/base.properties");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/sql-transformer.properties", dfs,
        dfsBasePath + "/sql-transformer.properties");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/source.avsc", dfs, dfsBasePath + "/source.avsc");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/source-flattened.avsc", dfs, dfsBasePath + "/source-flattened.avsc");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/target.avsc", dfs, dfsBasePath + "/target.avsc");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/target-flattened.avsc", dfs, dfsBasePath + "/target-flattened.avsc");

    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/source_short_trip_uber.avsc", dfs, dfsBasePath + "/source_short_trip_uber.avsc");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/source_uber.avsc", dfs, dfsBasePath + "/source_uber.avsc");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/target_short_trip_uber.avsc", dfs, dfsBasePath + "/target_short_trip_uber.avsc");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/target_uber.avsc", dfs, dfsBasePath + "/target_uber.avsc");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/invalid_hive_sync_uber_config.properties", dfs, dfsBasePath + "/config/invalid_hive_sync_uber_config.properties");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/uber_config.properties", dfs, dfsBasePath + "/config/uber_config.properties");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/short_trip_uber_config.properties", dfs, dfsBasePath + "/config/short_trip_uber_config.properties");
    UtilitiesTestBase.Helpers.copyToDFS("delta-streamer-config/clusteringjob.properties", dfs, dfsBasePath + "/clusteringjob.properties");

    TypedProperties props = new TypedProperties();
    props.setProperty("include", "sql-transformer.properties");
    props.setProperty("hoodie.datasource.write.keygenerator.class", TestHoodieDeltaStreamer.TestGenerator.class.getName());
    props.setProperty("hoodie.datasource.write.recordkey.field", "_row_key");
    props.setProperty("hoodie.datasource.write.partitionpath.field", "not_there");
    props.setProperty("hoodie.deltastreamer.schemaprovider.source.schema.file", dfsBasePath + "/source.avsc");
    props.setProperty("hoodie.deltastreamer.schemaprovider.target.schema.file", dfsBasePath + "/target.avsc");

    // Hive Configs
    props.setProperty(DataSourceWriteOptions.HIVE_URL().key(), "jdbc:hive2://127.0.0.1:9999/");
    props.setProperty(DataSourceWriteOptions.HIVE_DATABASE().key(), "testdb1");
    props.setProperty(DataSourceWriteOptions.HIVE_TABLE().key(), "hive_trips");
    props.setProperty(DataSourceWriteOptions.HIVE_PARTITION_FIELDS().key(), "datestr");
    props.setProperty(DataSourceWriteOptions.HIVE_PARTITION_EXTRACTOR_CLASS().key(),
        MultiPartKeysValueExtractor.class.getName());
    UtilitiesTestBase.Helpers.savePropsToDFS(props, dfs, dfsBasePath + "/" + PROPS_FILENAME_TEST_SOURCE);

    // Properties used for the delta-streamer which incrementally pulls from upstream Hudi source table and writes to
    // downstream hudi table
    TypedProperties downstreamProps = new TypedProperties();
    downstreamProps.setProperty("include", "base.properties");
    downstreamProps.setProperty("hoodie.datasource.write.recordkey.field", "_row_key");
    downstreamProps.setProperty("hoodie.datasource.write.partitionpath.field", "not_there");

    // Source schema is the target schema of upstream table
    downstreamProps.setProperty("hoodie.deltastreamer.schemaprovider.source.schema.file", dfsBasePath + "/target.avsc");
    downstreamProps.setProperty("hoodie.deltastreamer.schemaprovider.target.schema.file", dfsBasePath + "/target.avsc");
    UtilitiesTestBase.Helpers.savePropsToDFS(downstreamProps, dfs, dfsBasePath + "/test-downstream-source.properties");

    // Properties used for testing invalid key generator
    TypedProperties invalidProps = new TypedProperties();
    invalidProps.setProperty("include", "sql-transformer.properties");
    invalidProps.setProperty("hoodie.datasource.write.keygenerator.class", "invalid");
    invalidProps.setProperty("hoodie.datasource.write.recordkey.field", "_row_key");
    invalidProps.setProperty("hoodie.datasource.write.partitionpath.field", "not_there");
    invalidProps.setProperty("hoodie.deltastreamer.schemaprovider.source.schema.file", dfsBasePath + "/source.avsc");
    invalidProps.setProperty("hoodie.deltastreamer.schemaprovider.target.schema.file", dfsBasePath + "/target.avsc");
    UtilitiesTestBase.Helpers.savePropsToDFS(invalidProps, dfs, dfsBasePath + "/" + PROPS_FILENAME_TEST_INVALID);

    TypedProperties props1 = new TypedProperties();
    populateAllCommonProps(props1);
    UtilitiesTestBase.Helpers.savePropsToDFS(props1, dfs, dfsBasePath + "/" + PROPS_FILENAME_TEST_SOURCE1);

    TypedProperties properties = new TypedProperties();
    populateInvalidTableConfigFilePathProps(properties);
    UtilitiesTestBase.Helpers.savePropsToDFS(properties, dfs, dfsBasePath + "/" + PROPS_INVALID_TABLE_CONFIG_FILE);

    TypedProperties invalidHiveSyncProps = new TypedProperties();
    invalidHiveSyncProps.setProperty("hoodie.deltastreamer.ingestion.tablesToBeIngested", "uber_db.dummy_table_uber");
    invalidHiveSyncProps.setProperty("hoodie.deltastreamer.ingestion.uber_db.dummy_table_uber.configFile", dfsBasePath + "/config/invalid_hive_sync_uber_config.properties");
    UtilitiesTestBase.Helpers.savePropsToDFS(invalidHiveSyncProps, dfs, dfsBasePath + "/" + PROPS_INVALID_HIVE_SYNC_TEST_SOURCE1);

    prepareParquetDFSFiles(PARQUET_NUM_RECORDS, PARQUET_SOURCE_ROOT);
  }

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
  }

  @AfterAll
  public static void cleanupClass() {
    UtilitiesTestBase.cleanupClass();
    if (testUtils != null) {
      testUtils.teardown();
    }
  }

  @AfterEach
  public void teardown() throws Exception {
    super.teardown();
  }

  private static void populateInvalidTableConfigFilePathProps(TypedProperties props) {
    props.setProperty("hoodie.datasource.write.keygenerator.class", TestHoodieDeltaStreamer.TestGenerator.class.getName());
    props.setProperty("hoodie.deltastreamer.keygen.timebased.output.dateformat", "yyyyMMdd");
    props.setProperty("hoodie.deltastreamer.ingestion.tablesToBeIngested", "uber_db.dummy_table_uber");
    props.setProperty("hoodie.deltastreamer.ingestion.uber_db.dummy_table_uber.configFile", dfsBasePath + "/config/invalid_uber_config.properties");
  }

  static void populateAllCommonProps(TypedProperties props) {
    populateCommonProps(props);
    populateCommonKafkaProps(props);
    populateCommonHiveProps(props);
  }

  protected static void populateCommonProps(TypedProperties props) {
    props.setProperty("hoodie.datasource.write.keygenerator.class", TestHoodieDeltaStreamer.TestGenerator.class.getName());
    props.setProperty("hoodie.deltastreamer.keygen.timebased.output.dateformat", "yyyyMMdd");
    props.setProperty("hoodie.deltastreamer.ingestion.tablesToBeIngested", "short_trip_db.dummy_table_short_trip,uber_db.dummy_table_uber");
    props.setProperty("hoodie.deltastreamer.ingestion.uber_db.dummy_table_uber.configFile", dfsBasePath + "/config/uber_config.properties");
    props.setProperty("hoodie.deltastreamer.ingestion.short_trip_db.dummy_table_short_trip.configFile", dfsBasePath + "/config/short_trip_uber_config.properties");
  }

  protected static void populateCommonKafkaProps(TypedProperties props) {
    //Kafka source properties
    props.setProperty("bootstrap.servers", testUtils.brokerAddress());
    props.setProperty("auto.offset.reset", "earliest");
    props.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.setProperty("hoodie.deltastreamer.kafka.source.maxEvents", String.valueOf(5000));
  }

  protected static void populateCommonHiveProps(TypedProperties props) {
    // Hive Configs
    props.setProperty(DataSourceWriteOptions.HIVE_URL().key(), "jdbc:hive2://127.0.0.1:9999/");
    props.setProperty(DataSourceWriteOptions.HIVE_DATABASE().key(), "testdb2");
    props.setProperty(DataSourceWriteOptions.HIVE_ASSUME_DATE_PARTITION().key(), "false");
    props.setProperty(DataSourceWriteOptions.HIVE_PARTITION_FIELDS().key(), "datestr");
    props.setProperty(DataSourceWriteOptions.HIVE_PARTITION_EXTRACTOR_CLASS().key(),
        MultiPartKeysValueExtractor.class.getName());
  }

  protected static void prepareParquetDFSFiles(int numRecords) throws IOException {
    prepareParquetDFSFiles(numRecords, PARQUET_SOURCE_ROOT);
  }

  protected static void prepareParquetDFSFiles(int numRecords, String baseParquetPath) throws IOException {
    prepareParquetDFSFiles(numRecords, baseParquetPath, FIRST_PARQUET_FILE_NAME, false, null, null);
  }

  protected static void prepareParquetDFSFiles(int numRecords, String baseParquetPath, String fileName, boolean useCustomSchema,
                                               String schemaStr, Schema schema) throws IOException {
    String path = baseParquetPath + "/" + fileName;
    HoodieTestDataGenerator dataGenerator = new HoodieTestDataGenerator();
    if (useCustomSchema) {
      Helpers.saveParquetToDFS(Helpers.toGenericRecords(
          dataGenerator.generateInsertsAsPerSchema("000", numRecords, schemaStr),
          schema), new Path(path), HoodieTestDataGenerator.AVRO_TRIP_SCHEMA);
    } else {
      Helpers.saveParquetToDFS(Helpers.toGenericRecords(
          dataGenerator.generateInserts("000", numRecords)), new Path(path));
    }
  }

}
