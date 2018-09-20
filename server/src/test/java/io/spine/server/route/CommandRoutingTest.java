/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.route;

import com.google.common.testing.NullPointerTester;
import io.spine.base.CommandMessage;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.test.commands.CmdCreateProject;
import io.spine.test.route.RegisterUser;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"SerializableInnerClassWithNonSerializableOuterClass"
        /* OK as custom routes do not refer to the test suite. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("CommandRouting should")
class CommandRoutingTest {

    /** Default result of the command routing function. */
    private static final long DEFAULT_ANSWER = 42L;

    /** Custom result of the command routing function. */
    private static final long CUSTOM_ANSWER = 100500L;

    /** A custom default route. */
    private final CommandRoute<Long, CommandMessage> customDefault =
            new CommandRoute<Long, CommandMessage>() {
                private static final long serialVersionUID = 0L;

                @Override
                public Long apply(CommandMessage message, CommandContext context) {
                    return DEFAULT_ANSWER;
                }
            };
    /** A custom command path for {@code StringValue} command messages. */
    private final CommandRoute<Long, RegisterUser> customRoute =
            new CommandRoute<Long, RegisterUser>() {
                private static final long serialVersionUID = 0L;

                @Override
                public Long apply(RegisterUser message, CommandContext context) {
                    return CUSTOM_ANSWER;
                }
            };

    /** The object under tests. */
    private CommandRouting<Long> commandRouting;

    @BeforeEach
    void setUp() {
        commandRouting = CommandRouting.newInstance();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        NullPointerTester nullPointerTester = new NullPointerTester()
                .setDefault(CommandContext.class, CommandContext.getDefaultInstance());

        nullPointerTester.testAllPublicInstanceMethods(commandRouting);
        nullPointerTester.testAllPublicStaticMethods(CommandRouting.class);
    }

    @Test
    @DisplayName("have default route")
    void haveDefaultRoute() {
        assertNotNull(commandRouting.getDefault());
        assertTrue(commandRouting.getDefault() instanceof DefaultCommandRoute);
    }

    @Test
    @DisplayName("replace default route")
    void replaceDefaultRoute() {
        assertEquals(commandRouting, commandRouting.replaceDefault(customDefault));
        assertEquals(customDefault, commandRouting.getDefault());
    }

    @Test
    @DisplayName("add custom route")
    void addCustomRoute() {
        assertEquals(commandRouting, commandRouting.route(RegisterUser.class, customRoute));

        assertEquals(customRoute, commandRouting.get(RegisterUser.class)
                                                .get());
    }

    @Test
    @DisplayName("not allow overwriting set route")
    void notOverwriteSetRoute() {
        commandRouting.route(RegisterUser.class, customRoute);
        assertThrows(IllegalStateException.class,
                     () -> commandRouting.route(RegisterUser.class, customRoute));
    }

    @Test
    @DisplayName("remove previously set route")
    void removePreviouslySetRoute() {
        commandRouting.route(RegisterUser.class, customRoute);
        commandRouting.remove(RegisterUser.class);
    }

    @Test
    @DisplayName("throw ISE on removal if route is not set")
    void notRemoveIfRouteNotSet() {
        assertThrows(IllegalStateException.class, () -> commandRouting.remove(RegisterUser.class));
    }

    @Test
    @DisplayName("apply default route")
    void applyDefaultRoute() {
        TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());

        // Replace the default route since we have custom command message.
        commandRouting.replaceDefault(customDefault)
                      // Have custom route too.
                      .route(RegisterUser.class, customRoute);

        CmdCreateProject cmd = CmdCreateProject
                .newBuilder()
                .setId(newUuid())
                .build();
        CommandEnvelope command = CommandEnvelope.of(factory.createCommand(cmd));

        long id = commandRouting.apply(command.getMessage(), command.getCommandContext());

        assertEquals(DEFAULT_ANSWER, id);
    }

    @Test
    @DisplayName("apply custom route")
    void applyCustomRoute() {
        TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());

        // Have custom route.
        commandRouting.route(RegisterUser.class, customRoute);

        CommandEnvelope command = factory.generateEnvelope();

        long id = commandRouting.apply(command.getMessage(), command.getCommandContext());

        assertEquals(CUSTOM_ANSWER, id);
    }
}
