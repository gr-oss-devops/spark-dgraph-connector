package uk.co.gresearch.spark.dgraph.connector.sources

import org.scalatest.FunSpec
import uk.co.gresearch.spark.SparkTestSession
import uk.co.gresearch.spark.dgraph.connector._
import org.apache.spark.sql.execution.datasources.v2.DataSourceRDDPartition

class TestTriplesSource extends FunSpec with SparkTestSession {

  import spark.implicits._

  describe("TriplesDataSource") {

    it("should load triples via path") {
      spark
        .read
        .format(TriplesSource)
        .load("localhost:9080")
        .show(100, false)
    }

    it("should load triples via paths") {
      spark
        .read
        .format(TriplesSource)
        .load("localhost:9080", "127.0.0.1:9080")
        .show(100, false)
    }

    it("should load triples via target option") {
      spark
        .read
        .format(TriplesSource)
        .option(TargetOption, "localhost:9080")
        .load()
        .show(100, false)
    }

    it("should load triples via targets option") {
      spark
        .read
        .format(TriplesSource)
        .option(TargetsOption, "[\"localhost:9080\",\"127.0.0.1:9080\"]")
        .load()
        .show(100, false)
    }

    it("should load triples via implicit dgraph target") {
      spark
        .read
        .dgraphTriples("localhost:9080")
        .show(100, false)
    }

    it("should load triples via implicit dgraph targets") {
      spark
        .read
        .dgraphTriples("localhost:9080", "127.0.0.1:9080")
        .show(100, false)
    }

    it("should load string-object triples") {
      spark
        .read
        .option(TriplesModeOption, TriplesModeStringOption)
        .dgraphTriples("localhost:9080")
        .show(100, false)
    }

    it("should load typed-object triples") {
      spark
        .read
        .option(TriplesModeOption, TriplesModeTypedOption)
        .dgraphTriples("localhost:9080")
        .show(100, false)
    }

    it("should encode StringTriple") {
      val rows =
        spark
          .read
          .option(TriplesModeOption, TriplesModeStringOption)
          .dgraphTriples("localhost:9080")
          .as[StringTriple]
          .collectAsList()
      rows.forEach(println)
    }

    it("should encode TypedTriple") {
      val rows =
        spark
          .read
          .option(TriplesModeOption, TriplesModeTypedOption)
          .dgraphTriples("localhost:9080")
          .as[TypedTriple]
          .collectAsList()
      rows.forEach(println)
    }

    it("should fail without target") {
      assertThrows[IllegalArgumentException] {
        spark
          .read
          .format(TriplesSource)
          .load()
      }
    }

    it("should fail with unknown triple mode") {
      assertThrows[IllegalArgumentException] {
        spark
          .read
          .format(TriplesSource)
          .option(TriplesModeOption, "unknown")
          .load()
      }
    }

    it("should load as a single partition") {
      val target = "localhost:9080"
      val targets = Seq(Target(target))
      val partitions =
        spark
          .read
          .option(PartitionerOption, SingletonPartitionerOption)
          .dgraphTriples(target)
          .rdd
          .partitions.map {
            case p: DataSourceRDDPartition => Some(p.inputPartition)
            case _ => None
          }
      assert(partitions.length === 1)
      assert(partitions === Seq(Some(Partition(targets, None))))
    }

    it("should load as a predicate partitions") {
      val target = "localhost:9080"
      val targets = Seq(Target(target))
      val partitions =
        spark
          .read
          .option(PartitionerOption, PredicatePartitionerOption)
          .option(PredicatePartitionerPredicatesOption, "2")
          .dgraphTriples(target)
          .rdd
          .partitions.map {
          case p: DataSourceRDDPartition => Some(p.inputPartition)
          case _ => None
        }
      assert(partitions.length === 1)
      assert(partitions === Seq(Some(Partition(targets, None))))
    }

  }

}
