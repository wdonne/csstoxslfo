package net.pincette.csstoxslfo;

import static java.util.Optional.ofNullable;
import static net.pincette.css.sac.Selector.SAC_CHILD_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_CONDITIONAL_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_DESCENDANT_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_DIRECT_ADJACENT_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_ELEMENT_NODE_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_PSEUDO_ELEMENT_SELECTOR;
import static net.pincette.csstoxslfo.util.Util.createTreeSet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.pincette.css.sac.Condition;
import net.pincette.css.sac.ConditionalSelector;
import net.pincette.css.sac.DescendantSelector;
import net.pincette.css.sac.ElementSelector;
import net.pincette.css.sac.Selector;
import net.pincette.css.sac.SiblingSelector;
import net.pincette.csstoxslfo.util.DigitalTree;

/**
 * Represents a CSS style sheet in compiled form.
 *
 * @author Werner Donné
 */
class Compiled {
  static final String ANY_ELEMENT = "*|*";
  static final String SIBLING = "SIBLING";
  private static final String EPSILON = "EPSILON";
  private static final boolean trace = System.getProperty("net.pincette.csstoxslfo.trace") != null;
  DFAState startState = null;
  private int dfaStateCounter = 0;
  private int nfaStateCounter = 0;
  private final NFAStateConnection nfa = new NFAStateConnection(new NFAState(), new NFAState());

  private static Map<Object, SortedSet<NFAState>> collectNextSets(final SortedSet<NFAState> set) {
    final Map<Object, SortedSet<NFAState>> result = new HashMap<>();

    for (final NFAState state : set) {
      for (final Next next : state.next) {
        if (next.event != EPSILON) {
          result.computeIfAbsent(next.event, k -> new TreeSet<>(set.comparator())).add(next.state);
        }
      }
    }

    for (final SortedSet<NFAState> nextSet : result.values()) {
      for (final NFAState state : createTreeSet(nextSet, nextSet.comparator())) {
        epsilonMove(nextSet, state);
      }
    }

    return result;
  }

  private static NFAStateConnection constructAnd(
      final NFAStateConnection first, final NFAStateConnection second) {
    first.end.next.add(new Next(EPSILON, second.start));

    return new NFAStateConnection(first.start, second.end);
  }

  private static void dumpDFA(
      final DFAState state, final Set<Integer> seen, final PrintWriter out) {
    if (seen.contains(state.state)) {
      return;
    }

    out.println(state.state + ":");

    final List<DFAState> values = new ArrayList<>();

    for (final String event : state.events.keySet()) {
      final DFAState nextState = state.events.get(event);

      out.println("  " + event + " -> " + nextState.state);
      values.add(nextState);
    }

    for (Condition event : state.candidateConditions.keySet()) {
      final DFAState nextState = state.candidateConditions.get(event);

      out.println("  " + Util.conditionText(event) + " -> " + nextState.state);
      values.add(nextState);
    }

    dumpRules(state.rules, out);
    dumpRules(state.pseudoRules, out);
    out.println();
    seen.add(state.state);

    for (final DFAState s : values) {
      dumpDFA(s, seen, out);
    }
  }

  private static void dumpNFA(
      final NFAState state, final Set<Integer> seen, final PrintWriter out) {
    if (seen.contains(state.state)) {
      return;
    }

    out.println(state.state + ":");

    state.next.forEach(
        next ->
            out.println(
                "  "
                    + (next.event instanceof Condition e
                        ? Util.conditionText(e)
                        : next.event.toString())
                    + " -> "
                    + next.state.state));

    dumpRules(state.rules, out);
    dumpRules(state.pseudoRules, out);
    out.println();
    seen.add(state.state);
    state.next.forEach(next -> dumpNFA(next.state, seen, out));
  }

  private static void dumpRules(final List<Rule> rules, final PrintWriter out) {
    rules.forEach(
        rule ->
            out.println(
                "  "
                    + (rule.getElementName() != null ? rule.getElementName() : "")
                    + (rule.getPseudoElementName() != null ? rule.getPseudoElementName() : "")
                    + ": "
                    + rule.getProperty().getName()
                    + ": "
                    + rule.getProperty().getValue()));
  }

  private static void epsilonMove(final Set<NFAState> set, final NFAState state) {
    state.next.stream()
        .filter(next -> next.event.equals(EPSILON) && set.add(next.state))
        .forEach(next -> epsilonMove(set, next.state));
  }

  private static String label(final SortedSet<NFAState> set) {
    return set.stream()
        .map(state -> "#" + state.state)
        .reduce(new StringBuilder(), StringBuilder::append, (b1, b2) -> b1)
        .toString();
  }

  /**
   * Adds the rule to the NFA being built using the Thompson construction. The rule should be split,
   * i.e., it should have exactly one property.
   */
  void addRule(final Rule rule) {
    final NFAStateConnection states = constructNFA(rule.getSelector());

    if (rule.getPseudoElementName() == null) {
      ofNullable(states).ifPresent(s -> s.end.rules.add(rule));
    } else {
      ofNullable(states).ifPresent(s -> s.end.pseudoRules.add(rule));
    }

    ofNullable(states)
        .ifPresent(
            s -> {
              nfa.start.next.add(new Next(EPSILON, s.start));
              s.end.next.add(new Next(EPSILON, nfa.end));
            });
  }

  private NFAStateConnection constructChild(final DescendantSelector selector) {
    return selector.getSimpleSelector().getSelectorType() == SAC_PSEUDO_ELEMENT_SELECTOR
        ? constructNFA(selector.getAncestorSelector())
        : constructAnd(
            constructNFA(selector.getAncestorSelector()),
            constructNFA(selector.getSimpleSelector()));
  }

