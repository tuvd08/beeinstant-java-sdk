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

/**
 * Units used in BeeInstant
 */
public enum Unit {

    /* time units */
    NANO_SECOND("ns"),
    MICRO_SECOND("us"),
    MILLI_SECOND("ms"),
    SECOND("s"),
    MINUTE("m"),
    HOUR("h"),

    /* byte units */
    BYTE("b"),
    KILO_BYTE("kb"),
    MEGA_BYTE("mb"),
    GIGA_BYTE("gb"),
    TERA_BYTE("tb"),

    /* rate units */
    BIT_PER_SEC("bps"),
    KILO_BIT_PER_SEC("kbps"),
    MEGA_BIT_PER_SEC("mbps"),
    GIGA_BIT_PER_SEC("gbps"),
    TERA_BIT_PER_SEC("tbps"),

    PERCENT("p"),
    NONE("");

    final String unit;

    Unit(final String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return this.unit;
    }
}
