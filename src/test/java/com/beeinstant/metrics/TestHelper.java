/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 BeeInstant
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.beeinstant.metrics;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

class TestHelper {

    static void assertRecorderOutput(final List<Double> expectedValues,
                                     final Unit expectedUnit,
                                     final String actualOutput,
                                     final double epsilon) {

        Assert.assertTrue(actualOutput.endsWith(expectedUnit.toString()));

        final String[] values = actualOutput.substring(0, actualOutput.length() - expectedUnit.toString().length()).split("\\+");
        final List<Double> actualValues = new ArrayList<>();

        for (final String value : values) {
            actualValues.add(Double.parseDouble(value));
        }

        Assert.assertEquals(expectedValues.size(), actualValues.size());

        for (int i = 0; i < expectedValues.size(); i++) {
            Assert.assertEquals(expectedValues.get(i), actualValues.get(i), epsilon);
        }
    }
}
