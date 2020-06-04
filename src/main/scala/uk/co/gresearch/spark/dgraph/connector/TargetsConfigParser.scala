package uk.co.gresearch.spark.dgraph.connector

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.spark.sql.util.CaseInsensitiveStringMap

trait TargetsConfigParser {

  protected def getTargets(map: CaseInsensitiveStringMap): Seq[Target] = {
    val objectMapper = new ObjectMapper()
    val fromTargets = Seq(TargetsOption, "paths").flatMap(option =>
      Option(map.get(option)).map { pathStr =>
        objectMapper.readValue(pathStr, classOf[Array[String]]).toSeq
      }.getOrElse(Seq.empty[String])
    )

    val fromTarget = Seq(TargetOption, "path").flatMap(option =>
      Option(map.get(option))
    )

    val allTargets = fromTargets ++ fromTarget
    if (allTargets.isEmpty)
      throw new IllegalArgumentException("No Dgraph servers provided, provide targets via " +
        "DataFrameReader.load(…) or DataFrameReader.option(TargetOption, …)"
      )

    allTargets.map(Target)
  }
}