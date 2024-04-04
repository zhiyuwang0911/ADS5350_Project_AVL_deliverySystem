public class AvlNode<T> {
    T element;
    // The height of node
    int height;
    AvlNode<T> left;
    AvlNode<T> right;

    // no argument constructor
    public AvlNode(T element) {
        this(element, null, null);
    }

    // full argument constructor
    public AvlNode(T element, AvlNode<T> left, AvlNode<T> right) {
        this.element = element;
        this.left = left;
        this.right = right;
        this.height = 0;
    }
}

