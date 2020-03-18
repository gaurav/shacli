package org.renci.shacli.generator

import scala.language.reflectiveCalls
import scala.collection.JavaConverters._

import java.io.{File, BufferedOutputStream, FileOutputStream}

import com.typesafe.scalalogging.Logger
import org.apache.jena.rdf.model.{Model, InfModel, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFFormat}
import org.apache.jena.vocabulary.RDF
import org.topbraid.shacl.vocabulary.SH

import org.renci.shacli.ShacliApp

/**
  * Generate SHACL based on Turtle provided.
  */
object Generator {
  def generate(logger: Logger, conf: ShacliApp.Conf): Int = {
    val dataFiles: List[File]  = conf.generate.data()
    val baseURI: String        = conf.generate.baseURI()
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
    val typedResources = infModel.listResourcesWithProperty(RDF.`type`).toList.asScala.toSeq
    val resProps = typedResources
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

      val shapeResource =
        shapesModel.createResource(baseURI + rdfClass.asResource.getLocalName + "Shape")
      shapeResource.addProperty(RDF.`type`, SH.NodeShape)
      shapeResource.addProperty(SH.targetClass, rdfClass)

      val props = resProps
        .getOrElse(rdfClass, Seq())
        .flatMap(_.listProperties.toList.asScala.map(_.getPredicate))
        .toSet
      props.foreach(prop => {
        println(" - Property: " + dataModel.shortForm(prop.getURI))
      })
    })

    // Choose output file.
    val outputStream =
      if (outputFilename == "-") System.out
      else
        new BufferedOutputStream(new FileOutputStream(new File(outputFilename)))

    RDFDataMgr.write(outputStream, shapesModel, RDFFormat.TURTLE_PRETTY)

    if (outputFilename != "-") outputStream.close

    return 0
  }
}
