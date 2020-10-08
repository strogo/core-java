/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.annotation.SPI;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.StorageFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Streams.stream;
import static io.spine.server.delivery.InboxColumn.inbox_shard;
import static io.spine.server.delivery.InboxColumn.received_at;
import static io.spine.server.delivery.InboxColumn.status;
import static io.spine.server.delivery.InboxColumn.version;
import static io.spine.server.delivery.InboxMessageStatus.TO_DELIVER;
import static java.util.stream.Collectors.toList;

/**
 * A contract for storages of {@link Inbox} messages.
 *
 * <p>The records a storage of this type are spreads across shards identified by a
 * {@linkplain ShardIndex shard index}.
 *
 * <p>Typically, the storage instance is specific to the
 * {@linkplain io.spine.server.ServerEnvironment server environment} and is used across
 * {@code BoundedContext}s to store the delivered messages.
 */
@SPI
public class InboxStorage extends MessageStorage<InboxMessageId, InboxMessage> {

    public InboxStorage(StorageFactory factory, boolean multitenant) {
        super(factory.createRecordStorage(spec(), multitenant));
    }

    private static MessageRecordSpec<InboxMessageId, InboxMessage> spec() {
        @SuppressWarnings("ConstantConditions")     // Protobuf getters do not return {@code null}s.
        MessageRecordSpec<InboxMessageId, InboxMessage> spec =
                new MessageRecordSpec<>(InboxMessageId.class,
                                        InboxMessage.class,
                                        InboxMessage::getId,
                                        InboxColumn.definitions());
        return spec;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose this method to this package.
     */
    @Override
    protected synchronized void write(InboxMessage message) {
        super.write(message);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose this method to this package.
     */
    @Override
    protected synchronized void writeBatch(Iterable<InboxMessage> messages) {
        super.writeBatch(messages);
    }

    /**
     * Reads the contents of the storage by the given shard index and returns the first page
     * of the results.
     *
     * <p>The older items go first.
     *
     * @param index
     *         the shard index to return the results for
     * @param pageSize
     *         the maximum number of the elements per page
     * @return the first page of the results
     */
    public Page<InboxMessage> readAll(ShardIndex index, int pageSize) {
        Page<InboxMessage> page = new InboxPage(sinceWhen -> readAll(index, sinceWhen, pageSize));
        return page;
    }

    public ImmutableList<InboxMessage>
    readAll(ShardIndex index, @Nullable Timestamp sinceWhen, int pageSize) {
        RecordQueryBuilder<InboxMessageId, InboxMessage> builder =
                queryBuilder().where(inbox_shard)
                              .is(index);
        if (sinceWhen != null) {
            builder.where(received_at)
                   .isGreaterThan(sinceWhen);
        }
        RecordQuery<InboxMessageId, InboxMessage> query = limitAndOrder(pageSize, builder).build();
        Iterator<InboxMessage> iterator = readAll(query);
        return ImmutableList.copyOf(iterator);
    }

    private static RecordQueryBuilder<InboxMessageId, InboxMessage>
    limitAndOrder(int pageSize, RecordQueryBuilder<InboxMessageId, InboxMessage> builder) {
        return builder.limit(pageSize)
                      .sortAscendingBy(received_at)
                      .sortAscendingBy(version);
    }

    /**
     * Finds the newest message {@linkplain InboxMessageStatus#TO_DELIVER to deliver}
     * in the given shard.
     *
     * @param index
     *         the shard index to look in
     * @return the message found or {@code Optional.empty()} if there are no messages to deliver
     *         in the specified shard
     */
    public Optional<InboxMessage> newestMessageToDeliver(ShardIndex index) {
        RecordQuery<InboxMessageId, InboxMessage> query =
                queryBuilder().where(inbox_shard).is(index)
                              .where(status).is(TO_DELIVER)
                              .sortDescendingBy(received_at)
                              .limit(1)
                              .build();
        Iterator<InboxMessage> iterator = readAll(query);
        Optional<InboxMessage> result = iterator.hasNext() ? Optional.of(iterator.next())
                                                           : Optional.empty();
        return result;
    }

    /**
     * Removes the passed messages from the storage.
     *
     * <p>Does nothing for messages that aren't in the storage already.
     *
     * @param messages
     *         the messages to remove
     */
    synchronized void removeBatch(Iterable<InboxMessage> messages) {
        List<InboxMessageId> toRemove = stream(messages).map(InboxMessage::getId)
                                                        .collect(toList());
        deleteAll(toRemove);
    }
}
