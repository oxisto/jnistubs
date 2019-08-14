package com.github.oxisto.jnistubs

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import java.io.File
import java.io.FileNotFoundException

object JNIStubsCLI {

  @JvmStatic
  fun main(args: Array<String>) {
    // args 0 = root
    // args 1 = class

    val typeResolver = CombinedTypeSolver()
    val reflectionTypeSolver = ReflectionTypeSolver()
    typeResolver.add(reflectionTypeSolver)

    val javaParserTypeSolver = JavaParserTypeSolver(File(args[0]))
    typeResolver.add(javaParserTypeSolver)

    val symbolResolver = JavaSymbolSolver(typeResolver)
    val parserConfiguration = ParserConfiguration()
    parserConfiguration.setSymbolResolver(symbolResolver)

    val parser = JavaParser(parserConfiguration)

    try {
      val cu = parser.parse(File(args[0] + "/" + args[1]))
      val o = cu.result
      if (o.isPresent) {
        val result = o.get()

        for (type in result.types) {
          val header = HeaderGenerator(type)
          println(header.generate())

          val source = SourceGenerator(type)
          println(source.generate())
        }
      }

    } catch (e: FileNotFoundException) {
      e.printStackTrace()
    }

  }
}
