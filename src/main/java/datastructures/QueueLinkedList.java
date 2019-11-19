package datastructures;

/**
 * For implementing queue using using LinkedList- This QueueLinkedList class internally maintains LinkedList reference.
 * @param <E>
 */

public class QueueLinkedList<E>{

    MyLinkedList< E > linkedList = new MyLinkedList<E>(); // creation of Linked List

    /**
     * Insert element at rear in Queue
     */
    public void add(E value){
        linkedList.insertFirst(value);
    }

    /**
     * Removes elements from front of Queue
     */
    public void remove() throws QueueEmptyException {
        try{
            linkedList.removeLast();
        }catch(LinkedListEmptyException llee){
            throw new QueueEmptyException();
        }
    }

    /**
     * Poll elements from front of Queue
     */
    public E poll(){
        try{
            return (E) linkedList.removeLast().data;
        }catch(LinkedListEmptyException llee){
            throw new QueueEmptyException();
        }
    }

    /**
     * Display queue.
     */
    public void displayStack() {
        System.out.print("Displaying Queue > Front to Rear: ");
        linkedList.displayLinkedList();
    }

    public int size() {
        return linkedList.size();
    }

    public static void main(String[] args) {

        QueueLinkedList<Integer> queueLinkedList=new QueueLinkedList<Integer>();
        System.out.println(queueLinkedList.size());

        queueLinkedList.add(39); //insert node.
        queueLinkedList.add(71); //insert node.
        queueLinkedList.add(11); //insert node.
        queueLinkedList.add(76); //insert node.
        System.out.println(queueLinkedList.size());

        queueLinkedList.displayStack(); // display LinkedList

        System.out.println(queueLinkedList.poll());
        queueLinkedList.remove();  //remove Node

        System.out.println(queueLinkedList.size());
        queueLinkedList.displayStack(); //Again display LinkedList

    }


}
