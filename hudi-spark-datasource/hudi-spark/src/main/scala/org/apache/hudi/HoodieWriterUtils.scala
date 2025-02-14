/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi

import java.util.Properties

import scala.collection.JavaConverters._
import org.apache.hudi.DataSourceWriteOptions._
import org.apache.hudi.common.config.{HoodieConfig, TypedProperties}

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConverters.mapAsScalaMapConverter
import org.apache.hudi.common.config.HoodieMetadataConfig.METADATA_ENABLE_PROP
import org.apache.hudi.common.config.HoodieMetadataConfig.METADATA_VALIDATE_PROP
import org.apache.hudi.keygen.factory.HoodieSparkKeyGeneratorFactory
import org.apache.hudi.keygen.{BaseKeyGenerator, CustomAvroKeyGenerator, CustomKeyGenerator, KeyGenerator}

/**
 * WriterUtils to assist in write path in Datasource and tests.
 */
object HoodieWriterUtils {

  def javaParametersWithWriteDefaults(parameters: java.util.Map[String, String]): java.util.Map[String, String] = {
    mapAsJavaMap(parametersWithWriteDefaults(parameters.asScala.toMap))
  }

  /**
    * Add default options for unspecified write options keys.
    *
    * @param parameters
    * @return
    */
  def parametersWithWriteDefaults(parameters: Map[String, String]): Map[String, String] = {
    Map(OPERATION.key -> OPERATION.defaultValue,
      TABLE_TYPE.key -> TABLE_TYPE.defaultValue,
      PRECOMBINE_FIELD.key -> PRECOMBINE_FIELD.defaultValue,
      PAYLOAD_CLASS.key -> PAYLOAD_CLASS.defaultValue,
      RECORDKEY_FIELD.key -> RECORDKEY_FIELD.defaultValue,
      PARTITIONPATH_FIELD.key -> PARTITIONPATH_FIELD.defaultValue,
      KEYGENERATOR_CLASS.key -> DEFAULT_KEYGENERATOR_CLASS_OPT_VAL,
      METADATA_ENABLE_PROP.key -> METADATA_ENABLE_PROP.defaultValue.toString,
      METADATA_VALIDATE_PROP.key -> METADATA_VALIDATE_PROP.defaultValue.toString,
      COMMIT_METADATA_KEYPREFIX.key -> COMMIT_METADATA_KEYPREFIX.defaultValue,
      INSERT_DROP_DUPS.key -> INSERT_DROP_DUPS.defaultValue,
      STREAMING_RETRY_CNT.key -> STREAMING_RETRY_CNT.defaultValue,
      STREAMING_RETRY_INTERVAL_MS.key -> STREAMING_RETRY_INTERVAL_MS.defaultValue,
      STREAMING_IGNORE_FAILED_BATCH.key -> STREAMING_IGNORE_FAILED_BATCH.defaultValue,
      META_SYNC_CLIENT_TOOL_CLASS.key -> META_SYNC_CLIENT_TOOL_CLASS.defaultValue,
      HIVE_SYNC_ENABLED.key -> HIVE_SYNC_ENABLED.defaultValue,
      META_SYNC_ENABLED.key -> META_SYNC_ENABLED.defaultValue,
      HIVE_DATABASE.key -> HIVE_DATABASE.defaultValue,
      HIVE_TABLE.key -> HIVE_TABLE.defaultValue,
      HIVE_BASE_FILE_FORMAT.key -> HIVE_BASE_FILE_FORMAT.defaultValue,
      HIVE_USER.key -> HIVE_USER.defaultValue,
      HIVE_PASS.key -> HIVE_PASS.defaultValue,
      HIVE_URL.key -> HIVE_URL.defaultValue,
      HIVE_PARTITION_FIELDS.key -> HIVE_PARTITION_FIELDS.defaultValue,
      HIVE_PARTITION_EXTRACTOR_CLASS.key -> HIVE_PARTITION_EXTRACTOR_CLASS.defaultValue,
      HIVE_STYLE_PARTITIONING.key -> HIVE_STYLE_PARTITIONING.defaultValue,
      HIVE_USE_JDBC.key -> HIVE_USE_JDBC.defaultValue,
      HIVE_CREATE_MANAGED_TABLE.key() -> HIVE_CREATE_MANAGED_TABLE.defaultValue.toString,
      HIVE_SYNC_AS_DATA_SOURCE_TABLE.key() -> HIVE_SYNC_AS_DATA_SOURCE_TABLE.defaultValue(),
      ASYNC_COMPACT_ENABLE.key -> ASYNC_COMPACT_ENABLE.defaultValue,
      INLINE_CLUSTERING_ENABLE.key -> INLINE_CLUSTERING_ENABLE.defaultValue,
      ASYNC_CLUSTERING_ENABLE.key -> ASYNC_CLUSTERING_ENABLE.defaultValue,
      ENABLE_ROW_WRITER.key -> ENABLE_ROW_WRITER.defaultValue
    ) ++ DataSourceOptionsHelper.translateConfigurations(parameters)
  }

  def toProperties(params: Map[String, String]): TypedProperties = {
    val props = new TypedProperties()
    params.foreach(kv => props.setProperty(kv._1, kv._2))
    props
  }

  /**
   * Get the partition columns to stored to hoodie.properties.
   * @param parameters
   * @return
   */
  def getPartitionColumns(parameters: Map[String, String]): String = {
    val props = new TypedProperties()
    props.putAll(parameters.asJava)
    val keyGen = HoodieSparkKeyGeneratorFactory.createKeyGenerator(props)
    getPartitionColumns(keyGen)
  }

  def getPartitionColumns(keyGen: KeyGenerator): String = {
    keyGen match {
      // For CustomKeyGenerator and CustomAvroKeyGenerator, the partition path filed format
      // is: "field_name: field_type", we extract the field_name from the partition path field.
      case c: BaseKeyGenerator
        if c.isInstanceOf[CustomKeyGenerator] || c.isInstanceOf[CustomAvroKeyGenerator] =>
          c.getPartitionPathFields.asScala.map(pathField =>
            pathField.split(CustomAvroKeyGenerator.SPLIT_REGEX)
                .headOption.getOrElse(s"Illegal partition path field format: '$pathField' for ${c.getClass.getSimpleName}"))
            .mkString(",")

      case b: BaseKeyGenerator => b.getPartitionPathFields.asScala.mkString(",")
      case _=> null
    }
  }

  def convertMapToHoodieConfig(parameters: Map[String, String]): HoodieConfig = {
    val properties = new Properties()
    properties.putAll(mapAsJavaMap(parameters))
    new HoodieConfig(properties)
  }
}
