package com.github.oxisto.jnistubs

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import java.util.stream.Collectors

class SourceGenerator(type: TypeDeclaration<*>) : Generator(type) {

  fun generate(): String {
    return String.format("#include \"%s.h\"%n", this.typeDeclaration.nameAsString) + String.format(generateConstructor() + String.format(generateMethods()))
  }

  private fun generateConstructor(): String {
    val constructor = this.defaultConstructor

    return String.format(
            "%n"
                    + "%s::%s(JNIEnv *env) : env(env)%n"
                    + "{%n"
                    + "    this->cls = this->env->FindClass(\"%s\");%n"
                    + "    if (this->cls == nullptr)%n"
                    + "    {%n"
                    + "        throw this->env->ExceptionOccurred();%n"
                    + "    }%n"
                    + "%n"
                    + "    jmethodID constructor = this->env->GetMethodID(this->cls, \"<init>\", \"%s\");%n"
                    + "    if (constructor == nullptr)%n"
                    + "    {%n"
                    + "        throw this->env->ExceptionOccurred();%n"
                    + "    }%n"
                    + "%n"
                    + "    this->object = this->env->NewObject(this->cls, constructor);%n"
                    + "    if (object == nullptr)%n"
                    + "    {%n"
                    + "        throw this->env->ExceptionOccurred();%n"
                    + "    }%n"
                    + "}%n",
            this.typeDeclaration.nameAsString,
            this.typeDeclaration.nameAsString,
            getJNIClassName(this.typeDeclaration),
            getJNIMethodSignature(constructor!!))
  }

  override fun generateMethod(method: MethodDeclaration): String {
    return String.format(
            "%n"
                    + "%s %s::%s(%s)%n"
                    + "{%n"
                    + "    jmethodID method = this->env->GetMethodID(this->cls, \"%s\", \"%s\");%n"
                    + "    if (method == nullptr)%n"
                    + "    {%n"
                    + "        throw this->env->ExceptionOccurred();%n"
                    + "    }%n"
                    + "%n"
                    + "    %s result = env->%s(this->object, method%s);%n"
                    + "    if (env->ExceptionCheck())%n"
                    + "    {%n"
                    + "        throw this->env->ExceptionOccurred();%n"
                    + "    }%n"
                    + "%n"
                    + "    return result;%n"
                    + "}%n",
            getNativeType(method.type),
            this.typeDeclaration.name,
            generateUniqueName(this.typeDeclaration, method),
            getNativeArguments(method),
            method.name,
            getJNIMethodSignature(method),
            getNativeType(method.type),
            getNativeMethodCall(method.type),
            method.parameters.stream()
                    .map<String> { it.nameAsString }
                    .collect(Collectors.joining(", ", ", ", "")))
  }

  private fun getNativeMethodCall(type: Type): String {
    if (type.isPrimitiveType) {
      val prim = type.asPrimitiveType()
      return when (prim.type) {
        PrimitiveType.Primitive.BOOLEAN -> "CallBooleanMethod"
        PrimitiveType.Primitive.BYTE -> "CallByteMethod"
        PrimitiveType.Primitive.CHAR -> "CallCharMethod"
        PrimitiveType.Primitive.SHORT -> "CallShortMethod"
        PrimitiveType.Primitive.INT -> "CallIntMethod"
        PrimitiveType.Primitive.LONG -> "CallLongMethod"
        PrimitiveType.Primitive.FLOAT -> "CallFloatMethod"
        PrimitiveType.Primitive.DOUBLE -> "CallDoubleMethod"
        null -> throw GenerationException("Got unknown primitive")
      }
    } else if (type.isVoidType) {
      return "CallVoidMethod"
    }

    return "CallObjectMethod"
  }
}
