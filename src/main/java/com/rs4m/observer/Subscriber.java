package com.rs4m.observer;


public interface Subscriber<T> {
    /**
     * Updates the subscriber with the given event.
     *
     * @param event the event to update the subscriber with
     */
    void update(T event);
}
