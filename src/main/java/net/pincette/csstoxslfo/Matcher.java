package net.pincette.csstoxslfo;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static net.pincette.css.sac.Condition.SAC_AND_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_CLASS_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ID_CONDITION;
import static net.pincette.css.sac.Condition.SAC_LANG_CONDITION;
import static net.pincette.css.sac.Condition.SAC_NEGATIVE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ONE_OF_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_OR_CONDITION;
import static net.pincette.css.sac.Condition.SAC_POSITIONAL_CONDITION;
import static net.pincette.css.sac.Condition.SAC_PSEUDO_CLASS_CONDITION;
import static net.pincette.css.sac.DocumentHandler.SAC_NO_URI;
import static net.pincette.csstoxslfo.Compiled.ANY_ELEMENT;
import static net.pincette.csstoxslfo.Compiled.SIBLING;
import static net.pincette.csstoxslfo.Constants.XHTML;
import static net.pincette.csstoxslfo.Property.AFTER;
import static net.pincette.csstoxslfo.Property.BEFORE;
import static net.pincette.csstoxslfo.Util.ID;
import static net.pincette.csstoxslfo.Util.LANG;
import static net.pincette.csstoxslfo.Util.LOGGER;
import static net.pincette.util.Or.tryWith;
import static net.pincette.xml.sax.Util.attributes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.pincette.css.sac.AttributeCondition;
import net.pincette.css.sac.CombinatorCondition;
import net.pincette.css.sac.Condition;
import net.pincette.css.sac.LangCondition;
import net.pincette.css.sac.NegativeCondition;
import net.pincette.css.sac.PositionalCondition;
import net.pincette.xml.sax.Attribute;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Finds the matching rules as the document goes through it.
 *
 * @author Werner Donné
 */
class Matcher implements ContentHandler {
  private static final String DEFAULT_LANGUAGE = "en-GB";
  private static final String FIRST_CHILD = "first-child";
  private final Deque<Element> elements = new ArrayDeque<>();
  private final Compiled.DFAState startState;

  public Matcher(Compiled styleSheet) {
    startState = styleSheet.startState;
  }

  private static boolean checkAttributeCondition(
      final Element e,
      final AttributeCondition c,
      final BiPredicate<Attribute, AttributeCondition> test) {
    return attributes(e.attributes)
        .anyMatch(
            a ->
                a.localName.equals(c.getLocalName())
                    && test.test(a, c)
                    && ofNullable(c.getNamespaceURI())
                        .map(n -> SAC_NO_URI.equals(n) || n.equals(a.namespaceURI))
                        .orElse(true));
  }

  private static boolean checkAttributeCondition(final Element e, final AttributeCondition c) {
    return checkAttributeCondition(
        e, c, (a, co) -> ofNullable(co.getValue()).map(v -> v.equals(a.value)).orElse(true));
  }

  private static boolean checkBeginHyphenAttributeCondition(
      final Element e, final AttributeCondition c) {
    return checkAttributeCondition(
        e, c, (a, co) -> a.value.startsWith(co.getValue() + "-") || a.value.equals(co.getValue()));
  }

  private static boolean checkClassCondition(final Element e, final AttributeCondition c) {
    return ofNullable(e.attributes.getValue("class"))
        .map(v -> hasToken(v, c.getValue()))
        .orElse(false);
  }

