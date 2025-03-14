package net.pincette.csstoxslfo;

/**
 * @author Werner Donné
 */
interface PropertyContainer {
  Property[] getProperties();

  Property getProperty(String name);

  PropertyContainer removeProperty(String name);

  PropertyContainer setProperty(Property property);
}
