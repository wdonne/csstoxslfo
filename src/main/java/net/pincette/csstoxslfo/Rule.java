package net.pincette.csstoxslfo;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static net.pincette.css.sac.Condition.SAC_AND_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_CLASS_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ID_CONDITION;
import static net.pincette.css.sac.Condition.SAC_LANG_CONDITION;
import static net.pincette.css.sac.Condition.SAC_ONE_OF_ATTRIBUTE_CONDITION;
import static net.pincette.css.sac.Condition.SAC_OR_CONDITION;
import static net.pincette.css.sac.Condition.SAC_POSITIONAL_CONDITION;
import static net.pincette.css.sac.Condition.SAC_PSEUDO_CLASS_CONDITION;
import static net.pincette.css.sac.Selector.SAC_ELEMENT_NODE_SELECTOR;
import static net.pincette.css.sac.Selector.SAC_PSEUDO_ELEMENT_SELECTOR;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.pincette.css.sac.AttributeCondition;
import net.pincette.css.sac.CombinatorCondition;
import net.pincette.css.sac.Condition;
import net.pincette.css.sac.ConditionalSelector;
import net.pincette.css.sac.DescendantSelector;
import net.pincette.css.sac.ElementSelector;
import net.pincette.css.sac.NegativeSelector;
import net.pincette.css.sac.PositionalCondition;
import net.pincette.css.sac.Selector;
import net.pincette.css.sac.SiblingSelector;
import net.pincette.util.Cases;
import net.pincette.util.Util.GeneralException;

/**
 * Represents one CSS2 rule.
 *
 * @author Werner Donné
 */
class Rule {
  private static final String AFTER = "after";
  private static final String BEFORE = "before";
  private static final String FIRST_LINE = "first-line";

  private Property[] cachedArray = null;
  private final String elementName;
  private final int position;
  private final Map<String, Property> properties = new HashMap<>();
  private String pseudoElementName;
  private final Selector selector;
  private final Selector[] selectorChain;
  private final int specificity;

  /**
   * Use values like -1, 0 and +1 for <code>offset</code>. This will shift the specificity up or
   * down, which is needed to account for the style sheet source.
   */
  Rule(final Selector selector, final int position, final int offset) {
    this.selector = selector;
    this.position = position;
    selectorChain = Util.getSelectorChain(selector);
    specificity = specificity() + offset * 10000000;
    elementName = getElementName(selectorChain, selectorChain.length - 1);
    pseudoElementName = getPseudoElementName(selectorChain);
  }

  /**
   * The created rule physically shares the selector and specificity information. This makes it
   * possible to match a set of rules resulting after a split by picking only one of them.
   */
  private Rule(final Rule source, final int position) {
    this.selector = source.selector;
    this.position = position;
    this.elementName = source.elementName;
    this.pseudoElementName = source.pseudoElementName;
    this.selectorChain = source.selectorChain;
    this.specificity = source.specificity;
  }

  Rule addProperty(final Property property) {
    cachedArray = null;
    properties.put(property.getName(), property);

    return this;
  }

  /**
   * Returns the interned name of the element this rule applies to. If it doesn't apply to an
   * element <code>null</code> is returned.
   */
  String getElementName() {
    return elementName;
  }

  private static String getElementName(final Selector[] selectorChain, final int position) {
    return switch (selectorChain[position].getSelectorType()) {
      case SAC_ELEMENT_NODE_SELECTOR -> ((ElementSelector) selectorChain[position]).getLocalName();
      case SAC_PSEUDO_ELEMENT_SELECTOR -> getElementName(selectorChain, position - 1);
      default -> null;
    };
  }

  int getPosition() {
    return position;
  }

  Property[] getProperties() {
    if (cachedArray == null || cachedArray.length != properties.size()) {
      cachedArray = properties.values().toArray(new Property[0]);
    }

    return cachedArray;
  }

  public Property getProperty() {
    return Optional.of(getProperties())
        .filter(p -> p.length == 1)
        .map(p -> p[0])
        .orElseThrow(() -> new GeneralException("Unsplit rule"));
  }

  private static Stream<String> getPseudoClassConditions(final Condition c) {
    return switch (c.getConditionType()) {
      case SAC_PSEUDO_CLASS_CONDITION -> Stream.of(((AttributeCondition) c).getValue());
      case SAC_AND_CONDITION ->
          concat(
              getPseudoClassConditions(((CombinatorCondition) c).getFirstCondition()),
              getPseudoClassConditions(((CombinatorCondition) c).getSecondCondition()));
      default -> empty();
    };
  }

