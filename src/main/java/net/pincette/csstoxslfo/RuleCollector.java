package net.pincette.csstoxslfo;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.pincette.css.sac.LexicalUnit.SAC_IDENT;
import static net.pincette.css.sac.LexicalUnit.SAC_INTEGER;
import static net.pincette.csstoxslfo.PageSetupFilter.UNNAMED;
import static net.pincette.csstoxslfo.Property.CONTENT;
import static net.pincette.csstoxslfo.Property.COUNTER_RESET;
import static net.pincette.csstoxslfo.Property.INITIAL_PAGE_NUMBER;
import static net.pincette.csstoxslfo.Property.PAGE;
import static net.pincette.csstoxslfo.Util.ALL;
import static net.pincette.csstoxslfo.Util.getSacParser;
import static net.pincette.util.Or.tryWith;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import net.pincette.css.sac.CSSException;
import net.pincette.css.sac.DocumentHandler;
import net.pincette.css.sac.InputSource;
import net.pincette.css.sac.LexicalUnit;
import net.pincette.css.sac.Parser;
import net.pincette.css.sac.SACMediaList;
import net.pincette.css.sac.SelectorList;

/**
 * This class collects rules and page rules from the "all" and "print" or "screen" media, depending
 * on the mode. The other media are ignored. Rules without any properties are also ignored.
 *
 * @author Werner Donné
 */
class RuleCollector implements DocumentHandler {
  private final URL baseUrl;
  private final RuleCollector.RuleEmitter ruleEmitter;
  private final int offset;
  private final Map<String, PageRule> pageRules;
  private final boolean screen;
  private PageRule.MarginBox currentMarginBox = null;
  private PageRule currentPageRule = null;
  private Rule[] currentRules = null;
  private boolean ignore = false;
  private final Map<String, String> prefixMap = new HashMap<>();
  private int position;

  RuleCollector(
      final RuleCollector.RuleEmitter ruleEmitter,
      final Map<String, PageRule> pageRules,
      final URL baseUrl,
      final boolean screen,
      final int startPosition,
      final int offset) {
    this.ruleEmitter = ruleEmitter;
    this.pageRules = pageRules;
    this.baseUrl = baseUrl;
    this.screen = screen;
    this.position = startPosition;
    this.offset = offset;
  }

  private static boolean isPageCounter(final Property property) {
    final LexicalUnit unit = property.getLexicalUnit();

    return COUNTER_RESET.equals(property.getName())
        && unit.getLexicalUnitType() == SAC_IDENT
        && PAGE.equals(unit.getStringValue())
        && (unit.getNextLexicalUnit() == null
            || unit.getNextLexicalUnit().getLexicalUnitType() == SAC_INTEGER);
  }

  public void comment(final String text) {
    // Nothing to do.
  }

  public void endDocument(final InputSource source) {
    // Nothing to do.
  }

  public void endFontFace() {
    // Nothing to do.
  }

  public void endMarginBox(final String name) {
    currentMarginBox = null;
  }

  public void endMedia(final SACMediaList media) {
    ignore = false;
  }

  public void endPage(final String name, final String pseudoPage) {
    if (!currentPageRule.isEmpty()) {
      ofNullable(pageRules.get(currentPageRule.getName()))
          .ifPresentOrElse(
              existing -> {
                stream(currentPageRule.getProperties()).forEach(existing::setProperty);

                stream(currentPageRule.getMarginBoxes())
                    .forEach(
                        marginBox ->
                            ofNullable(existing.getMarginBox(marginBox.getName()))
                                .ifPresentOrElse(
                                    existingBox ->
                                        stream(marginBox.getProperties())
                                            .forEach(existingBox::setProperty),
                                    () -> existing.addMarginBox(marginBox)));
              },
              () -> pageRules.put(currentPageRule.getName(), currentPageRule));
    }

    currentPageRule = null;
  }

