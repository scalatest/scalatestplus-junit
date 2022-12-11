package org.scalatestplus.junit

import org.scalactic.{Prettifier, source}
import org.scalatest.{Assertion, Assertions}

trait VersionSpecificAssertionsForJUnit5 extends Assertions {
  import scala.language.experimental.macros

  override def assert(condition: Boolean)(implicit prettifier: Prettifier, pos: source.Position): Assertion = macro AssertionsForJUnit5Macro.assert

  override def assert(condition: Boolean, clue: Any)(implicit prettifier: Prettifier, pos: source.Position): Assertion = macro AssertionsForJUnit5Macro.assertWithClue

  override def assume(condition: Boolean)(implicit prettifier: Prettifier, pos: source.Position): Assertion = macro AssertionsForJUnit5Macro.assume

  override def assume(condition: Boolean, clue: Any)(implicit prettifier: Prettifier, pos: source.Position): Assertion = macro AssertionsForJUnit5Macro.assumeWithClue
}
