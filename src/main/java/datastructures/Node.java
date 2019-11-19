package datastructures;

/**
 *Node class, which holds data and contains next which points to next Node.
 */
class Node {
    public Object data; // data in Node.
    public Node next; // points to next Node in list.

    /**
     * Constructor
     */
    public Node(Object data){
        this.data = data;
    }

    /**
     * Display Node's data
     */
    public void displayNode(Object data) {
        System.out.print(data.toString() + " ");
    }
}
