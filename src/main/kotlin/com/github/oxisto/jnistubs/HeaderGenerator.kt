package com.github.oxisto.jnistubs

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration

class HeaderGenerator(type: TypeDeclaration<*>) : Generator(type) {

  fun generate(): String {
    return (String.format("#include <jni.h>%n%n")
            + String.format("class %s%n", typeDeclaration.nameAsString)
            + String.format("{%n")
            + String.format("public:%n")
            + generateConstructor()
            + generateMethods()
            + generateDefaults()
            + String.format("};%n"))
  }

  private fun generateDefaults(): String {
    return (String.format("%n")
            + String.format("%sconst inline jclass GetClass() { return this->cls; }%n",
            INDENT)
            + String.format("%sconst inline jobject GetObject() { return this->object; }%n",
            INDENT)
            + String.format("%n")
            + String.format("private:%n")
            + String.format("%sjobject object;%n", INDENT)
            + String.format("%sjclass cls;%n", INDENT)
            + String.format("%sJNIEnv *env;%n", INDENT))
  }

  override fun generateMethod(method: MethodDeclaration): String {
    return String.format("%n%s// %s%n", INDENT, method.signature) + String.format(
            "%s%s %s(%s);%n",
            INDENT,
            getNativeType(method.type),
            generateUniqueName(this.typeDeclaration, method),
            getNativeArguments(method))
  }

  private fun generateConstructor(): String {
    // for now we only support default constructors
    val constructor = this.defaultConstructor
            ?: throw GenerationException("Class does not have default constructor")

    return String.format("%s%s(JNIEnv *env);%n", INDENT, this.typeDeclaration.nameAsString)
  }
}
