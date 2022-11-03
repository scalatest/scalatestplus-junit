/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatestplus.junit

import org.junit.jupiter.engine.config.{DefaultJupiterConfiguration, JupiterConfiguration}
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.discovery.{ClassSelector, ClasspathRootSelector, PackageSelector}
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.junit.platform.engine.{EngineDiscoveryRequest, ExecutionRequest, TestDescriptor, UniqueId}
import org.scalatest.{Args, ConfigMap, Filter, Stopper, Tracker}

import scala.collection.JavaConverters._

class JUnit5TestEngine extends org.junit.platform.engine.TestEngine {

  private val uniqueId = UniqueId.forEngine("scalatest-test-engine")
  private val engineDesc = new EngineDescriptor(uniqueId, "ScalaTest Test Engine")

  def getId: String = uniqueId.toString

  def discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor = {
    // reference: https://blogs.oracle.com/javamagazine/post/junit-build-custom-test-engines-java
    val config = new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters)

    val alwaysTruePredicate =
      new java.util.function.Predicate[String]() {
        def test(t: String): Boolean = true
      }

    val isSuitePredicate =
      new java.util.function.Predicate[Class[_]]() {
        def test(t: Class[_]): Boolean = t.isAssignableFrom(classOf[org.scalatest.Suite])
      }

    discoveryRequest.getSelectorsByType(classOf[ClasspathRootSelector]).asScala.foreach { selector =>
      ReflectionSupport.findAllClassesInClasspathRoot(selector.getClasspathRoot, isSuitePredicate, alwaysTruePredicate)
        .asScala
        .map(aClass => new ClassTestDescriptor(uniqueId.append(ClassTestDescriptor.SEGMENT_TYPE, aClass.getName), aClass, config))
        .foreach(engineDesc.addChild _)
    }

    discoveryRequest.getSelectorsByType(classOf[PackageSelector]).asScala.foreach {selector =>
      ReflectionSupport.findAllClassesInPackage(selector.getPackageName(), isSuitePredicate, alwaysTruePredicate)
        .asScala
        .map(aClass => new ClassTestDescriptor(uniqueId.append(ClassTestDescriptor.SEGMENT_TYPE, aClass.getName), aClass, config))
        .foreach(engineDesc.addChild _)
    }

    discoveryRequest.getSelectorsByType(classOf[ClassSelector]).asScala.foreach { selector =>
      if (selector.getJavaClass.isAssignableFrom(classOf[org.scalatest.Suite]))
        engineDesc.addChild(new ClassTestDescriptor(uniqueId.append(ClassTestDescriptor.SEGMENT_TYPE, selector.getJavaClass.getName), selector.getJavaClass, config))
    }

    engineDesc
  }

  def execute(request: ExecutionRequest): Unit = {
    val engineDesc = request.getRootTestDescriptor
    val listener = request.getEngineExecutionListener
    engineDesc.getChildren.asScala.foreach { testDesc =>
      testDesc match {
        case clzDesc: ClassTestDescriptor =>
          val suiteClass = clzDesc.getTestClass
          val canInstantiate = JUnitHelper.checkForPublicNoArgConstructor(clzDesc.getTestClass) && clzDesc.getTestClass.isAssignableFrom(classOf[org.scalatest.Suite])
          require(canInstantiate, "Must pass an org.scalatest.Suite with a public no-arg constructor")
          val suiteToRun = suiteClass.newInstance.asInstanceOf[org.scalatest.Suite]

          val reporter = new EngineExecutionListenerReporter(listener, clzDesc, engineDesc)

          suiteToRun.run(None, Args(reporter,
            Stopper.default, Filter(), ConfigMap.empty, None,
            new Tracker, Set.empty))

        case _ => // Do nothing for other descriptor
      }
    }
  }
}