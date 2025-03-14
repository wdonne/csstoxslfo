package net.pincette.csstoxslfo;

import static java.util.Arrays.stream;
import static net.pincette.csstoxslfo.Util.fileToUrl;
import static net.pincette.util.Util.isUri;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import net.pincette.css.sac.CSSException;
import net.pincette.css.sac.CharacterDataSelector;
import net.pincette.css.sac.ConditionalSelector;
import net.pincette.css.sac.DescendantSelector;
import net.pincette.css.sac.DocumentHandler;
import net.pincette.css.sac.ElementSelector;
import net.pincette.css.sac.InputSource;
import net.pincette.css.sac.LexicalUnit;
import net.pincette.css.sac.NegativeSelector;
import net.pincette.css.sac.Parser;
import net.pincette.css.sac.ProcessingInstructionSelector;
import net.pincette.css.sac.SACMediaList;
import net.pincette.css.sac.Selector;
import net.pincette.css.sac.SelectorList;
import net.pincette.css.sac.SiblingSelector;
import net.pincette.css.sac.helpers.ParserFactory;

class TestSAC {
  private static final Tuple[] tuples = {
    new Tuple(LexicalUnit.SAC_OPERATOR_COMMA, "SAC_OPERATOR_COMMA"),
    new Tuple(LexicalUnit.SAC_OPERATOR_PLUS, "SAC_OPERATOR_PLUS"),
    new Tuple(LexicalUnit.SAC_OPERATOR_MINUS, "SAC_OPERATOR_MINUS"),
    new Tuple(LexicalUnit.SAC_OPERATOR_MULTIPLY, "SAC_OPERATOR_MULTIPLY"),
    new Tuple(LexicalUnit.SAC_OPERATOR_SLASH, "SAC_OPERATOR_SLASH"),
    new Tuple(LexicalUnit.SAC_OPERATOR_MOD, "SAC_OPERATOR_MOD"),
    new Tuple(LexicalUnit.SAC_OPERATOR_EXP, "SAC_OPERATOR_EXP"),
    new Tuple(LexicalUnit.SAC_OPERATOR_LT, "SAC_OPERATOR_LT"),
    new Tuple(LexicalUnit.SAC_OPERATOR_GT, "SAC_OPERATOR_GT"),
    new Tuple(LexicalUnit.SAC_OPERATOR_LE, "SAC_OPERATOR_LE"),
    new Tuple(LexicalUnit.SAC_OPERATOR_GE, "SAC_OPERATOR_GE"),
    new Tuple(LexicalUnit.SAC_OPERATOR_TILDE, "SAC_OPERATOR_TILDE"),
    new Tuple(LexicalUnit.SAC_INHERIT, "SAC_INHERIT"),
    new Tuple(LexicalUnit.SAC_INTEGER, "SAC_INTEGER"),
    new Tuple(LexicalUnit.SAC_REAL, "SAC_REAL"),
    new Tuple(LexicalUnit.SAC_EM, "SAC_EM"),
    new Tuple(LexicalUnit.SAC_EX, "SAC_EX"),
    new Tuple(LexicalUnit.SAC_PIXEL, "SAC_PIXEL"),
    new Tuple(LexicalUnit.SAC_INCH, "SAC_INCH"),
    new Tuple(LexicalUnit.SAC_CENTIMETER, "SAC_CENTIMETER"),
    new Tuple(LexicalUnit.SAC_MILLIMETER, "SAC_MILLIMETER"),
    new Tuple(LexicalUnit.SAC_POINT, "SAC_POINT"),
    new Tuple(LexicalUnit.SAC_PICA, "SAC_PICA"),
    new Tuple(LexicalUnit.SAC_PERCENTAGE, "SAC_PERCENTAGE"),
    new Tuple(LexicalUnit.SAC_URI, "SAC_URI"),
    new Tuple(LexicalUnit.SAC_COUNTER_FUNCTION, "SAC_COUNTER_FUNCTION"),
    new Tuple(LexicalUnit.SAC_COUNTERS_FUNCTION, "SAC_COUNTERS_FUNCTION"),
    new Tuple(LexicalUnit.SAC_RGBCOLOR, "SAC_RGBCOLOR"),
    new Tuple(LexicalUnit.SAC_DEGREE, "SAC_DEGREE"),
    new Tuple(LexicalUnit.SAC_GRADIAN, "SAC_GRADIAN"),
    new Tuple(LexicalUnit.SAC_RADIAN, "SAC_RADIAN"),
    new Tuple(LexicalUnit.SAC_MILLISECOND, "SAC_MILLISECOND"),
    new Tuple(LexicalUnit.SAC_SECOND, "SAC_SECOND"),
    new Tuple(LexicalUnit.SAC_HERTZ, "SAC_HERTZ"),
    new Tuple(LexicalUnit.SAC_KILOHERTZ, "SAC_KILOHERTZ"),
    new Tuple(LexicalUnit.SAC_IDENT, "SAC_IDENT"),
    new Tuple(LexicalUnit.SAC_STRING_VALUE, "SAC_STRING_VALUE"),
    new Tuple(LexicalUnit.SAC_ATTR, "SAC_ATTR"),
    new Tuple(LexicalUnit.SAC_RECT_FUNCTION, "SAC_RECT_FUNCTION"),
    new Tuple(LexicalUnit.SAC_UNICODERANGE, "SAC_UNICODERANGE"),
    new Tuple(LexicalUnit.SAC_SUB_EXPRESSION, "SAC_SUB_EXPRESSION"),
    new Tuple(LexicalUnit.SAC_FUNCTION, "SAC_FUNCTION"),
    new Tuple(LexicalUnit.SAC_DIMENSION, "SAC_DIMENSION")
  };

