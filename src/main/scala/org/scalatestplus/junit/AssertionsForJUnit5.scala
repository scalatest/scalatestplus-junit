/*
 * Copyright 2001-2022 Artima, Inc.
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

import org.scalatest._
import org.opentest4j.AssertionFailedError
import org.scalactic._
import org.scalactic.exceptions.NullArgumentException
import org.scalatest.exceptions.{StackDepthException, TestCanceledException}

/**
 * Trait that contains ScalaTest's basic assertion methods, suitable for use with JUnit 5.
 *
 * <p>
 * The assertion methods provided in this trait look and behave exactly like the ones in
 * <a href="../Assertions.html"><code>Assertions</code></a>, except instead of throwing
 * <a href="../exceptions/TestFailedException.html"><code>TestFailedException</code></a> they throw
 * <a href="JUnit5TestFailedError.html"><code>JUnit5TestFailedError</code></a>,
 * which extends <code>org.opentest4j.AssertionFailedError</code>.
 *
 * <p>
 * JUnit 3 (release 3.8 and earlier) distinguishes between <em>failures</em> and <em>errors</em>.
 * If a test fails because of a failed assertion, that is considered a <em>failure</em>. If a test
 * fails for any other reason, either the test code or the application being tested threw an unexpected
 * exception, that is considered an <em>error</em>. The way JUnit 3 decides whether an exception represents
 * a failure or error is that only thrown <code>junit.framework.AssertionFailedError</code>s are considered
 * failures. Any other exception type is considered an error. The exception type thrown by the JUnit 3
 * assertion methods declared in <code>junit.framework.Assert</code> (such as <code>assertEquals</code>,
 * <code>assertTrue</code>, and <code>fail</code>) is, therefore, <code>AssertionFailedError</code>.
 * </p>
 *
 * <p>
 * In JUnit 4, <code>junit.framework.AssertionFailedError</code> was made to extend <code>java.lang.AssertionError</code>,
 * and the distinction between failures and errors was essentially dropped. However, some tools that integrate
 * with JUnit carry on this distinction, so even if you are using JUnit 4 you may want to use this
 * <code>AssertionsForJUnit</code> trait instead of plain-old ScalaTest
 * <a href="../Assertions.html"><code>Assertions</code></a>.
 * </p>
 *
 * <p>
 * In JUnit 5, <code>org.opentest4j.AssertionFailedError</code> is used as test-related AssertionError instead.
 * </p>
 *
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
trait AssertionsForJUnit5 extends VersionSpecificAssertionsForJUnit5 {

  private[org] override def newAssertionFailedException(optionalMessage: Option[String], optionalCause: Option[Throwable], pos: source.Position, differences: scala.collection.immutable.IndexedSeq[String]): Throwable = {
    new JUnit5TestFailedError(optionalMessage, optionalCause, pos, None)
  }

  private[org] override def newTestCanceledException(optionalMessage: Option[String], optionalCause: Option[Throwable], pos: source.Position): Throwable =
    new TestCanceledException(toExceptionFunction(optionalMessage), optionalCause, pos, None)

  /**
   * If message or message contents are null, throw a null exception, otherwise
   * create a function that returns the option.
   */
  def toExceptionFunction(message: Option[String]): StackDepthException => Option[String] = {
    message match {
      case null => throw new NullArgumentException("message was null")
      case Some(null) => throw new NullArgumentException("message was a Some(null)")
      case _ => { e => message }
    }
  }
}

