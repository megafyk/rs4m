package com.rs4m.observer;

public interface Subscriber {
    /**
     * Receives an update from the publisher.
     *
     * @param event the event object containing the update
     */
    void update(Object event);

    /**
     * Returns the unique identifier for this subscriber.
     *
     * @return the subscriber ID
     */
    String getId();
}
