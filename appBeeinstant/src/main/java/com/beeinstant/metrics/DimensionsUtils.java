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

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Utilities for dealing with Dimensions
 */
class DimensionsUtils {

    private static Pattern VALID_NAME_REGEX = Pattern.compile("^[A-Za-z0-9\\+\\-\\*/:_\\.]+$");

    static boolean isValidName(final String name) {
        return VALID_NAME_REGEX.matcher(name).matches();
    }

    static Map<String, String> parseDimensions(final String dimensions) {
        final Map<String, String> dimensionsMap = new TreeMap<String, String>();
        final String[] keyValuePairs = dimensions.split(",");

        for (final String keyValuePair: keyValuePairs) {
            final String[] arr = keyValuePair.split("=");
            if (arr.length == 2) {
                final String key = arr[0].trim().toLowerCase();
                final String value = arr[1].trim();
                if (isValidName(key) && isValidName(value)) {
                    dimensionsMap.put(key, value);
                } else {
                    MetricsManager.reportError("Invalid dimension key or value pair " + key + "=" + value);
                    return new TreeMap<String, String>();
                }
            } else {
                MetricsManager.reportError("Invalid dimension key=value pair format " + keyValuePair);
                return new TreeMap<String, String>();
            }
        }

        return dimensionsMap;
    }

    public static String putIfAbsent(Map<String, String> map, String key, String value) {
        if (!map.containsKey(key))
            return map.put(key, value);
        else
            return map.get(key);
    }

    static String extendAndSerializeDimensions(final Map<String, String> rootDimensions, final String dimensions) {
        final Map<String, String> newDimensions = parseDimensions(dimensions);
        if (!newDimensions.isEmpty()) {
//            rootDimensions.forEach(newDimensions::putIfAbsent);
            for(String key : rootDimensions.keySet()) {
              putIfAbsent(newDimensions, key, rootDimensions.get(key));
            }
            return serializeDimensionsToString(newDimensions);
        }
        return "";
    }

    static String serializeDimensionsToString(final Map<String, String> dimensionsMap) {
        if(dimensionsMap == null || dimensionsMap.isEmpty()) {
            return "";
        }
//        return dimensionsMap.entrySet().stream().map(entry -> "d." + entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(","));
        StringBuilder builder = new StringBuilder();
        for (String key : dimensionsMap.keySet()) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append("d.").append(key).append("=").append(dimensionsMap.get(key));
        }
        return builder.toString();
    }

}