  private static boolean checkCondition(final Element e, final Condition c) {
    return switch (c.getConditionType()) {
      case SAC_AND_CONDITION ->
          checkCondition(e, ((CombinatorCondition) c).getFirstCondition())
              && checkCondition(e, ((CombinatorCondition) c).getSecondCondition());
      case SAC_ATTRIBUTE_CONDITION -> checkAttributeCondition(e, (AttributeCondition) c);
      case SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION ->
          checkBeginHyphenAttributeCondition(e, (AttributeCondition) c);
      case SAC_CLASS_CONDITION -> checkClassCondition(e, (AttributeCondition) c);
      case SAC_ID_CONDITION -> checkIdCondition(e, (AttributeCondition) c);
      case SAC_LANG_CONDITION -> checkLangCondition(e, (LangCondition) c);
      case SAC_NEGATIVE_CONDITION -> !checkCondition(e, ((NegativeCondition) c).getCondition());
      case SAC_ONE_OF_ATTRIBUTE_CONDITION ->
          checkOneOfAttributeCondition(e, (AttributeCondition) c);
      case SAC_OR_CONDITION ->
          checkCondition(e, ((CombinatorCondition) c).getFirstCondition())
              || checkCondition(e, ((CombinatorCondition) c).getSecondCondition());
      case SAC_POSITIONAL_CONDITION ->
          checkPositionalCondition(e, ((PositionalCondition) c).getPosition());
      case SAC_PSEUDO_CLASS_CONDITION -> checkPseudoClassCondition(e, (AttributeCondition) c);
      default -> false; // Ignore non-CSS2 or irrelevant condition types.
    };
  }

  private static boolean checkIdCondition(final Element e, final AttributeCondition c) {
    return attributes(e.attributes)
        .anyMatch(a -> ID.equals(a.type) && c.getValue().equals(a.value));
  }

  private static boolean checkLangCondition(final Element e, final LangCondition c) {
    return e.language.startsWith(c.getLang() + "-") || e.language.equals(c.getLang());
  }

  private static boolean checkOneOfAttributeCondition(final Element e, final AttributeCondition c) {
    return checkAttributeCondition(e, c, (a, co) -> hasToken(a.value, c.getValue()));
  }

  private static boolean checkPositionalCondition(final Element e, final int position) {
    // The element on the top of the stack is not yet in the child list of its
    // parent. The preceding sibling is the last element in the parent's child
    // list.

    return e.parent.children.size() == position;
  }

  private static boolean checkPseudoClassCondition(final Element e, final AttributeCondition c) {
    return AFTER.equals(c.getValue())
        || BEFORE.equals(c.getValue())
        || (FIRST_CHILD.equals(c.getValue()) && checkPositionalCondition(e, 0));
  }

  private static Comparator<Rule> comparator() {
    return comparing((Rule rule) -> rule.getProperty().getName())
        .thenComparing(rule -> !rule.getProperty().getImportant() ? 0 : 1)
        .thenComparing(Rule::getSpecificity)
        .thenComparing(Rule::getPosition);
  }

  private static Collection<Compiled.DFAState> getSiblingStates(
      final Collection<Compiled.DFAState> states) {
    return states.stream()
        .map(state -> state.events.get(SIBLING))
        .filter(Objects::nonNull)
        .collect(toSet());
  }

  private static boolean hasToken(final String s, final String token) {
    return Optional.of(s.indexOf(token))
        .map(
            i ->
                i != -1
                    && (i == 0 || s.charAt(i - 1) == ' ')
                    && (i == s.length() - token.length() || s.charAt(i + token.length()) == ' '))
        .orElse(false);
  }

  /**
   * More than one state transition can occur because when the candidate conditions are fullfilled,
   * they constitute an event. The universal selector transitions are also tried.
   */
  private static void step(final Compiled.DFAState state, final Element element) {
    stepOneEvent(
        state,
        element,
        ("".equals(element.namespaceURI) ? "*" : element.namespaceURI) + "|" + element.localName);

    if (!"".equals(element.namespaceURI)) {
      stepOneEvent(state, element, "*|" + element.localName);
      stepOneEvent(state, element, element.namespaceURI + "|*");
    } else {
      stepOneEvent(state, element, SAC_NO_URI + "|" + element.localName);
    }

    stepOneEvent(state, element, ANY_ELEMENT);
  }

  private static void stepOneEvent(
      final Compiled.DFAState state, final Element element, final String name) {
    ofNullable(state.events.get(name))
        .ifPresent(
            nextState -> {
              traceTransition(state, nextState, name);
              element.states.add(nextState);
              stepThroughConditions(nextState, element);
            });
  }

  private static void stepStates(
      final Collection<Compiled.DFAState> states, final Element element) {
    states.forEach(state -> step(state, element));
  }

