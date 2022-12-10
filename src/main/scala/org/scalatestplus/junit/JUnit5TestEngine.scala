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

import org.junit.jupiter.api.ClassDescriptor
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.engine.discovery.{ClassSelector, ClasspathRootSelector, FileSelector, PackageSelector}
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.junit.platform.engine.{EngineDiscoveryRequest, ExecutionRequest, TestDescriptor, TestExecutionResult, UniqueId}
import org.scalatest.{Args, ConfigMap, Filter, Stopper, Tracker}

import scala.collection.JavaConverters._
import java.util.logging.Logger

/**
 * ScalaTest implementation for JUnit 5 Test Engine.
 */ 
class JUnit5TestEngine extends org.junit.platform.engine.TestEngine {

  private val logger = Logger.getLogger(classOf[JUnit5TestEngine].getName)

  /**
   * Test engine ID, return "scalatest".
   */
  def getId: String = "scalatest"

  /**
   * Discover ScalaTest suites, you can disable the discover by setting system property org.scalatestplus.junit.JUnit5TestEngine.disabled to "true".
   */
  def discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor = {
    // reference: https://blogs.oracle.com/javamagazine/post/junit-build-custom-test-engines-java
    //            https://software-matters.net/posts/custom-test-engine/

    val engineDesc = new EngineDescriptor(uniqueId, "ScalaTest EngineDescriptor")

    if (System.getProperty("org.scalatestplus.junit.JUnit5TestEngine.disabled") != "true") {
      logger.info("Starting test discovery...")

      val alwaysTruePredicate =
        new java.util.function.Predicate[String]() {
          def test(t: String): Boolean = true
        }

      val isSuitePredicate =
        new java.util.function.Predicate[Class[_]]() {
          def test(t: Class[_]): Boolean = classOf[org.scalatest.Suite].isAssignableFrom(t)
        }

      discoveryRequest.getSelectorsByType(classOf[ClasspathRootSelector]).asScala.foreach { selector =>
        ReflectionSupport.findAllClassesInClasspathRoot(selector.getClasspathRoot, isSuitePredicate, alwaysTruePredicate)
          .asScala
          .map(aClass => new ScalaTestClassDescriptor(engineDesc, uniqueId.append(ScalaTestClassDescriptor.segmentType, aClass.getName), aClass))
          .foreach(engineDesc.addChild _)
      }

      discoveryRequest.getSelectorsByType(classOf[PackageSelector]).asScala.foreach { selector =>
        ReflectionSupport.findAllClassesInPackage(selector.getPackageName(), isSuitePredicate, alwaysTruePredicate)
          .asScala
          .map(aClass => new ScalaTestClassDescriptor(engineDesc, uniqueId.append(ScalaTestClassDescriptor.segmentType, aClass.getName), aClass))
          .foreach(engineDesc.addChild _)
      }

      discoveryRequest.getSelectorsByType(classOf[ClassSelector]).asScala.foreach { selector =>
        if (classOf[org.scalatest.Suite].isAssignableFrom(selector.getJavaClass))
          engineDesc.addChild(new ScalaTestClassDescriptor(engineDesc, uniqueId.append(ScalaTestClassDescriptor.segmentType, selector.getJavaClass.getName), selector.getJavaClass))
      }

      logger.info("Completed test discovery, discovered suite count: " + engineDesc.getChildren.size())
    }

    engineDesc
  }

  /**
   * Execute ScalaTest suites, you can disable the ScalaTest suites execution by setting system property org.scalatestplus.junit.JUnit5TestEngine.disabled to "true".
   */
  def execute(request: ExecutionRequest): Unit = {
    if (System.getProperty("org.scalatestplus.junit.JUnit5TestEngine.disabled") != "true") {
      logger.info("Start tests execution...")
      val engineDesc = request.getRootTestDescriptor
      val listener = request.getEngineExecutionListener
      listener.executionStarted(engineDesc)
      engineDesc.getChildren.asScala.foreach { testDesc =>
        testDesc match {
          case clzDesc: ScalaTestClassDescriptor =>
            logger.info("Start execution of suite class " + clzDesc.suiteClass.getName + "...")
            listener.executionStarted(clzDesc)
            val suiteClass = clzDesc.suiteClass
            val canInstantiate = JUnitHelper.checkForPublicNoArgConstructor(suiteClass) && classOf[org.scalatest.Suite].isAssignableFrom(suiteClass)
            require(canInstantiate, "Must pass an org.scalatest.Suite with a public no-arg constructor")
            val suiteToRun = suiteClass.newInstance.asInstanceOf[org.scalatest.Suite]

            val reporter = new EngineExecutionListenerReporter(listener, clzDesc, engineDesc)
            //listener.executionStarted(clzDesc)

            suiteToRun.run(None, Args(reporter,
              Stopper.default, Filter(), ConfigMap.empty, None,
              new Tracker, Set.empty))

            listener.executionFinished(clzDesc, TestExecutionResult.successful())

            logger.info("Completed execution of suite class " + clzDesc.suiteClass.getName + ".")

          case otherDesc =>
            // Do nothing for other descriptor, just log it.
            logger.warning("Found test descriptor " + otherDesc.toString + " that is not supported, skipping.")
        }
      }
      listener.executionFinished(engineDesc, TestExecutionResult.successful())
      logger.info("Completed tests execution.")
    }
  }
}