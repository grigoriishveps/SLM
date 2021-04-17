package com.queue;

import java.util.concurrent.atomic.AtomicReference;

class EmptyException extends RuntimeException{

}

class Node<T>{
    public T value;
    public AtomicReference<Node> next;
    public Node(T value){
        this.value = value;
        this.next = new AtomicReference<Node>(null);
    }
}

class LockFreeQueue<T> {
    AtomicReference<Node<T>> tail;
    AtomicReference<Node<T>> head;
    LockFreeQueue(){
        Node<T> node = new Node<>(null);
        head = new AtomicReference<>(node);
        tail = new AtomicReference<>(node);
    }
    public void enq(T value){
        Node<T> node = new Node<>(value);
        while (true){
            Node<T> last = tail.get();
            Node<T> next =  last.next.get();
            if (last == tail.get()){
                if(next == null){
                    if(last.next.compareAndSet(next, node)){
                        tail.compareAndSet(last, node);
                        return;
                    }
                } else {
                    tail.compareAndSet(last, next);
                }
            }
        }
    }

    public T deq() throws EmptyException{
        while (true){
            Node<T> first = head.get();
            Node<T> last = tail.get();
            Node<T> next = first.next.get();
            if(first == head.get()){
                if(first == last){
                    if(next == null){
                        throw new EmptyException();
                    }
                    tail.compareAndSet(last, next);
                } else{
                    T value = (T) next.value;
                    if (head.compareAndSet(first, next )) {

                        return value;
                    }
                }
            }
        }
    }
}
class IncrObj{
    static AtomicReference<Integer> count ;
    int real_count;
    static {
        count = new AtomicReference<Integer>(0);
    }
    IncrObj(){
        real_count = 0;
    }
}

class JThread extends Thread{
    LockFreeQueue<Integer> queue;
    IncrObj count;
    JThread(LockFreeQueue syncObject){
        super();
        this.queue = syncObject;
        //this.count = count;
    }
    public void run(){
        int sum = 0;
        for(int i = 0; i < 5000; i++) {
            queue.enq(i);
            sum += queue.deq();
        }
//        for(int i = 0; i < 5000; i++) {
//            sum += queue.deq();
//        }
        IncrObj.count.getAndAccumulate(sum, (x,y)-> (x+y));
        System.out.println("Thread end with sum " + sum);
    }
}

public class LockFreeQueueMain {

    public static void main(String[] args) throws InterruptedException {
//        Lock mutex = new MCS_Queue();

        LockFreeQueue<Integer> incrObj= new LockFreeQueue<>();

//        System.out.println("Main поток" +mutex.myNode.toString());
        int count_thread = 12;
        JThread arr[] = new JThread[count_thread];
        for(int i=0; i < count_thread; i++) {
            arr[i] = new JThread( incrObj);
        }

        for(int i=0; i < count_thread; i++) {
            arr[i].start();
        }
        for(int i=0; i < count_thread; i++) {
            try{
                arr[i].join();
            }
            catch(InterruptedException e){
                System.out.printf("%s has been interrupted", arr[i].getName());
            }
            finally{
                Thread.sleep(100);
            }
        }

        System.out.println("res" + IncrObj.count);

    }
}