  private NFAStateConnection constructConditional(final ConditionalSelector selector) {
    final NFAStateConnection first = constructNFA(selector.getSimpleSelector());
    final NFAState end = new NFAState();

    first.end.next.add(new Next(selector.getCondition(), end));

    return new NFAStateConnection(first.start, end);
  }

  private NFAStateConnection constructDescendant(final DescendantSelector selector) {
    return constructAnd(
        constructAnd(
            constructNFA(selector.getAncestorSelector()), constructKleeneClosure(ANY_ELEMENT)),
        constructNFA(selector.getSimpleSelector()));
  }

  private NFAStateConnection constructElement(final ElementSelector selector) {
    final NFAState start = new NFAState();
    final NFAState end = new NFAState();

    start.next.add(
        new Next(
            ofNullable(selector.getNamespaceURI()).orElse("*")
                + "|"
                + ofNullable(selector.getLocalName()).orElse("*"),
            end));

    return new NFAStateConnection(start, end);
  }

  private NFAStateConnection constructKleeneClosure(final Object event) {
    final NFAState start = new NFAState();
    final NFAState end = new NFAState();
    final NFAState from = new NFAState();
    final NFAState to = new NFAState();

    from.next.add(new Next(event, to));
    start.next.add(new Next(EPSILON, from));
    start.next.add(new Next(EPSILON, end));
    to.next.add(new Next(EPSILON, end));
    end.next.add(new Next(EPSILON, from));

    return new NFAStateConnection(start, end);
  }

  /** Applies the Thompson construction. */
  private NFAStateConnection constructNFA(final Selector selector) {
    return switch (selector.getSelectorType()) {
      case SAC_CONDITIONAL_SELECTOR -> constructConditional((ConditionalSelector) selector);
      case SAC_CHILD_SELECTOR -> constructChild((DescendantSelector) selector);
      case SAC_DESCENDANT_SELECTOR -> constructDescendant((DescendantSelector) selector);
      case SAC_DIRECT_ADJACENT_SELECTOR -> constructSibling((SiblingSelector) selector);
      case SAC_ELEMENT_NODE_SELECTOR -> constructElement((ElementSelector) selector);
      default -> new NFAStateConnection(null, null); // Ignore non-CSS2 selector types.
    };
  }

  private NFAStateConnection constructSibling(final SiblingSelector selector) {
    return constructAnd(
        constructAnd(constructNFA(selector.getSelector()), constructSiblingTransition()),
        constructNFA(selector.getSiblingSelector()));
  }

  private NFAStateConnection constructSiblingTransition() {
    final NFAState start = new NFAState();
    final NFAState end = new NFAState();

    start.next.add(new Next(SIBLING, end));

    return new NFAStateConnection(start, end);
  }

  void dumpDFA(final PrintWriter out) {
    if (trace) {
      out.println();
      out.println("DFA START");
      out.println();
      dumpDFA(startState, new HashSet<>(), out);
      out.println("DFA END");
      out.println();
      out.flush();
    }
  }

  void dumpNFA(final PrintWriter out) {
    if (trace) {
      out.println();
      out.println("NFA START");
      out.println();
      dumpNFA(nfa.start, new HashSet<>(), out);
      out.println("NFA END");
      out.println();
      out.flush();
    }
  }

  @SuppressWarnings("java:S106") // Not logging.
  void generateDFA() {
    dumpNFA(new PrintWriter(System.out));
    startState = generateDFA(nfa);
    dumpDFA(new PrintWriter(System.out));
  }

  /** Applies the subset construction. Returns the start state. */
  private DFAState generateDFA(final NFAStateConnection nfa) {
    final SortedSet<NFAState> set =
        new TreeSet<>
        // Sorting of the NFA states makes the labels unique.
        ((o1, o2) -> o1.state - o2.state);
    final Map<String, DFAState> states = new HashMap<>();

    set.add(nfa.start);
    epsilonMove(set, nfa.start);

    final DFAState result = new DFAState();

    states.put(label(set), result);
    generateTransitions(result, set, states);

    return result;
  }

  private void generateTransitions(
      final DFAState from, final SortedSet<NFAState> set, final Map<String, DFAState> states) {
    collectNextSets(set).entrySet().stream()
        .filter(e -> !e.getValue().isEmpty())
        .forEach(
            e -> {
              DFAState nextState;
              final String s = label(e.getValue());
              final DFAState state = states.get(s);

              if (state == null) {
                nextState = new DFAState();
                states.put(s, nextState);
              } else {
                nextState = state;
              }

              if (e.getKey() instanceof Condition c) {
                from.candidateConditions.put(c, nextState);
              } else {
                from.events.put((String) e.getKey(), nextState);
              }

              e.getValue()
                  .forEach(
                      next -> {
                        nextState.rules.addAll(next.rules);
                        nextState.pseudoRules.addAll(next.pseudoRules);
                      });

              if (state == null) {
                generateTransitions(nextState, e.getValue(), states);
              }
            });
  }

  private record NFAStateConnection(NFAState start, NFAState end) {}

  private record Next(Object event, NFAState state) {}

  /** Contains all matching rules sorted from least to most specific. */
  class DFAState {
    final Map<Condition, DFAState> candidateConditions = new HashMap<>();
    final DigitalTree<DFAState> events = new DigitalTree<>(trace);
    final List<Rule> pseudoRules = new ArrayList<>();
    final List<Rule> rules = new ArrayList<>();
    int state;

    private DFAState() {
      state = dfaStateCounter++;
    }
  }

  private class NFAState {
    private final List<Next> next = new ArrayList<>();
    private final List<Rule> pseudoRules = new ArrayList<>();
    private final List<Rule> rules = new ArrayList<>();
    private final int state;

    private NFAState() {
      state = nfaStateCounter++;
    }
  }
}
