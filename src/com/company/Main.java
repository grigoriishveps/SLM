package com.company;


import java.util.concurrent.atomic.AtomicReference;

interface Lock{
    void lock();
    void unlock();
}

class CLH_Queue implements Lock{
    private class QNode{
        boolean wait = false;
    }
    AtomicReference<QNode> tail = new AtomicReference<>(new QNode());
    ThreadLocal<QNode> myNode= ThreadLocal.withInitial(() -> new QNode());
    ThreadLocal<QNode> myPrev= ThreadLocal.withInitial(() -> null);

    public void lock(){
        QNode qNode = myNode.get();
        qNode.wait = true;
        QNode prev = tail.getAndSet(qNode);
        myPrev.set(prev);
        while (prev.wait){};
    }

    public void unlock(){
        QNode qnode = myNode.get();
        qnode.wait = false;
        myNode.set(myPrev.get());
    }
}

class MCS_Queue implements Lock{
    private class QNode{
        boolean wait = false;
        QNode next = null ;
    }

    AtomicReference<QNode> tail = new AtomicReference<QNode>(null);
    ThreadLocal<QNode> myNode = ThreadLocal.withInitial(() -> new QNode());
    public void lock(){
        QNode qnode = myNode.get();
        QNode prev = tail.getAndSet(qnode);
        if (prev != null){
            qnode.wait = true;
            prev.next = qnode;
            while (qnode.wait){
                //System.out.println("LOCK -" + myNode.toString());

                try{
                    Thread.sleep(10);
                }
                catch(InterruptedException e){
                    System.out.println("Thread has been interrupted");
                }
            }
        }
    }

    public void unlock(){
        QNode qnode = myNode.get();
        if(qnode.next == null){
            if(tail.compareAndSet(qnode,null))
                return;
            while(qnode.next == null) {
                }
        }
        qnode.next.wait = false;
        qnode.next = null;

    }
}
class IncrObj{
    static int count ;
    int real_count;
    static {
        count = 0;
    }
    IncrObj(){
        real_count = 0;
    }
}

class JThread extends Thread{
    Lock syncObject;
    IncrObj count;
    JThread(Lock syncObject, IncrObj count){
        super();
        this.syncObject = syncObject;
        this.count = count;
    }
    public void run(){

        for(int i = 0; i < 5000; i++) {
//            synchronized (syncObject) {
//                IncrObj.count++;
//            }
//            System.out.println("Thread obj" + syncObject.myNode.get() );
//            try{
//                Thread.sleep(30000);
//            }
//            catch(InterruptedException e){
//                System.out.println("Thread has been interrupted");
//            }

//            IncrObj.count++;

            syncObject.lock();
            IncrObj.count++;
            syncObject.unlock();
        }
        System.out.println("Thread ended");
    }
}

public class Main {

    public static void main(String[] args) throws InterruptedException {
//        Lock mutex = new MCS_Queue();
        Lock mutex = new CLH_Queue();
        IncrObj incrObj= new IncrObj();

//        System.out.println("Main поток" +mutex.myNode.toString());
        int count_thread = 12;
        JThread arr[] = new JThread[count_thread];
        for(int i=0; i < count_thread; i++) {
            arr[i] = new JThread(mutex, incrObj);
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
