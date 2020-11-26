package org.scalatestplus.junit

import org.scalactic.{Prettifier, source}
import org.scalatest.Assertions
import org.scalatest.AssertionsMacro.transform
import org.scalatest.compatible.Assertion

import scala.quoted.{Expr, Quotes}

object AssertionsForJUnitMacro {
  def assert(condition: Expr[Boolean], prettifier: Expr[Prettifier], pos: Expr[source.Position], clue: Expr[Any])(using Quotes): Expr[Assertion] =
    transform('{AssertionsForJUnit.assertionsHelper.macroAssert}, condition, prettifier, pos, clue)

  def assume(condition: Expr[Boolean], prettifier: Expr[Prettifier], pos: Expr[source.Position], clue: Expr[Any])(using Quotes): Expr[Assertion] =
    transform('{AssertionsForJUnit.assertionsHelper.macroAssume}, condition, prettifier, pos, clue)
}
