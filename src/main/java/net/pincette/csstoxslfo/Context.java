package net.pincette.csstoxslfo;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Element;

/**
 * @author Werner Donné
 */
class Context {
  final Map<String, String> metaData = new HashMap<>();
  final Map<String, PageRule> pageRules = new HashMap<>();
  final Map<String, Map<String, Element>> regions = new HashMap<>();
  PageRule[] resolvedPageRules;
}
