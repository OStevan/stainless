package stainless
package verification

import CoqExpression._

case class UnimplementedCoqExpression(msg: String) extends Exception(msg)

/**
 * Commands represent top-level Gallina declarations
 */
sealed abstract class CoqCommand {
  def coqString: String

  def $(c: CoqCommand) = Sequence(this,c)
}

case object NoCommand extends CoqCommand {
  override def coqString = ""
}

case class RequireImport(s: String) extends CoqCommand {
  override def coqString = s"Require Import $s."
}

case class OpenScope(s: String) extends CoqCommand {
  override def coqString = s"Open Scope $s."
}

case class Sequence(e1: CoqCommand, e2: CoqCommand) extends CoqCommand {
  override def coqString = e1.coqString + "\n" + e2.coqString
}

case class InductiveDefinition(id: CoqIdentifier, params: Seq[(CoqIdentifier,CoqExpression)], cases: Seq[InductiveCase]) extends CoqCommand {
  val paramString = params.map { case (arg,ty) => s"(${arg.coqString}: ${ty.coqString}) " }.mkString
  override def coqString = {
    // println("Translating: " + id.coqString)
   s"Inductive ${id.coqString} ${paramString}:=" +
    cases.map(_.coqString).mkString("\n","\n",".\n")
  }
}

case class FixpointDefinition(id: CoqIdentifier, params: Seq[(CoqIdentifier,CoqExpression)], returnType: CoqExpression, body: CoqExpression) extends CoqCommand {
  val paramString = params.map { case (arg,ty) => s"(${arg.coqString}: ${ty.coqString}) " }.mkString
  override def coqString = try {
    // println("Translating: " + id.coqString)
    s"Program Fixpoint ${id.coqString} ${paramString}: ${returnType.coqString} :=\n" +
      body.coqString + ".\n"
  } catch {
    case UnimplementedCoqExpression(_) =>
      println(s"Warning: could not translate ${id.coqString} to Coq. Admitting definition for now.")
      s"Definition ${id.coqString} ${paramString}: ${returnType.coqString}. Admitted."
  }
}

case class NormalDefinition(id: CoqIdentifier, params: Seq[(CoqIdentifier,CoqExpression)], returnType: CoqExpression, body: CoqExpression) extends CoqCommand {
  val paramString = params.map { case (arg,ty) => s"(${arg.coqString}: ${ty.coqString}) " }.mkString
  override def coqString = try {
    // println("Translating: " + id.coqString)
    s"Program Definition ${id.coqString} ${paramString}: ${returnType.coqString} :=\n" +
      body.coqString + ".\n"
  } catch {
    case UnimplementedCoqExpression(_) =>
      println(s"Warning: could not translate ${id.coqString} to Coq. Admitting definition for now.")
      s"Program Definition ${id.coqString} ${paramString}: ${returnType.coqString}. Admitted."
  }
}

// This class is used to represent the strings we want to print as is
case class RawCommand(s: String) extends CoqCommand {
  override def coqString = s
}

// This is used only for InductiveDefinition's
case class InductiveCase(constructor: CoqIdentifier, body: CoqExpression) {
  def coqString: String = {
    s"| ${constructor.coqString}: ${body.coqString}" 
  }
}


/**
  * Expressions describe Coq constructs for building terms/types/expressions
  */
sealed abstract class CoqExpression {
  def coqString: String

  def ===(that: CoqExpression) = CoqEquals(this,that)

  def apply(es: CoqExpression*) = {
    CoqApplication(this, es.toSeq)
  }
}

case object TypeSort extends CoqExpression {
  override def coqString: String = "Type"
}

case object CoqBool extends CoqExpression {
  override def coqString: String = "bool"
}

case object CoqZ extends CoqExpression {
  override def coqString: String = "Z"
}

case class Arrow(e1: CoqExpression, e2: CoqExpression) extends CoqExpression {
  def coqString: String = {
    optP(e1) + " -> " + optP(e2)
  }
}

case class CoqMatch(matched: CoqExpression, cases: Seq[CoqCase]) extends CoqExpression {
  override def coqString = 
    s"match ${matched.coqString} with" +
      cases.map(_.coqString).mkString("\n","\n","\nend")
}

