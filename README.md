# ScalaTest + JUnit
ScalaTest + JUnit provides integration support between ScalaTest and JUnit.

**Usage**

To use it for ScalaTest 3.2.2 and JUnit 4.12.x: 

SBT: 

```
libraryDependencies += "org.scalatestplus" %% "junit-4-12" % "3.2.2.0" % "test"
```

Maven: 

```
<dependency>
  <groupId>org.scalatestplus</groupId>
  <artifactId>junit-4-12</artifactId>
  <version>3.2.2.0</version>
  <scope>test</scope>
</dependency>
```

**Publishing**

Please use the following commands to publish to Sonatype: 

```
$ sbt +publishSigned
```