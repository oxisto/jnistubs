package com.github.oxisto;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.io.FileNotFoundException;

public class JNIStubsCLI {

  public static void main(String[] args) {
    // args 0 = root
    // args 1 = class

    var typeResolver = new CombinedTypeSolver();
    ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    typeResolver.add(reflectionTypeSolver);

    JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(args[0]));
    typeResolver.add(javaParserTypeSolver);

    var symbolResolver = new JavaSymbolSolver(typeResolver);
    var parserConfiguration = new ParserConfiguration();
    parserConfiguration.setSymbolResolver(symbolResolver);

    var parser = new JavaParser(parserConfiguration);

    try {
      var cu = parser.parse(new File(args[0] + "/" + args[1]));
      var o = cu.getResult();
      if (o.isPresent()) {
        var result = o.get();

        for (var type : result.getTypes()) {
          var header = new HeaderGenerator(type);
          System.out.println(header.generate());

          var source = new SourceGenerator(type);
          System.out.println(source.generate());
        }
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
}
