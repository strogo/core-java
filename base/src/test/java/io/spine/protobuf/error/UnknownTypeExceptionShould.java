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

package io.spine.protobuf.error;

import io.spine.type.UnknownTypeException;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnknownTypeExceptionShould {

    @Test
    public void have_ctor_with_type_name() {
        final String str = newUuid();
        final UnknownTypeException exception = new UnknownTypeException(str);

        assertTrue(exception.getMessage().contains(str));
    }

    @Test
    public void have_ctor_with_type_name_and_cause() {
        final String str = newUuid();
        RuntimeException cause = new RuntimeException("");
        final UnknownTypeException exception = new UnknownTypeException(str, cause);

        assertTrue(exception.getMessage().contains(str));
        assertEquals(cause, exception.getCause());
    }
}
