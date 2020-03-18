package org.renci.shacli.generator

import java.io.{BufferedOutputStream, File, FileOutputStream}

import com.typesafe.scalalogging.Logger
import org.apache.jena.rdf.model.{InfModel, Model, ModelFactory, RDFNode, Resource}
import org.apache.jena.riot.{RDFDataMgr, RDFFormat}
import org.apache.jena.vocabulary.{RDF, RDFS}
import org.renci.shacli.ShacliApp
import org.topbraid.shacl.vocabulary.SH

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls

/**
  * Generate SHACL based on Turtle provided.
  */
object Generator {
  def generate(logger: Logger, conf: ShacliApp.Conf): Int = {
    val dataFiles: List[File] = conf.generate.data()
    val baseURI: String = conf.generate.baseURI()
    val outputFilename: String = conf.generate.output()

    // Start by loading all data into a single data model.
    val dataModel: Model = ModelFactory.createDefaultModel
    dataFiles
      .flatMap(fileOrDir => {
        if (fileOrDir.isDirectory) fileOrDir.listFiles
        else Seq(fileOrDir)
      })
      .foreach(dataFile => {
        logger.info("Reading " + dataFile)
        dataModel.add(RDFDataMgr.loadModel(dataFile.toString))
      })
    val infModel: InfModel = ModelFactory.createRDFSModel(dataModel)

    // Identify every rdf:type in this data model.
    val typedResources = infModel.listResourcesWithProperty(RDF.`type`).toList.asScala
    val resProps: Map[RDFNode, Seq[Resource]] = typedResources
      .flatMap(res => res.listProperties(RDF.`type`).toList.asScala)
      .groupBy(_.getObject)
      .mapValues(_.map(_.getSubject))

    logger.info(s"Found ${resProps.size} resources belonging to ${resProps.keySet.size} types.")

    val shapesModel = ModelFactory.createDefaultModel
    shapesModel.setNsPrefixes(dataModel)
    shapesModel.setNsPrefix("", baseURI)
    shapesModel.setNsPrefix("shacl", SH.getURI)
    shapesModel.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")

    resProps.keys.foreach(rdfClass => {
      logger.info("Class " + dataModel.shortForm(rdfClass.asResource.getURI))

      val shapeRes =
        shapesModel.createResource(baseURI + rdfClass.asResource.getLocalName + "Shape")
      shapeRes.addProperty(RDF.`type`, SH.NodeShape)
      shapeRes.addProperty(SH.targetClass, rdfClass)

      val props = resProps
        .getOrElse(rdfClass, Seq())
        .flatMap(_.listProperties.toList.asScala.map(_.getPredicate))
        .toSet

      props.foreach(prop => {
        // Set up the shacl:path.
        val propRes = shapesModel.createResource()
        propRes.addProperty(
          SH.path,
          prop
        )

        // Find names and descriptions for this property.
        val labels = prop.listProperties(RDFS.label).toList.asScala.map(_.getLiteral)
        labels.foreach(literal => propRes.addProperty(
          SH.name,
          literal
        ))

        // Find a description for this property.
        propRes.addProperty(
          SH.description,
          "Enter description here"
        )

        // What is the minimum and maximum number of times this property is used
        // per entity?
        val resourcesInType = resProps.getOrElse(rdfClass, Seq())
        val propCountPerResource = resourcesInType.map(_.listProperties(prop).toList.size)

        val minCount: Long = propCountPerResource.min
        val maxCount: Long = propCountPerResource.max

        propRes.addLiteral(
          SH.minCount,
          minCount
        )

        propRes.addLiteral(
          SH.maxCount,
          maxCount
        )

        // What types do we see in this property?
        val objectsPerResource = resourcesInType.flatMap(_.listProperties(prop).toList.asScala.map(_.getObject))
        val valueType = if (objectsPerResource.isEmpty) {
          propRes.addProperty(
            SH.nodeKind,
            RDF.nil
          )
          "empty"
        } else {
          val resources = objectsPerResource.filter(_.isResource).map(_.asResource)
          val literals = objectsPerResource.filter(_.isLiteral).map(_.asLiteral)

          if (literals.isEmpty && resources.nonEmpty) {
            propRes.addProperty(
              SH.nodeKind,
              SH.BlankNodeOrIRI,
            )
            "blank node or IRI"
          } else if (literals.nonEmpty && resources.isEmpty) {
            val literalTypes = literals.map(_.getDatatypeURI).groupBy(identity)
            val size = literalTypes.keySet.size

            if (size == 0) {
              // No known literal types?
              propRes.addProperty(
                SH.nodeKind,
                SH.Literal,
              )
              propRes.addProperty(
                SH.datatype,
                RDF.nil
              )

              "literals of unknown type"
            } else if (size == 1) {
              // All literals are of the same type!
              propRes.addProperty(
                SH.nodeKind,
                SH.Literal,
              )
              propRes.addProperty(
                SH.datatype,
                shapesModel.createResource(literalTypes.keySet.seq.head)
              )

              "literals of type " + shapesModel.shortForm(literalTypes.keySet.seq.head)
            } else {
              // More than one literal type.
              propRes.addProperty(
                SH.nodeKind,
                SH.Literal,
              )
              literalTypes.keySet.foreach(typ => propRes.addProperty(
                SH.datatype,
                shapesModel.createResource(typ)
              ))

              "literals of types " + literalTypes.keySet.seq.map(shapesModel.shortForm).mkString(" or ")
            }
          } else {
            propRes.addProperty(
              SH.nodeKind,
              RDF.nil
            )
            "unknown"
          }
        }

        // Add it to the shapeRes.
        shapeRes.addProperty(SH.property, propRes)

        // Report to STDOUT.
        logger.info(s" - Property: ${dataModel.shortForm(prop.getURI)} (min: $minCount, max: $maxCount, type: $valueType)")
      })
    })

    // Choose output file.
    val outputStream =
      if (outputFilename == "-") System.out
      else
        new BufferedOutputStream(new FileOutputStream(new File(outputFilename)))

    RDFDataMgr.write(outputStream, shapesModel, RDFFormat.TURTLE_PRETTY)

    if (outputFilename != "-") outputStream.close()

    0
  }
}