case class CoqApplication(f: CoqExpression, args: Seq[CoqExpression]) extends CoqExpression {
  override def coqString = optP(f) + args.map(arg => " " + optP(arg)).mkString
}

case class CoqIdentifier(id: Identifier) extends CoqExpression {
  override def coqString = {
    val res = id.name.replaceAll("\\$","___")
      .replaceAll("::", "cons_")
      .replaceAll(":\\+", "snoc_")
      .replaceAll(":", "i_")
      .replaceAll("\\+", "plus_")
      .replaceAll("\\+\\+", "union_")
      .replaceAll("--", "substract_")
      .replaceAll("-", "minus_")
      .replaceAll("&", "c_")
    if (coqKeywords contains res) coqKeywords(res) 
    else if (validCoqIdentifier(res)) res
    else throw new Exception(s"$res is not a valid coq identifier")
  }
}

case class CoqTuple(es: Seq[CoqExpression]) extends CoqExpression {
  override def coqString = {
    es.map(_.coqString).mkString("(", ",", ")")
  }
}

case class CoqLibraryConstant(s: String) extends CoqExpression {
  override def coqString = s
}

case class Constructor(id: CoqExpression, args: Seq[CoqExpression]) extends CoqExpression {
  override def coqString = id.coqString + args.map(arg => " " + optP(arg)).mkString
}

case class CoqForall(args: Seq[(CoqIdentifier,CoqExpression)], body: CoqExpression) extends CoqExpression {
  override def coqString = 
    /*propInBool.coqString + */"(" + args.foldLeft(body.coqString) { case (acc,(id,tpe)) => 
      s"forall ${id.coqString}: ${tpe.coqString}, $acc"
    } + ")"
}

case class CoqLet(vd: CoqIdentifier, value: CoqExpression, body: CoqExpression) extends CoqExpression {
  override def coqString = s"let ${vd.coqString} := (${value.coqString}) in (${body.coqString})"
}

case class CoqLambda(vd: CoqIdentifier, body: CoqExpression) extends CoqExpression {
  override def coqString = s"fun ${vd.coqString} => ${body.coqString} "
}

// This class is used to represent the strings we want to print as is
case class RawExpression(s: String) extends CoqExpression {
  override def coqString = s
}

/**
 * Boolean operations and propositions
 */

case class Orb(es: Seq[CoqExpression]) extends CoqExpression {
  override def coqString = fold(FalseBoolean.coqString, es.map(_.coqString)) { case (a,b) => s"$a || $b" }
}

case class Andb(es: Seq[CoqExpression]) extends CoqExpression {
  override def coqString = fold(TrueBoolean, es) { 
    case (a,b) => ifthenelse(a, CoqBool, CoqLambda(coqUnused , b), CoqLambda(coqUnused, FalseBoolean))
  }.coqString
}

case class Negb(e: CoqExpression) extends CoqExpression {
  override def coqString = negbFun(e).coqString
}
//todo remove
case object TrueBoolean extends CoqExpression {
  override def coqString = "true"
}

case object FalseBoolean extends CoqExpression {
  override def coqString = "false"
}

case class CoqEquals(e1: CoqExpression, e2: CoqExpression) extends CoqExpression {
  override def coqString = /*propInBool.coqString + */"(" + e1.coqString + " = " + e2.coqString + ")"
}

case class CoqZNum(i: BigInt) extends CoqExpression {
  override def coqString = s"($i)%Z"
}

/**
 * Greater or equals
 */ 

/*case class GreB(*e1: CoqExpression, e2:CoqExpression) {
  override def coqString = 
}*/

case class CoqTupleType(ts: Seq[CoqExpression]) extends CoqExpression {
  override def coqString = ts.map(optP).mkString("(", " * ", ")%type")
}

/**
 * Set Operations
 */
case object CoqUnknown extends CoqExpression {
  override def coqString = "_"
}

case class CoqFiniteSet(args: Seq[CoqExpression], tpe: CoqExpression) extends CoqExpression {
  override def coqString = throw new UnimplementedCoqExpression("Finite Sets are not implemented yet.")
}

case class CoqSetUnion(e1: CoqExpression, e2: CoqExpression) extends CoqExpression {
  override def coqString = throw new UnimplementedCoqExpression("Union of Sets are not implemented yet.")
}

