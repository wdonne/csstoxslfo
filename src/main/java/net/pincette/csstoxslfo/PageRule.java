package net.pincette.csstoxslfo;

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents one CSS3 @page rule.
 *
 * @author Werner Donné
 */
class PageRule implements PropertyContainer {
  private final Map<String, MarginBox> marginBoxes;
  private final Map<String, Property> properties;
  private final String name;

  PageRule(final String name) {
    this.name = name;
    marginBoxes = new HashMap<>();
    properties = new HashMap<>();
  }

  PageRule(final PageRule rule) {
    name = rule.name;
    marginBoxes =
        rule.marginBoxes.entrySet().stream()
            .collect(toMap(Entry::getKey, entry -> new MarginBox(entry.getValue())));
    properties =
        rule.properties.entrySet().stream()
            .collect(toMap(Entry::getKey, entry -> new Property(entry.getValue())));
  }

  void addMarginBox(final MarginBox box) {
    marginBoxes.put(box.getName(), box);
  }

  String getName() {
    return name;
  }

  MarginBox getMarginBox(final String name) {
    return marginBoxes.get(name);
  }

  MarginBox[] getMarginBoxes() {
    return marginBoxes.values().toArray(new MarginBox[0]);
  }

  public Property[] getProperties() {
    return properties.values().toArray(new Property[0]);
  }

  public Property getProperty(final String name) {
    return properties.get(name);
  }

  boolean hasMarginBoxes() {
    return !marginBoxes.isEmpty();
  }

  boolean isEmpty() {
    return properties.isEmpty() && marginBoxes.isEmpty();
  }

  public PropertyContainer removeProperty(final String name) {
    properties.remove(name);

    return this;
  }

  public PropertyContainer setProperty(final Property property) {
    properties.put(property.getName(), property);

    return this;
  }

  static class MarginBox implements PropertyContainer {
    private final Map<String, Property> properties;
    private final String name;

    MarginBox(final String name) {
      this.name = name;
      properties = new HashMap<>();
    }

    MarginBox(final MarginBox box) {
      name = box.name;
      properties =
          box.properties.entrySet().stream()
              .collect(toMap(Entry::getKey, entry -> new Property(entry.getValue())));
    }

    String getName() {
      return name;
    }

    public Property getProperty(final String name) {
      return properties.get(name);
    }

    public Property[] getProperties() {
      return properties.values().toArray(new Property[0]);
    }

    public PropertyContainer removeProperty(final String name) {
      properties.remove(name);

      return this;
    }

    public PropertyContainer setProperty(final Property property) {
      properties.put(property.getName(), property);

      return this;
    }
  }
}
