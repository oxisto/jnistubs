package com.github.oxisto;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

public class HeaderGenerator extends Generator {

  public HeaderGenerator(TypeDeclaration<?> type) {
    super(type);
  }

  public String generate() {
    return String.format("#include <jni.h>%n%n")
        + String.format("class %s%n", typeDeclaration.getNameAsString())
        + String.format("{%n")
        + String.format("public:%n")
        + generateConstructor()
        + generateMethods()
        + generateDefaults()
        + String.format("};%n");
  }

  private String generateDefaults() {
    return String.format("%n")
        + String.format("%sconst inline jclass GetClass() { return this->cls; }%n", INDENT)
        + String.format("%sconst inline jobject GetObject() { return this->object; }%n", INDENT)
        + String.format("%n")
        + String.format("private:%n")
        + String.format("%sjobject object;%n", INDENT)
        + String.format("%sjclass cls;%n", INDENT)
        + String.format("%sJNIEnv *env;%n", INDENT);
  }

  protected String generateMethod(MethodDeclaration method) {
    return String.format("%n%s// %s%n", INDENT, method.getSignature())
        + String.format(
            "%s%s %s(%s);%n",
            INDENT,
            getNativeType(method.getType()),
            generateUniqueName(this.typeDeclaration, method),
            getNativeArguments(method));
  }

  private String generateConstructor() {
    // for now we only support default constructors
    var constructor = getDefaultConstructor();

    if (constructor == null) {
      throw new RuntimeException("Class does not have default constructor");
    }

    return String.format("%s%s(JNIEnv *env);%n", INDENT, this.typeDeclaration.getNameAsString());
  }
}
