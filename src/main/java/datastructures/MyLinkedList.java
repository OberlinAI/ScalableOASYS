package datastructures;

/**
 * LinkedList class
 */
class MyLinkedList<E> {
    private Node first; // ref to first link on list

    /**
     * LinkedList constructor
     */
    public MyLinkedList(){
        first = null;
    }

    /**
     * Insert New Node at first position
     */
    public void insertFirst(E data) {
        Node newNode = new Node(data); //Creation of New Node.
        newNode.next = first;   //newLink ---> old first
        first = newNode;  //first ---> newNode
    }


    /**
     * removes last Node from LinkedList
     */
    public Node removeLast(){

        //Case1: when there is no element in LinkedList
        if(first==null){  //means LinkedList in empty, throw exception.
            throw new LinkedListEmptyException("LinkedList doesn't contain any Nodes.");
        }

        //Case2: when there is only one element in LinkedList
        if(first.next==null){   //means LinkedList consists of only one element, remove that.
            Node tempNode = first; // save reference to first Node in tempNode- so that we could return saved reference.
            first=first.next; // remove firstNode (make first point to secondNode)
            return tempNode;  //return removed Node.
        }

        //Case3: when there are atLeast two elements in LinkedList
        Node previous=null;
        Node current=first;

        while(current.next!=null){//Executes until we don't find last Node of LinkedList.
            //If next of some Node is pointing to null, that means it's a last Node.
            previous=current;
            current=current.next;   //move to next node.
        }

        previous.next=null;     //Now, previous is pointing to second last Node of LinkiedList,
        //make it point to null [it byPasses current Node(last Node of LinkedList) which was in between]
        return current;
    }


    /**
     * Display LinkedList
     */
    public void displayLinkedList() {
        Node tempDisplay = first; // start at the beginning of linkedList
        while (tempDisplay != null){ // Executes until we don't find end of list.
            tempDisplay.displayNode(tempDisplay.data);
            tempDisplay = tempDisplay.next; // move to next Node
        }
        System.out.println();

    }

    /**
     * Display LinkedList
     */
    public int size() {
        int count = 0;
        Node tempDisplay = first; // start at the beginning of linkedList
        while (tempDisplay != null){ // Executes until we don't find end of list.
//    	   tempDisplay.displayNode(tempDisplay.data);
            tempDisplay = tempDisplay.next; // move to next Node
            count++;
        }
        return count;
    }

}
