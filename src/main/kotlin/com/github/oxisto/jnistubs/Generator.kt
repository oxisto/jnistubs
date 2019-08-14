package com.github.oxisto.jnistubs

import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithParameters
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.type.VoidType
import java.util.stream.Collectors

abstract class Generator(protected var typeDeclaration: TypeDeclaration<*>) {

  internal val defaultConstructor: ConstructorDeclaration?
    get() {
      for (member in this.typeDeclaration.members) {
        if (member is ConstructorDeclaration && member.parameters.isEmpty()) {
          return member
        }
      }

      return null
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
  protected fun generateUniqueName(type: TypeDeclaration<*>, method: MethodDeclaration): Any {
    // check if name is not unique on its own
    val list = type.getMethodsByName(method.nameAsString)

    if (list.size > 1) {
      // construct JNI argument signatures to see if this is really a problem
      val jniList = list.stream().map { this.getNativeArgumentSignature(it) }.collect(Collectors.toSet());

      // since this is now a set and unique, check if we "removed" some entries
      if (list.size != jniList.size) {
        // now we have a problem, since argument signatures that were different in java are now the
        // same in JNI
        // get the index
        for (i in list.indices) {
          val m = list[i]
          if (m === method) {
            return method.name.toString() + "" + i
          }
        }
      }
    }

    return method.name
  }

  internal fun getNativeArguments(method: MethodDeclaration): String {
    return method.parameters.stream()
            .map { parameter -> getNativeType(parameter.type) + " " + parameter.nameAsString }
            .collect(Collectors.joining(", "))
  }

  private fun getNativeArgumentSignature(method: MethodDeclaration): String {
    return method.parameters.stream()
            .map { parameter -> getNativeType(parameter.type) }
            .collect(Collectors.joining(", "))
  }

  protected fun getNativeType(type: Type): String {
    return when {
      type.isPrimitiveType -> "j" + type.asPrimitiveType()
      type.isArrayType -> getNativeType(type.asArrayType().componentType) + "Array"
      type.toString() == "String" -> "jstring"
      type.toString() == "Class" -> "jclass"
      else -> "jobject"
    }
  }

  private fun getJNITypeSignature(type: Type): String {
    when {
      type.isPrimitiveType -> {
        val prim = type.asPrimitiveType()
        return when (prim.type) {
          PrimitiveType.Primitive.BOOLEAN -> "Z"
          PrimitiveType.Primitive.BYTE -> "B"
          PrimitiveType.Primitive.CHAR -> "C"
          PrimitiveType.Primitive.SHORT -> "S"
          PrimitiveType.Primitive.INT -> "I"
          PrimitiveType.Primitive.LONG -> "J"
          PrimitiveType.Primitive.FLOAT -> "F"
          PrimitiveType.Primitive.DOUBLE -> "D"
          null -> throw GenerationException("Got unknown primitive")
        }
      }
      type.isArrayType -> return "[" + getJNITypeSignature(type.asArrayType().componentType)
      type.isVoidType -> return "V"
    }

    return "L" + getJNIClassName(type) + ";"
  }

  internal fun getJNIMethodSignature(methodDeclaration: MethodDeclaration): String {
    return getJNIMethodSignature(methodDeclaration, methodDeclaration.type)
  }

  internal fun getJNIMethodSignature(constructorDeclaration: ConstructorDeclaration): String {
    return getJNIMethodSignature(constructorDeclaration, VoidType())
  }

  private fun getJNIMethodSignature(node: NodeWithParameters<*>, returnType: Type): String {
    var s = "("

    s += node.parameters.stream()
            .map { parameter -> getJNITypeSignature(parameter.type) }
            .collect(Collectors.joining())

    s += ")"
    s += getJNITypeSignature(returnType)

    return s
  }

  internal fun getJNIClassName(type: TypeDeclaration<*>): String {
    val o = type.fullyQualifiedName

    return if (o.isPresent) {
      o.get().replace(".", "/")
    } else {
      type.nameAsString
    }
  }

  private fun getJNIClassName(type: Type): String {
    val resolved = type.resolve()

    return resolved.describe().replace(".", "/")
  }

  protected fun generateMethods(): String {
    val buffer = StringBuilder()
    for (method in this.typeDeclaration.methods) {
      buffer.append(generateMethod(method as MethodDeclaration))
    }

    return buffer.toString()
  }

  internal abstract fun generateMethod(method: MethodDeclaration): String

  companion object {

    @JvmStatic
    protected val INDENT: Any = " ".repeat(4)
  }
}