case class CoqSetType(base: CoqExpression) extends CoqExpression {
  override def coqString = s"set (${base.coqString})"
}

case class CoqBelongs(e1: CoqExpression, e2: CoqExpression) extends CoqExpression {
  override def coqString = throw new UnimplementedCoqExpression("Set membership is not implemented yet.")
}

// represents the refinement of the type `tpe` by `body`, i.e. {id: tpe | body}
case class Refinement(id: CoqIdentifier, tpe: CoqExpression, body: CoqExpression) extends CoqExpression {
  def coqString: String = try {
    s"{${id.coqString}: ${tpe.coqString} | ${body.coqString}}"
  } catch {
    case UnimplementedCoqExpression(_) =>
      println(s"IMPORTANT WARNING (Soundness): could not refine type $tpe by $body, due to unimplemented operations")
      s"{${id.coqString}: ${tpe.coqString} |   True}"
  }
}

// This class is used to represent the expressions for which we didn't make a construct
case class UnimplementedExpression(s: String) extends CoqExpression {
  override def coqString = throw new UnimplementedCoqExpression(s)
}

// used in the CoqMatch construct
case class CoqCase(pattern: CoqPattern, body: CoqExpression) {
  def coqString: String = {
    s"| ${pattern.coqString} => ${body.coqString}" 
  }
}

/**
  * Patterns are used as the left-hand-side for match cases
  */
abstract class CoqPattern {
  def coqString: String
}

case class InductiveTypePattern(id: CoqIdentifier, subPatterns: Seq[CoqPattern]) extends CoqPattern {
  override def coqString = id.coqString + subPatterns.map((p: CoqPattern) => 
    " " + optP(p)
  ).mkString
}

case class VariablePattern(id: Option[CoqIdentifier]) extends CoqPattern {
  override def coqString = if (id.isEmpty) "_" else id.get.coqString
}


case class CoqTuplePatternVd(ps: Seq[CoqPattern], vd: VariablePattern) extends CoqPattern {
  override def coqString = {
    ps.map(_.coqString).mkString("(", ",", ")") + " as " + vd.coqString
  }
}

case class CoqTuplePattern(ps: Seq[CoqPattern]) extends CoqPattern {
  override def coqString = {
    ps.map(_.coqString).mkString("(", ",", ")")
  }
}

object CoqExpression {
  def fold[T](baseCase: T, exprs: Seq[T])(operation: (T,T) => T) = {
    if (exprs.size == 0) baseCase
    else exprs.tail.foldLeft(exprs.head)(operation)
  }

  val implbFun = CoqLibraryConstant("implb")
  val andbFun = CoqLibraryConstant("andb")
  val orbFun = CoqLibraryConstant("orb")
  val negbFun = CoqLibraryConstant("negb")
  val trueProp = CoqLibraryConstant("True")
  val falseProp = CoqLibraryConstant("False")
  val propSort = CoqLibraryConstant("Prop")
  val propInBool = CoqLibraryConstant("propInBool")
  val magic = CoqLibraryConstant("magic")
  val typeSort = CoqLibraryConstant("Type")
  val mapType = CoqLibraryConstant("map_type")
  val ifthenelse = CoqLibraryConstant("ifthenelse")

  val coqUnused = CoqIdentifier(new Identifier("_", 0,0))

  def implb(e1: CoqExpression, e2: CoqExpression): CoqExpression = {
    CoqApplication(implbFun, Seq(e1,e2))
  }

  // we require too many parentheses!
  // we recompute coqString too many times
  def requiresParentheses(e: CoqExpression): Boolean = e.coqString.contains(" ")
  def requiresParentheses(e: CoqPattern): Boolean = e.coqString.contains(" ")

  def optP(e: CoqExpression) = if (requiresParentheses(e)) s"(${e.coqString})" else e.coqString
  def optP(e: CoqPattern) = if (requiresParentheses(e)) s"(${e.coqString})" else e.coqString

  def validCoqIdentifier(s: String) = s matches """[A-Z|a-z|_][\w_]*"""

  val coqKeywords = Map(
    "forall" -> "_forall",
    "exists" -> "_exists",
    "exists2" -> "_exists2"
  )

  // FIXME: not thread safe
  //var m: Map[String,String] = Map()
}