package ch.davidfrank.regexsolver

import dk.brics.automaton.Automaton
import dk.brics.automaton.RegExp
import dk.brics.automaton.State
import dk.brics.automaton.Transition
import java.util.*

class Solver(val left: List<String>, val top: List<String>,
             val right: List<String>, val bottom: List<String>) {

  private val rowAutomata: List<Automaton> = left.zip(right).map {
    RegExp(it.first).toAutomaton(true).intersection(RegExp(it.second).toAutomaton(true))
  }
  private val columnAutomata: List<Automaton> = top.zip(bottom).map {
    RegExp(it.first).toAutomaton(true).intersection(RegExp(it.second).toAutomaton(true))
  }

  private val rows = left.size
  private val columns = top.size

  private val currentChars = CharTable(rows, columns)
  private val currentStates = StateTable()

  fun solve(): Solution? {
    return if (tryField(0)) currentChars.toSolution()
    else null
  }

  private fun tryField(field: Int): Boolean {
    val row = field / columns
    val column = field % columns

    val joined = join(currentStates[row, column - 1].first, currentStates[row - 1, column].second)
    for ((char, rowState, columnState) in joined) {
      currentStates[row, column] = Pair(rowState, columnState)
      currentChars[row, column] = char
      if (row == rows - 1 && !columnState.isAccept) continue
      if (column == columns - 1 && !rowState.isAccept) continue
      if (field == rows * columns - 1) {
        return true
      } else {
        if (tryField(field + 1)) {
          return true
        }
      }
    }
    return false
  }

  private fun join(rowState: State?, columnState: State?): List<Triple<Char, State, State>> {
    if (rowState == null || columnState == null) {
      return emptyList()
    }
    val joinResult = ArrayList<Triple<Char, State, State>>()
    for (transition1 in rowState.transitions) {
      for (transition2 in columnState.transitions) {
        val char = joinTransitions(transition1, transition2)
        if (char != null) {
          joinResult += Triple(char, transition1.dest, transition2.dest)
        }
      }
    }
    return joinResult
  }

  private fun joinTransitions(trans1: Transition, trans2: Transition): Char? {
    val min = if (trans1.min < trans2.min) trans2.min else trans1.min
    val max = if (trans1.max < trans2.max) trans1.max else trans2.max

    if (min <= max) {
      return min
    }
    return null
  }

  private class CharTable(val rows: Int, val columns: Int) {
    private val chars = CharArray(rows * columns)
    operator fun get(row: Int, column: Int): Char =
        chars[row * columns + column]

    operator fun set(row: Int, column: Int, value: Char) {
      chars[row * columns + column] = value
    }

    fun toSolution(): Solution =
        Solution((0..rows - 1).map { row ->
          buildString {
            for (col in 0..columns - 1) {
              append(chars[row * columns + col])
            }
          }
        })

  }

  private inner class StateTable {
    private val currentStates: Array<Pair<State?, State?>> = initCurrentStates()

    private fun initCurrentStates(): Array<Pair<State?, State?>> {
      val states = Array<Pair<State?, State?>>((rows + 1) * (columns + 1), { Pair(null, null) })
      rowAutomata.forEachIndexed { row, automaton ->
        states[(row + 1) * (columns + 1)] = Pair(automaton.initialState, null)
      }
      columnAutomata.forEachIndexed { column, automaton ->
        states[column + 1] = Pair(null, automaton.initialState)
      }
      return states
    }

    operator fun get(row: Int, column: Int): Pair<State?, State?> =
        currentStates[(row + 1) * (columns + 1) + column + 1]

    operator fun set(row: Int, column: Int, value: Pair<State, State>) {
      currentStates[(row + 1) * (columns + 1) + column + 1] = value
    }

  }
}

fun main(args: Array<String>) {
  //
  //  println(Solver(
  //      listOf("[UGLER]*", "[CAST]*REX[PEA]*", "[SIRES]*", "(L|OFT|ON)*", "H*(AY|ED)*"),
  //      listOf("[ARK]*O.*", "[TUBE]*", "[BORF]."))
  //      .solve())

  //  println(Solver(
  //      listOf("[A-GN-Z]+"),
  //      listOf("[D-HJ-M]", "[^A-RU-Z]"),
  //      listOf("[^A-DI-S]+"),
  //      listOf("[^F-KM-Z]", "[A-KS-V]")
  //  ).solve())

  println(Solver(
      listOf("[[HEL ]+P.+", "[MI/SON]+[^OLDE]{4}", "[IN'THE\\. ]+", ".[A-G]+(R|D)+[END]+"),

      listOf("[O-S G-L]+", "[ANTIGE]+", "(S | S|'A)+", "[PI RD]+",
          "(TD|L|LO|O|OH)+", "[HITE' ]+", "[MENDS]+"),
      listOf(".[SEPOLI ]+", ".{3,4}( H| |IM)+", "[IT' ]{4}[H.TE]+", ".{4}(NI|TE|N|DE)+"),
      listOf("(  |OR|HO|ME)+", "[A-G]N+(GI|IG|PI)", "[RAM ES']+", "[^AINED]+",
          "[HORTED]+", "[F-K]{2}[F-M]..?", "(S|I|MS)[MYEND]*")
  ).solve())

  //  println(Solver(
  //      listOf("a[a-g]", ".*"),
  //      listOf(".*", ".r"))
  //      .solve())
}
