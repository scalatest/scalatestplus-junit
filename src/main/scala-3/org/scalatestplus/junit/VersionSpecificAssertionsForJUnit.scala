package org.scalatestplus.junit

import org.scalactic.{Prettifier, source}
import org.scalatest.{Assertions, AssertionsMacro}
import org.scalatest.compatible.Assertion

trait VersionSpecificAssertionsForJUnit extends Assertions {
  // https://github.com/lampepfl/dotty/pull/8601#pullrequestreview-380646858
  implicit object UseJUnitAssertions

  inline def assert(inline condition: Boolean)(implicit prettifier: Prettifier, pos: source.Position, use: UseJUnitAssertions.type): Assertion =
    ${ AssertionsForJUnitMacro.assert('{condition}, '{prettifier}, '{pos}, '{""}) }

  inline def assert(inline condition: Boolean, clue: Any)(implicit prettifier: Prettifier, pos: source.Position, use: UseJUnitAssertions.type): Assertion =
    ${ AssertionsForJUnitMacro.assert('{condition}, '{prettifier}, '{pos}, '{clue}) }

  inline def assume(inline condition: Boolean)(implicit prettifier: Prettifier, pos: source.Position, use: UseJUnitAssertions.type): Assertion =
    ${ AssertionsForJUnitMacro.assume('{condition}, '{prettifier}, '{pos}, '{""}) }

  inline def assume(inline condition: Boolean, clue: Any)(implicit prettifier: Prettifier, pos: source.Position, use: UseJUnitAssertions.type): Assertion =
    ${ AssertionsForJUnitMacro.assume('{condition}, '{prettifier}, '{pos}, '{clue}) }

}
