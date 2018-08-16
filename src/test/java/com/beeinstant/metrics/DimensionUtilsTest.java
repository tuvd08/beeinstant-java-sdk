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
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DimensionUtilsTest {

    @Test
    public void testParseDimensions() {
        final Map<String, String> expectedOutput = new HashMap<String, String>(){{
            put("key1", "Tada");
        }};
        Assert.assertTrue(DimensionsUtils.parseDimensions("").isEmpty());
        Assert.assertTrue(DimensionsUtils.parseDimensions(" somekey ").isEmpty());
        Assert.assertTrue(DimensionsUtils.parseDimensions(" somekey = , somekey2 = ").isEmpty());
        Assert.assertTrue(DimensionsUtils.parseDimensions(" somekey, somekey2, somekey3 ").isEmpty());
        Assert.assertTrue(DimensionsUtils.parseDimensions(" kEY1 = Coool, KEY1   = Tada   ").equals(expectedOutput));
        Assert.assertTrue(DimensionsUtils.parseDimensions(" kEY1 = , KEY1   = Tada, HelloWorld   ").isEmpty());
        Assert.assertTrue(DimensionsUtils.parseDimensions(" kEY1 = , KEY1   = Tada, HelloWorld   ").isEmpty());
        Assert.assertTrue(DimensionsUtils.parseDimensions(" kEY2 = #@, KEY1   = Tada   ").isEmpty());
    }

    @Test
    public void testExtendDimensions() {
        final Map<String, String> rootDimensions = DimensionsUtils.parseDimensions("Service=ImageSharing, Api=Upload");
        Assert.assertEquals("", DimensionsUtils.extendAndSerializeDimensions(rootDimensions, ""));
        Assert.assertEquals("", DimensionsUtils.extendAndSerializeDimensions(rootDimensions, "Nothing"));
        Assert.assertEquals("d.api=Upload,d.location=Hanoi,d.service=ImageSharing", DimensionsUtils.extendAndSerializeDimensions(rootDimensions, "location=Hanoi"));
        Assert.assertEquals("d.api=Download,d.service=ImageSharing", DimensionsUtils.extendAndSerializeDimensions(rootDimensions, "api=Download"));
        Assert.assertEquals("d.api=Download,d.location=Hanoi,d.service=ImageSharing", DimensionsUtils.extendAndSerializeDimensions(rootDimensions, "location=Hanoi,api=Download"));
        Assert.assertEquals("d.api=Upload,d.service=ImageSharing", DimensionsUtils.extendAndSerializeDimensions(new HashMap<String, String>(), "service=ImageSharing, api=Upload"));
    }

    @Test
    public void testCheckName() {
        Assert.assertTrue(DimensionsUtils.isValidName("HelloWorld+-*/:_1.2.3"));
        Assert.assertFalse(DimensionsUtils.isValidName("HelloWorld@-*/:_1.2.3"));
    }
}
