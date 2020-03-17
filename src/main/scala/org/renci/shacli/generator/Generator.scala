package org.renci.shacli.generator

import com.typesafe.scalalogging.Logger

import org.renci.shacli.ShacliApp

/**
  * Generate SHACL based on Turtle provided.
  */
object Generator {
  def generate(logger: Logger, conf: ShacliApp.Conf): Int = {
    println("Generate with conf: " + conf)
    return 0
  }
}
