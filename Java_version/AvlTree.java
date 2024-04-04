import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AvlTree<T> {

    // avltree maximum height minimum height difference is usually set to 1
    private static final int ALLOWED_IMBALANCE = 1;
    private AvlNode<T> root;
    // custom comparison
    private Comparator<T> comparator;

    public AvlTree() {
    }

    public AvlTree(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    // Use custom comparison
    private int compare(T e1, T e2) {
        if (comparator != null) {
            return comparator.compare(e1, e2);
        }
        return ((java.lang.Comparable<T>) e1).compareTo(e2);
    }

    public void setComparator(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    // insert a element
    public void insert(T x) {
        root = insert(x, root);
    }

    // remove a element
    public void remove(T x) {
        root = remove(x, root);
    }

    // Get a list using preorder traversal
    public List<T> tolist() {
        List<T> list = new ArrayList<>();
        if (root != null) {
            tolist(list, root);
        }
        return list;
    }

    // get the smallest element
    public T findMin() {
        if (root == null) {
            return null;
        }
        return findMin(root).element;
    }

    // get the biggest element
    public T findMax() {
        if (root == null) {
            return null;
        }
        return findMax(root).element;
    }

    public void makeEmpty() {
        root = null;
    }

    // return
    public boolean isEmpty() {
        return root == null;
    }

    // Insert private method
    private AvlNode<T> insert(T x, AvlNode<T> t) {

        //If the root node is empty, the current x node is the root
        if (null == t) {
            return new AvlNode<>(x);
        }

        int compareResult = compare(x, t.element);

        //Less than the current root node insert x to the left of the root node
        if (compareResult < 0) {
            t.left = insert(x, t.left);
        } else if (compareResult > 0) {
            //Greater than the current root node, insert x to the right of the root node
            t.right = insert(x, t.right);
        }
        return balance(t);
    }

    // balance method
    private AvlNode<T> balance(AvlNode<T> t) {
        if (t == null) {
            return t;
        }

        // if left higher than right
        if (height(t.left) - height(t.right) > ALLOWED_IMBALANCE) {
            if (height(t.left.left) >= height(t.left.right)) {
                t = rotateWithLeftChild(t);
            } else {
                t = doubleWithLeftChild(t);
            }
        } else if (height(t.right) - height(t.left) > ALLOWED_IMBALANCE) {
            if (height(t.right.right) >= height(t.right.left)) {
                t = rotateWithRightChild(t);
            } else {
                t = doubleWithRightChild(t);
            }
        }
        t.height = Math.max(height(t.left), height(t.right)) + 1;
        return t;
    }

    // double rotate
    private AvlNode<T> doubleWithRightChild(AvlNode<T> k3) {
        k3.right = rotateWithLeftChild(k3.right);
        return rotateWithRightChild(k3);
    }

    // rotate
    private AvlNode<T> rotateWithRightChild(AvlNode<T> k2) {
        AvlNode<T> k1 = k2.right;
        k2.right = k1.left;
        k1.left = k2;
        k2.height = Math.max(height(k2.right), height(k2.left)) + 1;
        k1.height = Math.max(height(k1.right), k2.height) + 1;
        return k1;
    }

    private AvlNode<T> doubleWithLeftChild(AvlNode<T> k3) {
        k3.left = rotateWithRightChild(k3.left);
        return rotateWithLeftChild(k3);
    }

    private AvlNode<T> rotateWithLeftChild(AvlNode<T> k2) {
        AvlNode<T> k1 = k2.left;
        k2.left = k1.right;
        k1.right = k2;
        k2.height = Math.max(height(k2.left), height(k2.right)) + 1;
        k1.height = Math.max(height(k1.left), k2.height) + 1;
        return k1;
    }

    // Get the height of node
    private int height(AvlNode<T> t) {
        return t == null ? -1 : t.height;
    }

    // search, remove and rebalance
    private AvlNode<T> remove(T x, AvlNode<T> t) {

        if (null == t) {
            return t;
        }

        int compareResult = compare(x, t.element);

        //Less than the current root node
        if (compareResult < 0) {
            t.left = remove(x, t.left);
        } else if (compareResult > 0) {
            //Greater than the current root node
            t.right = remove(x, t.right);
        } else if (t.left != null && t.right != null) {
            //Find the smallest node on the right
            t.element = findMin(t.right).element;
            // The right side of the current node is equal to the right side
            // of the original node and the replacement node that has been
            // selected is deleted.
            t.right = remove(t.element, t.right);
        } else {
            t = (t.left != null) ? t.left : t.right;
        }
        return balance(t);
    }

    // findMin private method
    private AvlNode<T> findMin(AvlNode<T> root) {
        if (root == null) {
            return null;
        } else if (root.left == null) {
            return root;
        }
        return findMin(root.left);
    }

    // findMax private method
    private AvlNode<T> findMax(AvlNode<T> root) {
        if (root == null) {
            return null;
        } else if (root.right == null) {
            return root;
        } else {
            return findMax(root.right);
        }
    }

    // tolist private method
    private void tolist(List<T> list, AvlNode<T> node) {
        if (node.left != null) {
            tolist(list, node.left);
        }
        list.add(node.element);
        if (node.right != null) {
            tolist(list, node.right);
        }
    }

}
