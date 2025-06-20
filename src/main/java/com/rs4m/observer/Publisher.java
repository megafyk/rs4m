package com.rs4m.observer;

public interface Publisher {
    /**
     * Registers a subscriber to receive updates from this publisher.
     *
     * @param subscriber the subscriber to register
     */
    void register(Subscriber subscriber);

    /**
     * Unregisters a subscriber from receiving updates from this publisher.
     *
     * @param subscriber the subscriber to unregister
     */
    void unregister(Subscriber subscriber);

    /**
     * Notifies all registered subscribers of an event.
     *
     * @param event the event to notify subscribers about
     */
    void notify(Object event);
}
