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

package org.scalatestplus.junit {

  import org.scalatest._

  // Put fixture suites in a subpackage, so they won't be discovered by
  // -m org.scalatest.junit when running the test target for this project.
  package helpers {

    import org.junit.runner.{Description, RunWith}
    import org.junit.runner.manipulation.{Filter => TestFilter}

    @RunWith(classOf[JUnitRunner])
    class EasySuite extends funsuite.AnyFunSuite {

      val runCount = 3
      val failedCount = 2
      val ignoreCount = 1

      test("JUnit ran this OK!") {
        assert(1 === 1)
      }

      test("JUnit ran this OK!, but it had a failure we hope") {
        assert(1 === 2)
      }

      test("bla bla bla") {
        assert(1 === 2)
      }

      ignore("I should be ignored") {
        assert(1 === 2)
      }
    }

    //
    // Blows up in beforeAll before any tests can be run.
    //
    @RunWith(classOf[JUnitRunner])
    class KerblooeySuite extends funsuite.AnyFunSuite with BeforeAndAfterAll {

      override def beforeAll(): Unit = {
        throw new RuntimeException("kerblooey")
      }

      val runCount = 0
      val failedCount = 1
      val ignoreCount = 0

      test("JUnit ran this OK!") {
        assert(1 === 1)
      }

      test("JUnit ran this OK!, but it had a failure we hope") {
        assert(1 === 2)
      }

      ignore("I should be ignored") {
        assert(1 === 2)
      }
    }

    class NameFilter(namePattern: String) extends TestFilter {
      override def shouldRun(description: Description): Boolean = {
        description.getClassName().contains(namePattern)
      }

      override def describe(): String = this.toString
    }

    //
    // Suite to be filtered out by the name filter
    //
    @RunWith(classOf[JUnitRunner])
    class FilteredOutSuite extends FunSuite with BeforeAndAfterAll {
      test("JUnit ran this OK!") {
        assert(1 === 1)
      }
    }

    //
    // Suite not to be filtered by the name filter
    //
    @RunWith(classOf[JUnitRunner])
    class FilteredInSuite extends FunSuite with BeforeAndAfterAll {
      test("JUnit ran this OK!") {
        assert(1 === 1)
      }
    }
  }

  import org.junit.runner.Description
  import org.junit.runner.JUnitCore
  import org.junit.runner.notification.Failure
  import org.junit.runner.notification.RunNotifier
  import org.scalatestplus.junit.helpers.EasySuite
  import org.scalatestplus.junit.helpers.KerblooeySuite
  import org.scalatestplus.junit.helpers.{FilteredInSuite, FilteredOutSuite, NameFilter}
  import scala.util.Try

  class JUnitRunnerSuite extends funsuite.AnyFunSuite {

    test("That EasySuite gets run by JUnit given its RunWith annotation") {
      val result = JUnitCore.runClasses(classOf[EasySuite])
      val easySuite = new EasySuite
      assert(result.getRunCount === easySuite.runCount) // EasySuite has 3 tests (excluding the ignored one)
      assert(result.getFailureCount === easySuite.failedCount) // EasySuite has 2 tests that blow up
      assert(result.getIgnoreCount === easySuite.ignoreCount) // EasySuite has 1 ignored test
    }

    //
    // This is a regression test for a problem where a failure was
    // sometimes not reported in Jenkins when a beforeAll method threw
    // an exception.
    //
    // The fix was to catch and report the exception as a failure
    // from JUnitRunner.run, instead of allowing the exception to
    // propagate up.
    //
    test("a test failure is reported due to an exception thrown from " +
         "beforeAll when JUnitRunner.run is called directly") {
      class MyRunNotifier extends RunNotifier {
        var methodInvocationCount = 0
        var passed: Option[Failure] = None
        override def fireTestFailure(failure: Failure): Unit = {
          methodInvocationCount += 1
          passed = Some(failure)
        }
      }
      val runNotifier = new MyRunNotifier

      (new JUnitRunner(classOf[KerblooeySuite])).run(runNotifier)

      import scala.language.reflectiveCalls

      assert(runNotifier.methodInvocationCount === 1)
      assert(runNotifier.passed.get.getDescription.getDisplayName ===
             "org.scalatestplus.junit.helpers.KerblooeySuite")
    }

    //
    // This test verifies that the fix tested above didn't break the
    // behavior seen when JUnit calls the JUnitRunner.
    //
    test("That a test failure is reported due to an exception thrown from " +
         "beforeAll when JUnitRunner is called from JUnit") {
      val result = JUnitCore.runClasses(classOf[KerblooeySuite])
      val kerblooeySuite = new KerblooeySuite
      assert(result.getRunCount === kerblooeySuite.runCount) 
      assert(result.getFailureCount === kerblooeySuite.failedCount) 
      assert(result.getIgnoreCount === kerblooeySuite.ignoreCount)
    }

    test("Test a suite can be filtered by name" +
      "as the runner implements filterable now")
    {
      val runNotifier =
        new RunNotifier {
          var ran: List[Description] = Nil
          override def fireTestFinished(description: Description): Unit = {
            ran = description :: ran
          }
        }

      val runners = (new JUnitRunner(classOf[FilteredOutSuite])) ::
                    (new JUnitRunner(classOf[FilteredInSuite])) :: Nil

      val filter = new NameFilter("FilteredIn")
      val filteredRunners = runners.flatMap(runner => Try{
        runner.filter(filter)
        runner
      }.toOption.toList)

      filteredRunners.foreach(_.run(runNotifier))
      assert(runNotifier.ran.size === 1)
      assert(runNotifier.ran.head.getDisplayName ===
        "JUnit ran this OK!(org.scalatestplus.junit.helpers.FilteredInSuite)")
    }
  }
}
