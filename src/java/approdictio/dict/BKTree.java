// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; 
// version 2.1 of the License.

// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
package approdictio.dict;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * a dictionary implementation using <em>Burkhard Keller Tree</em>s.
 * Burkhard Keller Trees store objects of type {@code T} for efficient
 * retrieval of approximately equal objects. Approximate equality is defined
 * by an {@link IntMetric} defined on {@code T}.
 * <p>
 * <p>
 * In a typical use case, first  elements are {@link #add(Object) add}ed
 * to the tree. Then {@link #lookup(Object,Integer) lookup(query)}
 * finds values that are at most {@code maxDist} away from your {@code query},
 * where {@code maxDist} is the maximum distance specified in the constructor.
 * </p>
 * 
 * <p>
 * This class provides no way to delete values from the tree.
 * </p>
 * 
 * @param <V> the type of objects to be stored in the tree. To use the
 *        BKTree, a metric {@link IntMetric} must be defined on {@code V}.
 * 
 * @author harald
 * 
 * @see <a href="http://portal.acm.org/citation.cfm?id=362003.362025">paper
 *      by Burkhard and Keller</a>
 * @see <a href="http://en.wikipedia.org/wiki/BK-tree">English Wikipedia on
 *      BK-Trees</a>
 */

public class BKTree<V> implements Dictionary<V, Integer> {
  private BKNode<V> root;

  private final IntMetric<V> metric;

  // +********************************************************************
  /**
   * <p>
   * creates a BKTree to store objects of type T while using the metric
   * provided to organize lookup.
   * </p>
   */
  public BKTree(IntMetric<V> metric) {
    this.metric = metric;
  }

  // +********************************************************************
  IntMetric<V> getMetric() {
    return metric;
  }
  // +********************************************************************
  BKNode<V> getRoot() {
    return root;
  }
  // +********************************************************************
  interface LinkTable<T> {
    BKNode<T> get(int d);
    BKNode<T> set(int d, BKNode<T> node);
    int size();
  }

  // +********************************************************************
  static class ArrayLinkTable<T> implements LinkTable<T> {
    @SuppressWarnings("unchecked")
    private BKNode<T>[] a = new BKNode[0];

    public int size() {
      return a.length;
    }
    public BKNode<T> get(int d) {
      if( d >= a.length ) {
        return null;
      } else {
        return a[d];
      }
    }

    private static final int REASONABLE_SIZE_EXTEND = 3;
    public BKNode<T> set(int d, BKNode<T> node) {
      if( d >= a.length ) {
        @SuppressWarnings("unchecked")
        BKNode<T>[] tmp = new BKNode[d + REASONABLE_SIZE_EXTEND];
        System.arraycopy(a, 0, tmp, 0, a.length);
        a = tmp;
      }
      BKNode<T> old = a[d];
      a[d] = node;
      return old;
    }
  }

  // +********************************************************************
  private void add(BKNode<V> node, V token) {
    int d = metric.d(node.getValue(), token);
    if( d==0 ) {
      return;
    }
    
    BKNode<V> child = node.get(d);
    if( child == null ) {
      node.set(d, new BKNode<V>(token));
    } else {
      add(child, token);
    }
  }
  // +********************************************************************
  /**
   * <p>
   * for debugging only.
   * </p>
   */
  void dump(Appendable out) {
    if( root == null ) {
      return;
    }
    root.dump(out, -1, "");
  }
  // +********************************************************************
  /**
   * <p>
   * adds the value to the tree. If the {@code value} has a distance of zero
   * to an already stored value, according to our metric, the value is not
   * stored again.
   * </p>
   */
  public void add(V value) {
    if( root == null ) {
      root = new BKNode<V>(value);
      return;
    }
    add(root, value);
  }
  // +********************************************************************
  private int lookup(BKNode<V> node, List<ResultElem<V,Integer>> result,
                     V queryValue, int maxDist, boolean distinct)
  {
    int bestDist = Integer.MAX_VALUE;
    V value = node.getValue();
    int d = metric.d(value, queryValue);
    if( d<=maxDist && !(distinct && queryValue.equals(value)) ) {
      result.add(new ResultElem<V,Integer>(value, d));
      if( d<bestDist ) {
        bestDist = d;
      }
    }
    int from = Math.max(d-maxDist, 0);
    int to = d+maxDist;
    for(int i = from; i<=to; i++) {
      BKNode<V> child = node.get(i);
      if( child==null ) {
        continue;
      }
      int childrenBestDist =
          lookup(child, result, queryValue, maxDist, distinct);
      if( childrenBestDist<bestDist ) {
        bestDist = childrenBestDist;
      }
    }
    return bestDist;
  }
  // +********************************************************************
  /**
   * <p>
   * looks up the given value and returns all values stored that are at most
   * {@code maxDist} away from the given value. In particular a value {@code
   * x} is returned, if {@code metric.d(queryValue, x)<=maxDist}.
   * </p>
   */
  public List<ResultElem<V,Integer>> lookup(V queryValue, Integer maxDist) {
    return lookup(queryValue, maxDist, false);
  }
  /*+******************************************************************/
  public List<ResultElem<V, Integer>> lookupDistinct(V queryValue, 
                                                     Integer maxDist) {
    return lookup(queryValue, maxDist, true);
  }
  /*+******************************************************************/
  private List<ResultElem<V,Integer>> lookup(V queryValue, 
                                             int maxDist, boolean distinct) {
    List<ResultElem<V,Integer>> result =
        new ArrayList<ResultElem<V,Integer>>();

    if( root==null ) {
      return result;
    }

    int bestDist = lookup(root, result, queryValue, maxDist, distinct);
    if( result.size()==0 ) {
      return result;
    }

    return filterBest(result, bestDist);
  }
  /*+******************************************************************/
  private List<ResultElem<V, Integer>> 
  filterBest(List<ResultElem<V, Integer>> candidates,
             int bestDist) {

    List<ResultElem<V, Integer>> result = 
      new ArrayList<ResultElem<V, Integer>>(candidates.size());
    for(ResultElem<V,Integer> cand : candidates) {
      if( cand.d==bestDist ) {
        result.add(cand);
      }
    }
    return result;
  }
  // +********************************************************************
}