  public void endSelector(final SelectorList selectors) {
    if (!ignore) {
      stream(currentRules)
          .filter(rule -> rule.getProperties().length > 0)
          .flatMap(rule -> stream(rule.split()))
          .forEach(ruleEmitter::addRule);
      currentRules = null;
    }
  }

  int getCurrentPosition() {
    return position;
  }

  private boolean hasOneOfMedia(final SACMediaList media, final String[] choices) {
    if (media == null) {
      return false;
    }

    for (int i = 0; i < media.getLength(); ++i) {
      for (String choice : choices) {
        if (media.item(i).equals(choice)) {
          return true;
        }
      }
    }

    return false;
  }

  public void ignorableAtRule(final String atRule) {
    // Nothing to do.
  }

  public void importStyle(
      final String uri, final SACMediaList media, final String defaultNamespaceURI) {
    if (!ignore
        && (media == null
            || hasOneOfMedia(media, new String[] {ALL, screen ? "screen" : "print"}))) {
      try {
        final Parser parser = getSacParser();
        final URL url = (baseUrl != null ? new URL(baseUrl, uri) : new URL(uri));
        final RuleCollector importCollector =
            new RuleCollector(ruleEmitter, pageRules, url, screen, position, offset);

        parser.setDocumentHandler(importCollector);
        parser.parseStyleSheet(url.toString());
        position = importCollector.getCurrentPosition();
      } catch (Exception e) {
        throw new CSSException(e);
      }
    }
  }

  private Property initialPageNumber(final Property property) {
    final LexicalUnit unit = property.getLexicalUnit();

    return unit.getNextLexicalUnit() == null
        ? new Property(INITIAL_PAGE_NUMBER, "1", property.getImportant(), prefixMap)
        : new Property(
            INITIAL_PAGE_NUMBER,
            unit.getNextLexicalUnit(),
            property.getImportant(),
            prefixMap,
            baseUrl);
  }

  public void namespaceDeclaration(final String prefix, final String uri) {
    prefixMap.put(prefix, uri);
  }

  public void property(final String name, final LexicalUnit value, final boolean important) {
    if (!ignore && (!"".equals(value.getStringValue()) || CONTENT.equals(name))) {
      final Property[] properties =
          new Property(name.toLowerCase(), value, important, prefixMap, baseUrl).split();

      if (currentRules != null) {
        stream(currentRules).forEach(rule -> stream(properties).forEach(rule::addProperty));
      } else if (currentMarginBox != null) {
        stream(properties).forEach(currentMarginBox::setProperty);
      } else if (currentPageRule != null) {
        stream(properties)
            .forEach(
                property ->
                    currentPageRule.setProperty(
                        isPageCounter(property) ? initialPageNumber(property) : property));
      }
    }
  }

  public void startDocument(final InputSource source) {
    // Nothing to do.
  }

  public void startFontFace() {
    // Nothing to do.
  }

  public void startMarginBox(final String name) {
    if (currentPageRule != null) {
      currentMarginBox = new PageRule.MarginBox(name);
      currentPageRule.addMarginBox(currentMarginBox);
    }
  }

  public void startMedia(final SACMediaList media) {
    ignore = !hasOneOfMedia(media, new String[] {"all", screen ? "screen" : "print"});
  }

  public void startPage(final String name, final String pseudoPage) {
    if (!ignore) {
      currentPageRule =
          new PageRule(
              tryWith(() -> name != null && pseudoPage != null ? (pseudoPage + "-" + name) : null)
                  .or(() -> ofNullable(name))
                  .or(() -> ofNullable(pseudoPage))
                  .get()
                  .orElse(UNNAMED));
    }
  }

  public void startSelector(final SelectorList selectors) {
    if (ignore || selectors.getLength() == 0) {
      currentRules = null;

      return;
    }

    currentRules = new Rule[selectors.getLength()];

    for (int i = 0; i < currentRules.length; ++i) {
      currentRules[i] = new Rule(selectors.item(i), position++, offset);
    }
  }

  interface RuleEmitter {
    void addRule(Rule rule);
  }
}
