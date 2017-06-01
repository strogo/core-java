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

package org.spine3.validate;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import io.spine.base.FieldPath;
import io.spine.validate.ConstraintViolation;

import java.util.List;

/**
 * Validates fields of type {@link EnumValueDescriptor}.
 *
 * @author Dmitry Kashcheiev
 */
@Deprecated //Due to renaming of package to 'io.spine'.
class EnumFieldValidator extends FieldValidator<EnumValueDescriptor> {

    /**
     * Creates a new validator instance.
     *
     * @param descriptor    a descriptor of the field to validate
     * @param fieldValues   values to validate
     * @param rootFieldPath a path to the root field (if present)
     */
    EnumFieldValidator(Descriptors.FieldDescriptor descriptor,
                       Object fieldValues,
                       FieldPath rootFieldPath) {
        super(descriptor,
              FieldValidator.<EnumValueDescriptor>toValueList(fieldValues),
              rootFieldPath,
              false);
    }

    @Override
    protected boolean isValueNotSet(EnumValueDescriptor value) {
        final int intValue = value.getNumber();
        final boolean result = intValue == 0;
        return result;
    }

    @Override
    protected List<ConstraintViolation> validate() {
        checkIfRequiredAndNotSet();
        final List<ConstraintViolation> violations = super.validate();
        return violations;
    }
}
