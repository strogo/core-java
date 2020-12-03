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

package io.spine.server.command.model.given.reaction;

import io.spine.model.contexts.projects.command.SigAddTaskToProject;
import io.spine.model.contexts.projects.command.SigCreateProject;
import io.spine.model.contexts.projects.command.SigStartTask;
import io.spine.model.contexts.projects.event.SigTaskStarted;
import io.spine.model.contexts.projects.rejection.SigCannotCreateProject;

/**
 * A test environment for {@link io.spine.server.command.model.CommandReactionSignatureTest
 * CommandReactionSignatureTest}.
 */
final class EventMessages {

    /** Prevents instantiation of this test environment utility. */
    private EventMessages() {
    }

    static SigCannotCreateProject cannotCreateProject() {
        return SigCannotCreateProject.newBuilder()
                                     .build();
    }

    static SigTaskStarted taskStarted() {
        return SigTaskStarted.getDefaultInstance();
    }

    static SigCreateProject createProject() {
        return SigCreateProject.getDefaultInstance();
    }

    static SigAddTaskToProject addTaskToProject() {
        return SigAddTaskToProject.getDefaultInstance();
    }

    static SigStartTask startTask() {
        return SigStartTask.getDefaultInstance();
    }
}