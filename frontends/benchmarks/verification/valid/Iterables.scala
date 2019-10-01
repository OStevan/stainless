
import stainless.lang._
import stainless.collection._
import stainless.annotation._

object Iterables {

  def test_setToList(set: Set[BigInt]) = {
    require(set.contains(1) && set.contains(2) && !set.contains(3))

    val res = set.toList

    assert(res.contains(1))
    assert(res.contains(2))
    assert(!res.contains(3))
  }

  def test_setMap(set: Set[BigInt]) = {
    require(set.contains(1) && set.contains(2) && !set.contains(3))

    val res = set.map(_ + 1)

    assert(res.contains(2))
    assert(res.contains(3))
    assert(!res.contains(4))
  }

  def test_mapKeys(map: Map[Int, String]) = {
    require(map.contains(1) && map.contains(2) && !map.contains(3))

    val res = map.keys

    assert(res.contains(1))
    assert(res.contains(2))
    assert(!res.contains(3))
  }

  def test_mapValues(map: Map[Int, String]) = {
    require(map.get(1) == Some("foo") && map.get(2) == Some("bar"))

    val res = map.values

    assert(res.contains("foo"))
    assert(res.contains("bar"))
  }

  def test_mapToList(map: Map[Int, String]) = {
    require(map.get(1) == Some("foo") && map.get(2) == Some("bar"))

    val res = map.toList

    assert(res.contains((1, "foo")))
    assert(res.contains((2, "bar")))
  }
}