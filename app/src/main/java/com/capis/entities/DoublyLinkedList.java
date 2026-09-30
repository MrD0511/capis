package com.capis.entities;

class DoublyLinkedList<K, V> {
    private Node<K, V> head;
    private Node<K, V> tail;

    public DoublyLinkedList() {
        this.head = new Node<K, V>(null, null);
        this.tail = new Node<K, V>(null, null);

        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    public void addToFront(Node<K, V> node) {
        node.next = this.head.next;
        node.prev = this.head;

        this.head.next.prev = node;
        this.head.next = node;
    }

    public void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    public Node<K, V> removeFromEnd() {
        if(this.tail.prev == this.head) {
            return null; // List is empty
        }

        Node<K, V> nodeToRemove = this.tail.prev;
        remove(nodeToRemove);

        return nodeToRemove;
    }

    public void moveToFront(Node<K, V> node) {
        remove(node);
        addToFront(node);
    }

}