  @SuppressWarnings("java:S106") // Not logging.
  public static void main(final String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: TestSAC url_or_filename");
      return;
    }

    final Parser parser = new ParserFactory().makeParser();
    final PrintWriter out = new PrintWriter(System.out);

    parser.setDocumentHandler(new SACWriter(out, new File(args[0]).toURI().toURL()));
    parser.parseStyleSheet(
        isUri(args[0]) ? new URL(args[0]).toString() : fileToUrl(new File(args[0])).toString());
    out.flush();
  }

  private record SACWriter(PrintWriter out, URL baseUrl) implements DocumentHandler {
    public void comment(final String text) throws CSSException {
      out.println("comment: " + text);
    }

    public void endDocument(final InputSource source) throws CSSException {
      out.println("end document");
    }

    public void endFontFace() throws CSSException {
      out.println("end font face");
    }

    public void endMarginBox(final String name) throws CSSException {
      out.println("end margin box");
    }

    public void endMedia(final SACMediaList media) throws CSSException {
      out.println("end media");
    }

    public void endPage(final String name, final String pseudePage) throws CSSException {
      out.println("end page");
    }

    public void endSelector(final SelectorList selectors) throws CSSException {
      out.println("end selector");
    }

    private static String enumerateUnits(final LexicalUnit value) {
      return lexicalUnitTypeToString(value)
          + (value.getNextLexicalUnit() != null
              ? (", " + enumerateUnits(value.getNextLexicalUnit()))
              : "");
    }

    public void ignorableAtRule(final String atRule) throws CSSException {
      out.println("ignorable at-rule: " + atRule);
    }

    public void importStyle(
        final String uri, final SACMediaList media, final String defaultNamespaceURI)
        throws CSSException {
      out.println("import: " + uri);

      try {
        final Parser parser = new ParserFactory().makeParser();

        parser.setDocumentHandler(new SACWriter(out, new URL(baseUrl, uri)));
        parser.parseStyleSheet(new URL(baseUrl, uri).toString());
      } catch (Exception e) {
        throw new CSSException(e);
      }
    }

    private static String lexicalUnitTypeToString(final LexicalUnit value) {
      return stream(tuples)
          .filter(tuple -> tuple.id == value.getLexicalUnitType())
          .findFirst()
          .map(tuple -> tuple.name)
          .orElse("unknown");
    }

    public void namespaceDeclaration(final String prefix, final String uri) throws CSSException {
      out.println("namespace declaration: " + prefix + ", " + uri);
    }

    public void property(final String name, final LexicalUnit value, final boolean important)
        throws CSSException {
      out.println(
          "property: "
              + name
              + ": "
              + Util.lexicalUnitToString(value, false, null)
              + "("
              + enumerateUnits(value)
              + "), "
              + important);
    }

    private String selectorText(final Selector selector) {
      return switch (selector.getSelectorType()) {
        case Selector.SAC_ANY_NODE_SELECTOR -> "(any)";
        case Selector.SAC_CDATA_SECTION_NODE_SELECTOR ->
            "(cdata: " + ((CharacterDataSelector) selector).getData() + ")";
        case Selector.SAC_CHILD_SELECTOR ->
            "(child: "
                + selectorText(((DescendantSelector) selector).getAncestorSelector())
                + " "
                + selectorText(((DescendantSelector) selector).getSimpleSelector())
                + ")";
        case Selector.SAC_COMMENT_NODE_SELECTOR ->
            "(comment: " + ((CharacterDataSelector) selector).getData() + ")";
        case Selector.SAC_CONDITIONAL_SELECTOR ->
            "(conditional: "
                + Util.conditionText(((ConditionalSelector) selector).getCondition())
                + " "
                + selectorText(((ConditionalSelector) selector).getSimpleSelector())
                + ")";
        case Selector.SAC_DESCENDANT_SELECTOR ->
            "(descendant: "
                + selectorText(((DescendantSelector) selector).getAncestorSelector())
                + " "
                + selectorText(((DescendantSelector) selector).getSimpleSelector())
                + ")";
        case Selector.SAC_DIRECT_ADJACENT_SELECTOR ->
            "(sibling: "
                + selectorText(((SiblingSelector) selector).getSelector())
                + " "
                + selectorText(((SiblingSelector) selector).getSiblingSelector())
                + ")";
        case Selector.SAC_ELEMENT_NODE_SELECTOR ->
            "(element: "
                + (((ElementSelector) selector).getNamespaceURI() != null
                    ? (((ElementSelector) selector).getNamespaceURI() + "#")
                    : "")
                + ((ElementSelector) selector).getLocalName()
                + ")";
        case Selector.SAC_NEGATIVE_SELECTOR ->
            "(negative: " + selectorText(((NegativeSelector) selector).getSimpleSelector()) + ")";
        case Selector.SAC_PROCESSING_INSTRUCTION_NODE_SELECTOR ->
            "(pi: ("
                + ((ProcessingInstructionSelector) selector).getData()
                + ") ("
                + ((ProcessingInstructionSelector) selector).getTarget()
                + "))";
        case Selector.SAC_PSEUDO_ELEMENT_SELECTOR ->
            "(pseudo: "
                + (((ElementSelector) selector).getNamespaceURI() != null
                    ? (((ElementSelector) selector).getNamespaceURI() + "#")
                    : "")
                + ((ElementSelector) selector).getLocalName()
                + ")";
        case Selector.SAC_ROOT_NODE_SELECTOR -> "(root)";
        case Selector.SAC_TEXT_NODE_SELECTOR ->
            "(text: " + ((CharacterDataSelector) selector).getData() + ")";
        default -> "(unknown)";
      };
    }

    public void startDocument(final InputSource source) throws CSSException {
      out.println("start document");
    }

    public void startFontFace() throws CSSException {
      out.println("start font face");
    }

    public void startMarginBox(final String name) throws CSSException {
      out.println("start magin box: " + name);
    }

    public void startMedia(final SACMediaList media) throws CSSException {
      out.print("start media:");

      for (int i = 0; i < media.getLength(); ++i) {
        out.print(" " + media.item(i));
      }

      out.println();
    }

    public void startPage(final String name, final String pseudoPage) throws CSSException {
      out.println("start page: " + name + ", " + pseudoPage);
    }

    public void startSelector(final SelectorList selectors) throws CSSException {
      out.print("start selector:");

      for (int i = 0; i < selectors.getLength(); ++i) {
        out.print(" " + selectorText(selectors.item(i)));
      }

      out.println();
    }
  }

  private record Tuple(int id, String name) {}
}
