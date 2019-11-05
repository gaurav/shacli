import java.io.File

import com.typesafe.scalalogging.LazyLogging
import com.github.tototoshi.csv.CSVReader

/**
 * CreateSHACLFromGoogleDocs reads the Google Docs as exported to CSV sheets in an input directory
 * and produces SHACL shapes for the specified attributes.
 */

object CreateSHACLFromGoogleDocs extends App with LazyLogging {
  val inputDir = new File(args(0))

  // Step 1. Read Type.csv to get a list of classes.
  val entities: Seq[Map[String, String]] = CSVReader.open(new File(inputDir, "Type.csv")).allWithHeaders
  val entitiesById: Map[String, Seq[Map[String, String]]] = entities.groupBy(_.getOrElse("id", "(unknown)"))

  // Step 2. Read Attribute.csv to get a list of attributes.
  val attributes: Seq[Map[String, String]] = CSVReader.open(new File(inputDir, "Attribute.csv")).allWithHeaders
  val attributesById: Map[String, Seq[Map[String, String]]] = attributes.groupBy(_.getOrElse("id", "(unknown)"))

  // Step 3. If a Type has a parentType, then it should have all the attributes of the parentType
  // as well.
  def getAllAttributes(entityId: String): Seq[Map[String, String]] = {
    if (entityId == "") return Seq()
    val attrs: Seq[Map[String, String]] = attributes.filter(attr => attr.contains("entityId") && attr("entityId") == entityId)
    val parentAttrs: Seq[Map[String, String]] = entitiesById.get(entityId).map(_.flatMap { entity: Map[String, String] =>
      entity.get("parentType").map(getAllAttributes(_)).getOrElse(Seq())
    }).getOrElse(Seq())
    parentAttrs ++ attrs
  }
  val allAttributes = entitiesById.keys.toSeq.sorted.map(entityId => {
    logger.info(s"Entity ${entityId}")
    getAllAttributes(entityId).map(attr => {
      logger.info(s" - Attribute ${attr("name")}")
    })

  })
}
