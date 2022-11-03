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

import org.junit.jupiter.engine.descriptor.{ClassTestDescriptor, TestMethodTestDescriptor}
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.junit.platform.engine.{EngineExecutionListener, TestDescriptor, TestExecutionResult, UniqueId}
import org.scalatest.{Resources => _, _}
import org.junit.runner.notification.RunNotifier
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.scalatest.events._

private[junit] class EngineExecutionListenerReporter(listener: EngineExecutionListener, clzDesc: ScalaTestClassDescriptor, engineDesc: TestDescriptor) extends Reporter {

  // This form isn't clearly specified in JUnit docs, but some tools may assume it, so why rock the boat.
  // Here's what JUnit code does:
  //   public static Description createTestDescription(Class<?> clazz, String name, Annotation... annotations) {
  //       return new Description(String.format("%s(%s)", name, clazz.getName()), annotations);
  //   }
  // So you can see the test name shows up, which is normally a test method name, followed by the fully qualified class name in parens
  // We put test name and suite class name (or suite name if no class) in parens, but don't try and do anything to get rid of spaces or
  // parens the test or suite names themselves, since it is unclear if this format is used by anyone anyway. If actual bug reports come
  // in, then we can fix each actual problem once it is understood.
  //
  private def testDescriptionName(suiteName: String, suiteClassName: Option[String], testName: String) =
    suiteClassName match {
      case Some(suiteClassName) => testName + "(" + suiteClassName + ")"
      case None => testName + "(" + suiteName + ")"
    }

  private def suiteDescriptionName(suiteName: String, suiteClassName: Option[String]) =
    suiteClassName match {
      case Some(suiteClassName) => suiteClassName
      case None => suiteName
    }

  private def createTestDescriptor(suiteId: String, suiteName: String, suiteClassName: Option[String], testName: String): ScalaTestDescriptor = {
    val uniqueId = UniqueId.parse(testDescriptionName(suiteName, suiteClassName, testName))
    new ScalaTestDescriptor(clzDesc, uniqueId, testName)
  }

  override def apply(event: Event): Unit = {

    event match {

      case TestStarting(ordinal, suiteName, suiteId, suiteClassName, testName, testText, formatter, location, rerunnable, payload, threadName, timeStamp) =>
        val testDesc = createTestDescriptor(suiteId, suiteName, suiteClassName, testName)
        listener.executionStarted(testDesc)

      case TestFailed(ordinal, message, suiteName, suiteId, suiteClassName, testName, testText, recordedEvents, analysis, throwable, duration, formatter, location, rerunnable, payload, threadName, timeStamp) =>
        val throwableOrNull =
          throwable match {
            case Some(t) => t
            case None => null // Yuck. Not sure if the exception passed to new Failure can be null, but it could be given this code. Usually throwable would be defined.
          }
        val testDesc = createTestDescriptor(suiteId, suiteName, suiteClassName, testName)
        listener.executionFinished(testDesc, TestExecutionResult.failed(throwableOrNull))

      case TestSucceeded(ordinal, suiteName, suiteId, suiteClassName, testName, testText, recordedEvents, duration, formatter, location, rerunnable, payload, threadName, timeStamp) =>
        val testDesc = createTestDescriptor(suiteId, suiteName, suiteClassName, testName)
        listener.executionFinished(testDesc, TestExecutionResult.successful())

      case TestIgnored(ordinal, suiteName, suiteId, suiteClassName, testName, testText, formatter, location, payload, threadName, timeStamp) =>
        val testDesc = createTestDescriptor(suiteId, suiteName, suiteClassName, testName)
        listener.executionSkipped(testDesc, "Test ignored.")

      // TODO: I dont see TestCanceled here. Probably need to add it
      // Closest thing we can do with pending is report an ignored test
      case TestPending(ordinal, suiteName, suiteId, suiteClassName, testName, testText, recordedEvents, duration, formatter, location, payload, threadName, timeStamp) =>
        val testDesc = createTestDescriptor(suiteId, suiteName, suiteClassName, testName)
        listener.executionSkipped(testDesc, "Test pending.")

      case SuiteAborted(ordinal, message, suiteName, suiteId, suiteClassName, throwable, duration, formatter, location, rerunnable, payload, threadName, timeStamp) =>
        val throwableOrNull =
          throwable match {
            case Some(t) => t
            case None => null // Yuck. Not sure if the exception passed to new Failure can be null, but it could be given this code. Usually throwable would be defined.
          }
        listener.executionFinished(clzDesc, TestExecutionResult.aborted(throwableOrNull))

      case RunAborted(ordinal, message, throwable, duration, summary, formatter, location, payload, threadName, timeStamp) =>
        val throwableOrNull =
          throwable match {
            case Some(t) => t
            case None => null // Yuck. Not sure if the exception passed to new Failure can be null, but it could be given this code. Usually throwable would be defined.
          }
        listener.executionFinished(engineDesc, TestExecutionResult.aborted(throwableOrNull))

      case _ =>
    }
  }

  // In the unlikely event that a message is blank, use the throwable's detail message
  def messageOrThrowablesDetailMessage(message: String, throwable: Option[Throwable]): String = {
    val trimmedMessage = message.trim
    if (!trimmedMessage.isEmpty)
      trimmedMessage
    else
      throwable match {
        case Some(t) => t.getMessage.trim
        case None => ""
      }
  }
}