/**
 * Companion object that facilitates the importing of <code>AssertionsForJUnit5</code> members as
 * an alternative to mixing it in. One use case is to import <code>AssertionsForJUnit5</code> members so you can use
 * them in the Scala interpreter:
 *
 * <pre>
 * sbt:junit-5.9> console
 * [info] Starting scala interpreter...
 * Welcome to Scala 2.13.10 (OpenJDK 64-Bit Server VM, Java 1.8.0_352).
 * Type in expressions for evaluation. Or try :help.
 *
 * scala> import org.scalatestplus.junit.AssertionsForJUnit5._
 * import org.scalatestplus.junit.AssertionsForJUnit5._
 *
 * scala> assert(1 === 2)
 * org.scalatestplus.junit.JUnit5TestFailedError: 1 did not equal 2
 *   at org.scalatestplus.junit.AssertionsForJUnit5.newAssertionFailedException(AssertionsForJUnit5.scala:64)
 *   at org.scalatestplus.junit.AssertionsForJUnit5.newAssertionFailedException$(AssertionsForJUnit5.scala:63)
 *   at org.scalatestplus.junit.AssertionsForJUnit5$.newAssertionFailedException(AssertionsForJUnit5.scala:122)
 *   at org.scalatestplus.junit.AssertionsForJUnit5$AssertionsHelper.macroAssert(AssertionsForJUnit5.scala:159)
 *   ... 35 elided
 * scala> val caught = intercept[StringIndexOutOfBoundsException] { "hi".charAt(-1) }
 * val caught: StringIndexOutOfBoundsException = java.lang.StringIndexOutOfBoundsException: String index out of range: -1
 * </pre>
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
object AssertionsForJUnit5 extends AssertionsForJUnit5 {

  import Requirements._

  /**
   * Helper class used by code generated by the <code>assert</code> macro.
   */
  class AssertionsHelper {

    private def append(currentMessage: Option[String], clue: Any) = {
      val clueStr = clue.toString
      if (clueStr.isEmpty)
        currentMessage
      else {
        currentMessage match {
          case Some(msg) =>
            // clue.toString.head is guaranteed to work, because the previous if check that clue.toString != ""
            val firstChar = clueStr.head
            if (firstChar.isWhitespace || firstChar == '.' || firstChar == ',' || firstChar == ';')
              Some(msg + clueStr)
            else
              Some(msg + " " + clueStr)
          case None => Some(clueStr)
        }
      }
    }

    /**
     * Assert that the passed in <code>Bool</code> is <code>true</code>, else fail with <code>TestFailedException</code>.
     *
     * @param bool the <code>Bool</code> to assert for
     * @param clue optional clue to be included in <code>TestFailedException</code>'s error message when assertion failed
     */
    def macroAssert(bool: Bool, clue: Any, prettifier: Prettifier, pos: source.Position): Assertion = {
      requireNonNull(clue)(prettifier, pos)
      if (!bool.value) {
        val failureMessage = if (Bool.isSimpleWithoutExpressionText(bool)) None else Some(bool.failureMessage)
        throw newAssertionFailedException(append(failureMessage, clue), None, pos, bool.analysis)
      }
      Succeeded
    }

    def macroAssert(bool: Bool, clue: Any, pos: source.Position): Assertion =
      macroAssert(bool, clue, bool.prettifier, pos)

    /**
     * Assume that the passed in <code>Bool</code> is <code>true</code>, else throw <code>TestCanceledException</code>.
     *
     * @param bool the <code>Bool</code> to assume for
     * @param clue optional clue to be included in <code>TestCanceledException</code>'s error message when assertion failed
     */
    def macroAssume(bool: Bool, clue: Any, prettifier: Prettifier, pos: source.Position): Assertion = {
      requireNonNull(clue)(prettifier, pos)
      if (!bool.value) {
        val failureMessage = if (Bool.isSimpleWithoutExpressionText(bool)) None else Some(bool.failureMessage)
        throw newTestCanceledException(append(failureMessage, clue), None, pos)
      }
      Succeeded
    }

    def macroAssume(bool: Bool, clue: Any, pos: source.Position): Assertion =
      macroAssume(bool, clue, bool.prettifier, pos)
  }

  /**
   * Helper instance used by code generated by macro assertion.
   */
  val assertionsHelper = new AssertionsHelper

}
