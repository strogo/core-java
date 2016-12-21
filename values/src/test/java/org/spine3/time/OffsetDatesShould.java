/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.time;

import org.junit.Test;
import org.spine3.protobuf.Timestamps;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class OffsetDatesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(OffsetDates.class));
    }

    @Test
    public void obtain_current_OffsetDate_using_ZoneOffset() {
        final int expectedSeconds = 3*Timestamps.SECONDS_PER_HOUR;
        final ZoneOffset inKiev = ZoneOffsets.ofHours(3);
        final OffsetDate today = OffsetDates.now(inKiev);
        final Calendar calendar = Calendar.getInstance();

        assertEquals(calendar.get(Calendar.YEAR), today.getDate().getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, today.getDate().getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), today.getDate().getDay());
        assertEquals(expectedSeconds, today.getOffset().getAmountSeconds());
    }

    @Test
    public void obtain_current_OffsetDate_using_LocalDate_and_ZoneOffset() {
        final int expectedSeconds = 5* Timestamps.SECONDS_PER_HOUR + 30*Timestamps.SECONDS_PER_MINUTE;
        final ZoneOffset inDelhi = ZoneOffsets.ofHoursMinutes(5, 30);
        final LocalDate today = LocalDates.now();

        final OffsetDate todayInDelhi = OffsetDates.of(today, inDelhi);
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        assertEquals(calendar.get(Calendar.YEAR), todayInDelhi.getDate().getYear());
        assertEquals(calendar.get(Calendar.MONTH) + 1, todayInDelhi.getDate().getMonthValue());
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), todayInDelhi.getDate().getDay());
        assertEquals(expectedSeconds, todayInDelhi.getOffset().getAmountSeconds());
    }

}
