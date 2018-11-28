package stainless.frontends.fast.irs

import stainless.frontends.fast.IRs

trait PatternMatching { self: IRs =>
  object PatternMatching {
    case class MatchExpression(lhs: Exprs.Expr, cases: Seq[MatchCase]) extends Exprs.Expr {
      override def getHoles: Seq[Hole] = lhs.getHoles ++ cases.flatMap(_.getHoles)
    }

    case class MatchCase(pattern: Pattern, optGuard: Option[Exprs.Expr], rhs: Exprs.Expr) extends IR {
      override def getHoles: Seq[Hole] = optGuard match {
        case Some(x) =>
          pattern.getHoles ++ x.getHoles ++ rhs.getHoles
        case None => pattern.getHoles ++ rhs.getHoles
      }
    }

    abstract class Pattern extends IR {
      override def getHoles: Seq[Hole] = this match {
        case WildcardPattern(Some(binder)) => binder.getHoles
        case ADTPattern(binder, id, tps, subPatterns) => (binder match {
          case Some(a) => a.getHoles
          case None => Seq.empty
        }) ++ id.getHoles ++ tps.flatMap(_.getHoles) ++ subPatterns.flatMap(_.getHoles)
        case TuplePattern(binder, subPatterns) => binder match {
          case Some(a) => a.getHoles ++ subPatterns.flatMap(_.getHoles)
          case None => Seq.empty
        }
        case LiteralPattern(binder, lit) => binder match {
          case Some(a) => a.getHoles
          case None => Seq.empty
        }
        case _ => Seq.empty
      }
    }

    case class WildcardPattern(binder: Option[Bindings.Binding]) extends Pattern
    case class ADTPattern(binder: Option[Bindings.Binding], id: Identifiers.Identifier,
                          tps: Seq[Types.Type], subPatterns: Seq[Pattern]) extends Pattern
    case class TuplePattern(binder: Option[Bindings.Binding], subPatterns: Seq[Pattern]) extends Pattern
    case class LiteralPattern(binder: Option[Bindings.Binding],
                                  lit: Exprs.Literal) extends Pattern
  }
}
