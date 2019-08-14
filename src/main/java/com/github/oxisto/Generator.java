package com.github.oxisto;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import java.util.stream.Collectors;

public abstract class Generator {
  protected TypeDeclaration typeDeclaration;

  protected static final Object INDENT = " ".repeat(4);

  public Generator(TypeDeclaration type) {
    this.typeDeclaration = type;
  }

  /**
   * Tries to find a unique name for a method. There could be a problem if the java function is
   * overloaded using an object, which would result in two C++ functions with the same name and both
   * have jobject as argument. It would not be a problem if they have different JNI types.
   *
   * @param type
   * @param method
   * @return
   */
  protected Object generateUniqueName(TypeDeclaration<?> type, MethodDeclaration method) {
    // check if name is not unique on its own
    var list = type.getMethodsByName(method.getNameAsString());

    if (list.size() > 1) {
      // construct JNI argument signatures to see if this is really a problem
      var jniList = list.stream().map(this::getNativeArgumentSignature).collect(Collectors.toSet());

      // since this is now a set and unique, check if we "removed" some entries
      if (list.size() != jniList.size()) {
        // now we have a problem, since argument signatures that were different in java are now the
        // same in JNI
        // get the index
        for (int i = 0; i < list.size(); i++) {
          var m = list.get(i);
          if (m == method) {
            return method.getName() + "" + i;
          }
        }
      }
    }

    return method.getName();
  }

  ConstructorDeclaration getDefaultConstructor() {
    for (var member : this.typeDeclaration.getMembers()) {
      if (member instanceof ConstructorDeclaration) {
        var constructor = (ConstructorDeclaration) member;

        if (constructor.getParameters().isEmpty()) {
          return constructor;
        }
      }
    }

    return null;
  }

  String getNativeArguments(MethodDeclaration method) {
    return method.getParameters().stream()
        .map(parameter -> getNativeType(parameter.getType()) + " " + parameter.getNameAsString())
        .collect(Collectors.joining(", "));
  }

  private String getNativeArgumentSignature(MethodDeclaration method) {
    return method.getParameters().stream()
        .map(parameter -> getNativeType(parameter.getType()))
        .collect(Collectors.joining(", "));
  }

  String getNativeType(Type type) {
    if (type.isPrimitiveType()) {
      return "j" + type.asPrimitiveType();
    } else if (type.isArrayType()) {
      return getNativeType(type.asArrayType().getComponentType()) + "Array";
    } else if (type.toString().equals("String")) {
      return "jstring";
    } else if (type.toString().equals("Class")) {
      return "jclass";
    } else {
      return "jobject";
    }
  }

  String getJNITypeSignature(Type type) {
    if (type.isPrimitiveType()) {
      var prim = type.asPrimitiveType();
      switch (prim.getType()) {
        case BOOLEAN:
          return "Z";
        case BYTE:
          return "B";
        case CHAR:
          return "C";
        case SHORT:
          return "S";
        case INT:
          return "I";
        case LONG:
          return "J";
        case FLOAT:
          return "F";
        case DOUBLE:
          return "D";
      }
    } else if (type.isArrayType()) {
      return "[" + getJNITypeSignature(type.asArrayType().getComponentType());
    } else if (type.isVoidType()) {
      return "V";
    }

    return "L" + getJNIClassName(type) + ";";
  }

  String getJNIMethodSignature(MethodDeclaration methodDeclaration) {
    return getJNIMethodSignature(methodDeclaration, methodDeclaration.getType());
  }

  String getJNIMethodSignature(ConstructorDeclaration constructorDeclaration) {
    return getJNIMethodSignature(constructorDeclaration, new VoidType());
  }

  String getJNIMethodSignature(NodeWithParameters<?> node, Type returnType) {
    var s = "(";

    s +=
        node.getParameters().stream()
            .map(parameter -> getJNITypeSignature(parameter.getType()))
            .collect(Collectors.joining());

    s += ")";
    s += getJNITypeSignature(returnType);

    return s;
  }

  String getJNIClassName(TypeDeclaration type) {
    var o = type.getFullyQualifiedName();

    if (o.isPresent()) {
      return ((String) o.get()).replace(".", "/");
    } else {
      return type.getNameAsString();
    }
  }

  String getJNIClassName(Type type) {
    var resolved = type.resolve();

    return resolved.describe().replace(".", "/");
  }

  protected String generateMethods() {
    var buffer = new StringBuilder();
    for (var method : this.typeDeclaration.getMethods()) {
      buffer.append(generateMethod((MethodDeclaration) method));
    }

    return buffer.toString();
  }

  abstract String generateMethod(MethodDeclaration method);
}
