# jnistubs

jnistubs automatically generated C++ stubs for your Java classes to call them via JNI

## Build

Build using `GraalVM` `native-image`

```
./gradlew clean nativeImage
```

## Install

```
cp build/graal/jnistubs /usr/local/bin
```

## Usage

```
jnistubs <root> <pathToJavaFile>
```
