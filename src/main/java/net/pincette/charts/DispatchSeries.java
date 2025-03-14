package net.pincette.charts;

@FunctionalInterface
interface DispatchSeries<T> {
  void apply(T subject, int series, String name, String value, int ppi);
}
