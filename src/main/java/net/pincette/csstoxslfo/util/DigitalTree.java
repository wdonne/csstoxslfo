package net.pincette.csstoxslfo.util;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Werner Donné
 */
public class DigitalTree<T> {
  private static final int BITS = 8;
  private static final int MASK = (0x1 << BITS) - 1;
  private static final int SIZE = (0x1 << BITS);

  private Set<String> keys;

  @SuppressWarnings("unchecked")
  private Node[] root = (Node[]) Array.newInstance(Node.class, SIZE);

  public DigitalTree() {
    this(false);
  }

  public DigitalTree(boolean saveKeys) {
    keys = saveKeys ? new HashSet<>() : null;
  }

  /** Creates a shallow copy. */
  public DigitalTree(final DigitalTree<T> other) {
    root = cloneNodes(other.root);

    if (other.keys != null) {
      keys = new HashSet<>(other.keys);
    }
  }

  private Node[] cloneNodes(final Node[] nodes) {
    @SuppressWarnings("unchecked")
    final Node[] result = (Node[]) Array.newInstance(Node.class, nodes.length);

    for (int i = 0; i < nodes.length; ++i) {
      if (nodes[i] != null) {
        result[i] = new Node(nodes[i]);
      }
    }

    return result;
  }

  public T get(final String key) {
    Node[] current = root;
    int i;
    final int length = key.length();
    Node node = null;

    for (i = 0; i < length && current != null; ++i) {
      final char c = key.charAt(i);

      for (int j = 0; j < 16 / BITS && current != null; ++j) {
        node = current[((MASK << (j * BITS)) & c) >>> (j * BITS)];
        current = node != null ? node.nodes : null;
      }
    }

    return i == length && node != null ? node.object : null;
  }

  public Set<String> keySet() {
    return keys;
  }

  @SuppressWarnings("unchecked")
  public void put(final String key, final T o) {
    Node[] current = root;
    final int length = key.length();
    Node node = null;

    for (int i = 0; i < length; ++i) {
      final char c = key.charAt(i);

      for (int j = 0; j < 16 / BITS; ++j) {
        final int value = ((MASK << (j * BITS)) & c) >>> (j * BITS);

        if (current[value] == null) {
          current[value] = new Node();
        }

        if (current[value].nodes == null && (i < length - 1 || j < (16 / BITS) - 1)) {
          current[value].nodes = (Node[]) Array.newInstance(Node.class, SIZE);
        }

        node = current[value];
        current = current[value].nodes;
      }
    }

    if (node != null) {
      node.object = o;
    }

    if (keys != null) {
      keys.add(key);
    }
  }

  public void remove(final String key) {
    put(key, null);
  }

  private class Node {
    private Node[] nodes;
    private T object;

    private Node() {}

    private Node(final Node other) {
      object = other.object;
      nodes = cloneNodes(other.nodes);
    }
  }
}
