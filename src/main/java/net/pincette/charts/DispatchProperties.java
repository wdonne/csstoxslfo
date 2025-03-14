package net.pincette.charts;

@FunctionalInterface
interface DispatchProperties<T> {
  void apply(T subject, String name, String value, int ppi);
}