  private static void stepThroughConditions(final Compiled.DFAState state, final Element element) {
    state.candidateConditions.entrySet().stream()
        .filter(e -> e.getValue() != null && checkCondition(element, e.getKey()))
        .forEach(
            e -> {
              traceTransition(state, e.getValue(), e.getKey());
              element.states.add(e.getValue());
            });
  }

  private static void traceElement(final String qName, final Attributes atts) {
    LOGGER.finest(
        () ->
            qName
                + ": "
                + attributes(atts).map(a -> a.qName + "=" + a.value).collect(joining(" "))
                + "\n");
  }

  private static void traceTransition(
      final Compiled.DFAState from, final Compiled.DFAState to, final Object event) {
    LOGGER.finest(
        () ->
            from.state
                + " -> "
                + to.state
                + ": "
                + (event instanceof Condition c ? Util.conditionText(c) : event.toString()));
  }

  public void characters(final char[] ch, final int start, final int length) {
    // Nothing to do.
  }

  public void endDocument() {
    // Nothing to do.
  }

  public void endElement(final String namespaceURI, final String localName, final String qName) {
    final Element element = elements.pop();

    ofNullable(elements.peek()).ifPresent(e -> e.children.add(element));
    element.children = null;
  }

  public void endPrefixMapping(final String prefix) {
    // Nothing to do.
  }

  String getCurrentLanguage() {
    return !elements.isEmpty() ? elements.peek().language : null;
  }

  private String getLanguage(
      final String namespaceURI, final Attributes attributes, final Element parent) {
    return tryWith(() -> Objects.equals(XHTML, namespaceURI) ? attributes.getValue(LANG) : null)
        .or(() -> attributes.getValue("xml:" + LANG))
        .get()
        .filter(net.pincette.csstoxslfo.util.Util::isLanguageTag)
        .orElse(parent.language);
  }

  public void ignorableWhitespace(char[] ch, int start, int length) {
    // Nothing to do.
  }

  /** Returns the rules that match a pseudo element sorted from least to most specific. */
  Collection<Rule> matchingPseudoRules() {
    return selectRules(s -> s.pseudoRules);
  }

  /** Returns the rules that match a normal element sorted from least to most specific. */
  Collection<Rule> matchingRules() {
    return selectRules(s -> s.rules);
  }

  public void processingInstruction(final String target, final String data) {
    // Nothing to do.
  }

  private Collection<Rule> selectRules(final Function<Compiled.DFAState, List<Rule>> select) {
    return ofNullable(elements.peek()).stream()
        .flatMap(e -> e.states.stream())
        .flatMap(state -> select.apply(state).stream())
        .sorted(comparator())
        .collect(toCollection(ArrayList::new));
  }

  public void setDocumentLocator(final Locator locator) {
    // Nothing to do.
  }

  public void skippedEntity(final String name) {
    // Nothing to do.
  }

  public void startDocument() {
    elements.clear();

    final Element root = new Element("", "/");

    root.language = DEFAULT_LANGUAGE;
    elements.push(root);
  }

  public void startElement(
      final String namespaceURI, final String localName, final String qName, final Attributes atts)
      throws SAXException {
    final Element element = new Element(namespaceURI, localName);

    element.parent = elements.peek();

    ofNullable(element.parent)
        .ifPresent(
            p -> {
              element.attributes = atts;
              element.language = getLanguage(namespaceURI, atts, p);
              elements.push(element);

              traceElement(namespaceURI + "|" + localName, atts);
              stepStates(p.states, element);

              if (!p.children.isEmpty()) {
                stepStates(getSiblingStates(p.children.get(p.children.size() - 1).states), element);
              }
            });

    // At every element new rules can be started, because they are relative.

    step(startState, element);
  }

  public void startPrefixMapping(final String prefix, final String uri) {
    // Nothing to do.
  }

  private static class Element {
    private Attributes attributes;
    private List<Element> children = new ArrayList<>();
    private String language;
    private final String localName;
    private final String namespaceURI;
    private Element parent;
    private final List<Compiled.DFAState> states = new ArrayList<>();

    private Element(final String namespaceURI, final String localName) {
      this.namespaceURI = namespaceURI != null ? namespaceURI : "";
      this.localName = localName;
    }
  }
}
