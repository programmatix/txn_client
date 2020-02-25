/*
 * Copyright (c) 2018 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.transactions.util;

import com.couchbase.client.core.cnc.Event;
import com.couchbase.client.core.cnc.EventBus;
import com.couchbase.client.core.cnc.EventSubscription;
import com.couchbase.transactions.log.TransactionCleanupAttempt;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This super simple event bus should be used in testing only to assert certain
 * events got pushed through.
 *
 * Note it's better to do something like this:
 *
 * cluster.environment().eventBus().subscribe(ev -> lostCleanupEvents.publish(ev));
 *
 * than to provide the event bus during cluster creation, as that replaces the default event bus entirely -
 * effectively disabling logging.
 */
public class LostTxnEventSaver implements EventBus {

  /**
   * Holds all published events until cleared.
   */
  private ArrayList<TransactionCleanupAttempt> events = new ArrayList<>();

  @Override
  public synchronized PublishResult publish(final Event event) {
    if (event instanceof TransactionCleanupAttempt) {
      events.add((TransactionCleanupAttempt) event);
    }

    return PublishResult.SUCCESS;
  }

  @Override
  public synchronized EventSubscription subscribe(final Consumer<Event> consumer) {
    return null;
  }

  @Override
  public synchronized void unsubscribe(final EventSubscription subscription) {

  }

  public synchronized List<TransactionCleanupAttempt> events() {
    return events;
  }

  public void clear() {
    events.clear();
  }

  @Override
  public Mono<Void> start() {
    return Mono.empty();
  }

  @Override
  public Mono<Void> stop(Duration timeout) {
    return Mono.empty();
  }
}
