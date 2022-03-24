/*
 * Copyright 2020 G-Research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.gresearch.spark.dgraph.connector

import org.apache.spark.sql.connector.expressions._
import org.apache.spark.sql.connector.read._
import org.apache.spark.sql.connector.read.partitioning.{ClusteredDistribution, Distribution, Partitioning}
import org.apache.spark.sql.types.StructType
import uk.co.gresearch.spark.dgraph.connector.model.GraphTableModel
import uk.co.gresearch.spark.dgraph.connector.partitioner.Partitioner

case class TripleScan(partitioner: Partitioner, model: GraphTableModel)
  extends Scan with SupportsReportPartitioning with SupportsReportOrdering
    with Batch {

  override def readSchema(): StructType = model.readSchema()

  override def toBatch: Batch = this

  private lazy val partitions: Array[InputPartition] = partitioner.getPartitions.toArray

  override def planInputPartitions(): Array[InputPartition] = partitions

  override def createReaderFactory(): PartitionReaderFactory =
    TriplePartitionReaderFactory(model.withMetrics(AccumulatorPartitionMetrics()))

  override def outputPartitioning(): Partitioning = new Partitioning {
    def numPartitions: Int = partitions.length

    def satisfy(distribution: Distribution): Boolean = distribution match {
      case c: ClusteredDistribution =>
        partitioner.getPartitionColumns.exists(_.toSet.equals(c.clusteredColumns.toSet))
      case _ => false
    }
  }

  override def outputOrdering(): Array[SortOrder] =
    partitioner.getOrderColumns.map(_.map(columnName => new SortOrder {
      override def expression(): Expression = new Transform {
        override def name(): String = "identity"

        override def references(): Array[NamedReference] = Array.empty

        override def arguments(): Array[Expression] = Seq(new NamedReference {
          override def fieldNames(): Array[String] = Seq(columnName).toArray
        }).toArray
      }

      override def direction(): SortDirection = SortDirection.ASCENDING

      override def nullOrdering(): NullOrdering = NullOrdering.NULLS_FIRST
    }).toArray).getOrElse(Array.empty[SortOrder])

}
