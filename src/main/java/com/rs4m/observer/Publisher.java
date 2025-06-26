package com.rs4m.observer;

public interface Publisher<T> {
    /**
     * Registers a subscriber to receive updates from this publisher.
     *
     * @param subscriber the subscriber to register
     */
    void register(Subscriber<T> subscriber);

    /**
     * Unregisters a subscriber from receiving updates from this publisher.
     *
     * @param subscriber the subscriber to unregister
     */
    void unregister(Subscriber<T> subscriber);

    /**
     * Notifies all registered subscribers of an event.
     *
     * @param event the event to notify subscribers about
     */
    void notify(T event);
}
