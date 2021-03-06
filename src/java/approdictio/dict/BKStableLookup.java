package approdictio.dict;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

import approdictio.dict.BKTree.LinkTable;

/**
 * is an iterator that returns lookup results in a {@link BKTree} according
 * to an order provided. This order is completely independent of the metric
 * used in the tree.
 * <p>
 * <strong>IMPORTANT:</strong>
 * </p>
 * <ol>
 * <li>The {@link BKTree} must have been filled with elements in the order
 * given, smallest element first. The the first result element will be the
 * smallest matching element.</li>
 * <li>While the iterator is being used, no new elements may be inserted in
 * to the tree, otherwise the result is undefined.</li>
 * </ol>
 * 
 * @param <W> is the type stored in the referenced {@link BKTree}.
 */
public final class BKStableLookup<W> implements Iterator<W> {
  private final int maxDist;
  private final W queryValue;
  private final Comparator<W> order;
  private final IntMetric<W> metric;
  private W prepared = null;
  private long computeMaxMillis= Long.MAX_VALUE;
      
  /**
   * creates an iterator to extract values from the given {@link BKTree} that
   * have at most {@code maxDist} distance to the {@code queryValue}
   * according to the tree's metric.
   * 
   * @param bkTree is the tree to search
   * @param queryValue the value to search for
   * @param maxDist the maximum distance a result may have to
   *        {@code queryValue}
   * @param order according to which matches are returned. This
   *        requires that the tree was filled with values sorted according to
   *        this order, smallest first. Note: the {@code queryValue} is never
   *        compared against any of the tree's values with this comparator.
   *        The comparator is only ever used to compare tree elements with
   *        each other.
   */
  public BKStableLookup(BKTree<W> bkTree,
                        W queryValue, 
                        int maxDist,
                        Comparator<W> order) {
    this.queryValue = queryValue;
    this.order = order;
    this.maxDist = maxDist;
    BKNode<W> root = bkTree.getRoot();
    if( root!=null) {
      toInspect.add(root);
    }
    this.metric = bkTree.getMetric();
  }
  /*+**********************************************************************/
  /**
   * restrict the maximum compute time for all {@link #next} calls to the given
   * timeout. This is wall time, not CPU cycle time. After the timeout,
   * {@link #hasNext} will return false and calls to {@link #next} will throw an
   * exception. If a call to {@code hasNext} returns true, it is guaranteed,
   * however, that one more call to {@code next} succeeds.
   * 
   * <p>
   * Once the time ran out, this method may not be called again to reset the
   * time, since the internal iteration state is then undefined.</p>
   * 
   * @throws IllegalStateException
   *           if called after the time ran out during a {@code next} or
   *           {@code hasNext} call.
   */
  public void setComputeTimeoutMillis(long deltaFromNow){
    if (computeMaxMillis<=0) {
      throw new IllegalStateException("reset of timeout after it triggered "
          + "not possible");
    }
    computeMaxMillis = System.currentTimeMillis()+deltaFromNow;
  }
  /*+**********************************************************************/
  private final Comparator<BKNode<W>> 
  valueComparator = new Comparator<BKNode<W>>() {
    @Override
    public int compare(BKNode<W> arg0, BKNode<W> arg1) {
      return order.compare(arg0.getValue(), arg1.getValue());
    }
  };
  private static final int USEFULE_INITIAL_QUEUESIZE = 200;
  private final PriorityQueue<BKNode<W>> toInspect = 
    new PriorityQueue<BKNode<W>>(USEFULE_INITIAL_QUEUESIZE, valueComparator);

  @Override
  public boolean hasNext() {
    return prepare()!=null;
  }

  @Override
  public W next() {
    W value = prepare();
    prepared = null;
    if (value==null) {
      throw new NoSuchElementException();
    }
    return value;
  }
  
  private W prepare() {
    if (prepared!=null) {
      return prepared;
    }
    
    while (computeTimeLeft() && !toInspect.isEmpty()) {
      BKNode<W> node = toInspect.poll();
      prepared = node.getValue();
      queueChildren(node);
      //System.out.println("checking "+prepared);
      int d = metric.d(prepared, queryValue);
      if (d<=maxDist) {
        return prepared;
      }
    }
    return null;
  }
  
  private boolean computeTimeLeft() {
    if (System.currentTimeMillis()>=computeMaxMillis) {
      computeMaxMillis = 0;
      return false;
    }
    return true;
  }
  
  private void 
  queueChildren(BKNode<W> node) {
    LinkTable<W> children = node.getChildren();
    int l = children.size();
    for(int i=0; i<l; i++) {
      BKNode<W> child = children.get(i);
      if (child!=null) {
        toInspect.add(child);
      }
    }
  }
  /**
   * is not supported.
   * @throws UnsupportedOperationException
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();      
  }
}