  /**
   * Returns the interned pseudo-element name or <code>null</code> if the rule doesn't apply to a
   * pseudo-element.
   */
  String getPseudoElementName() {
    return pseudoElementName;
  }

  private static String getPseudoElementName(final Selector[] selectorChain) {
    return Cases.<Selector[], String>withValue(selectorChain)
        .or(
            s -> s[s.length - 1].getSelectorType() == SAC_PSEUDO_ELEMENT_SELECTOR,
            s -> ((ElementSelector) s[s.length - 1]).getLocalName())
        .or(
            s ->
                s.length > 1
                    && s[s.length - 2].getSelectorType() == Selector.SAC_CONDITIONAL_SELECTOR,
            s ->
                getPseudoElementNameConditional(
                    ((ConditionalSelector) s[s.length - 2]).getCondition()))
        .get()
        .orElse(null);
  }

  private static String getPseudoElementNameConditional(final Condition c) {
    return Cases.<Set<String>, String>withValue(getPseudoClassConditions(c).collect(toSet()))
        .or(conditions -> conditions.contains(BEFORE), conditions -> BEFORE)
        .or(conditions -> conditions.contains(AFTER), conditions -> AFTER)
        .or(conditions -> conditions.contains(FIRST_LINE), conditions -> FIRST_LINE)
        .get()
        .orElse(null);
  }

  /** Returns the selector that matches the rule. */
  Selector getSelector() {
    return selector;
  }

  /** Flattens the selector expression tree in infix order. */
  Selector[] getSelectorChain() {
    return selectorChain;
  }

  int getSpecificity() {
    return specificity;
  }

  void setPseudoElementName(final String name) {
    pseudoElementName = name;
  }

  private int specificity() {
    final Specificity s = new Specificity();

    specificity(selector, s);

    return 10000 * s.ids + 100 * s.attributes + s.names;
  }

  private static void specificity(final Selector selector, final Specificity s) {
    if (selector instanceof ConditionalSelector c) {
      specificity(c.getCondition(), s);
      specificity(c.getSimpleSelector(), s);
    } else if (selector instanceof DescendantSelector d) {
      specificity(d.getAncestorSelector(), s);
      specificity(d.getSimpleSelector(), s);
    } else if (selector instanceof NegativeSelector n) {
      specificity(n.getSimpleSelector(), s);
    } else if (selector instanceof SiblingSelector sb) {
      specificity(sb.getSelector(), s);
      specificity(sb.getSiblingSelector(), s);
    } else if (selector.getSelectorType() == SAC_ELEMENT_NODE_SELECTOR
        && ((ElementSelector) selector).getLocalName() != null
    // There is no name for "*".
    ) {
      ++s.names;
    }
  }

  private static void specificity(final Condition c, final Specificity s) {
    switch (c.getConditionType()) {
      case SAC_ID_CONDITION:
        ++s.ids;
        break;
      case SAC_ATTRIBUTE_CONDITION,
      SAC_BEGIN_HYPHEN_ATTRIBUTE_CONDITION,
      SAC_CLASS_CONDITION,
      SAC_LANG_CONDITION,
      SAC_ONE_OF_ATTRIBUTE_CONDITION,
      SAC_PSEUDO_CLASS_CONDITION:
        ++s.attributes;
        break;
      case SAC_AND_CONDITION, SAC_OR_CONDITION:
        specificity(((CombinatorCondition) c).getFirstCondition(), s);
        specificity(((CombinatorCondition) c).getSecondCondition(), s);
        break;
      default:
        break;
    }

    if (c.getConditionType() == SAC_POSITIONAL_CONDITION
        && ((PositionalCondition) c).getPosition() == 1
    // first-child pseudo class.
    ) {
      ++s.attributes;
    }
  }

  /**
   * Splits this rule into a set of equivalent rules in which there is only one property. For each
   * property of this rule, there will be a new one.
   */
  Rule[] split() {
    final int pos = getPosition();

    return stream(getProperties())
        .map(p -> new Rule(this, pos).addProperty(p))
        .toArray(Rule[]::new);
  }

  private static class Specificity {
    private int attributes;
    private int ids;
    private int names;
  }
}
