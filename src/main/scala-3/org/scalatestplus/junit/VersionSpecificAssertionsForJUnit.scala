package org.scalatestplus.junit

import org.scalactic.{Prettifier, source}
import org.scalatest.{Assertions, AssertionsMacro}
import org.scalatest.compatible.Assertion

trait VersionSpecificAssertionsForJUnit extends Assertions {
  inline override def assert(inline condition: Boolean)(implicit prettifier: Prettifier, pos: source.Position): Assertion =
    ${ AssertionsForJUnitMacro.assert('{condition}, '{prettifier}, '{pos}, '{""}) }

  inline override def assert(inline condition: Boolean, clue: Any)(implicit prettifier: Prettifier, pos: source.Position): Assertion =
    ${ AssertionsForJUnitMacro.assert('{condition}, '{prettifier}, '{pos}, '{clue}) }

  inline override def assume(inline condition: Boolean)(implicit prettifier: Prettifier, pos: source.Position): Assertion =
    ${ AssertionsForJUnitMacro.assume('{condition}, '{prettifier}, '{pos}, '{""}) }

  inline override def assume(inline condition: Boolean, clue: Any)(implicit prettifier: Prettifier, pos: source.Position): Assertion =
    ${ AssertionsForJUnitMacro.assume('{condition}, '{prettifier}, '{pos}, '{clue}) }

}
