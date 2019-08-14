package com.github.oxisto;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.Type;
import java.util.stream.Collectors;

public class SourceGenerator extends Generator {

  public SourceGenerator(TypeDeclaration type) {
    super(type);
  }

  public String generate() {
    return String.format("#include \"%s.h\"%n", this.typeDeclaration.getNameAsString())
        + String.format(generateConstructor() + String.format(generateMethods()));
  }

  private String generateConstructor() {
    var constructor = getDefaultConstructor();

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
        this.typeDeclaration.getNameAsString(),
        this.typeDeclaration.getNameAsString(),
        getJNIClassName(this.typeDeclaration),
        getJNIMethodSignature(constructor));
  }

  public String generateMethod(MethodDeclaration method) {
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
        getNativeType(method.getType()),
        this.typeDeclaration.getName(),
        generateUniqueName(this.typeDeclaration, method),
        getNativeArguments(method),
        method.getName(),
        getJNIMethodSignature(method),
        getNativeType(method.getType()),
        getNativeMethodCall(method.getType()),
        method.getParameters().stream()
            .map(NodeWithSimpleName::getNameAsString)
            .collect(Collectors.joining(", ", ", ", "")));
  }

  private String getNativeMethodCall(Type type) {
    if (type.isPrimitiveType()) {
      var prim = type.asPrimitiveType();
      switch (prim.getType()) {
        case BOOLEAN:
          return "CallBooleanMethod";
        case BYTE:
          return "CallByteMethod";
        case CHAR:
          return "CallCharMethod";
        case SHORT:
          return "CallShortMethod";
        case INT:
          return "CallIntMethod";
        case LONG:
          return "CallLongMethod";
        case FLOAT:
          return "CallFloatMethod";
        case DOUBLE:
          return "CallDoubleMethod";
      }
    } else if (type.isVoidType()) {
      return "CallVoidMethod";
    }

    return "CallObjectMethod";
  }
}
