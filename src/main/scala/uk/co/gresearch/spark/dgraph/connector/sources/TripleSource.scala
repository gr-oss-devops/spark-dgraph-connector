package uk.co.gresearch.spark.dgraph.connector.sources

import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import uk.co.gresearch.spark.dgraph.connector.encoder.{StringTripleEncoder, TypedTripleEncoder}
import uk.co.gresearch.spark.dgraph.connector.{TripleTable, SchemaProvider, TableProviderBase, Target, TriplesModeOption, TriplesModeStringOption, TriplesModeTypedOption}

class TripleSource() extends TableProviderBase with SchemaProvider {

  override def shortName(): String = "dgraph-triples"

  def getTripleMode(map: CaseInsensitiveStringMap): Option[String] =
    Option(map.get(TriplesModeOption))

  def getTable(options: CaseInsensitiveStringMap): Table = {
    val targets = getTargets(options)
    val schema = getSchema(targets)
    val tripleMode = getTripleMode(options)
    val encoder = tripleMode match {
      case Some(TriplesModeStringOption) => new StringTripleEncoder
      case Some(TriplesModeTypedOption) => new TypedTripleEncoder
      case Some(mode) => throw new IllegalArgumentException(s"Unknown triple mode: ${mode}")
      case None => new TypedTripleEncoder
    }
    new TripleTable(targets, schema, encoder)
  }

}