/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage.memory;

import com.google.common.testing.SerializableTester;
import com.google.protobuf.Timestamp;
import org.junit.Ignore;
import org.junit.Test;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.type.TypeUrl;

/**
 * @author Alexander Yevsyukov
 */
public class InMemoryRecordStorageIOShould {

    @Ignore //TODO:2017-05-30:alexander.yevsyukov: Resume when other projection-related tests are fixed.
    @Test
    public void serialize() {
        final InMemoryRecordStorage<Long> recordStorage =
                InMemoryRecordStorage.newInstance(TypeUrl.of(Timestamp.class), true);
        final RecordStorageIO<Long> io = recordStorage.getIO(Long.class);

        SerializableTester.reserialize(io);
    }
}
