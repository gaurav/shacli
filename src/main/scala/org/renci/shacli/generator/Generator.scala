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

    resProps.keys.foreach(rdfClass => {
      println("Class " + dataModel.shortForm(rdfClass.asResource.getURI))

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

        shapeRes.addProperty(SH.property, propRes)

        // Report
        println(s" - Property: ${dataModel.shortForm(prop.getURI)} (min: $minCount, max: $maxCount)")
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
