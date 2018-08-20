package com.beeinstant.metrics;

import android.util.Log;

public class Logger {
    public void error(String s) {
        LOG(s);
    }
    public void error(Throwable e) {
        LOG(e);
    }
    public void error(String s, Throwable e) {
        LOG(e, s);
    }
    public void debug(String s) {
        LOG(s);
    }
    public void debug(Throwable e) {
        LOG(e);
    }
    public void debug(String s, Throwable e) {
        LOG(e, s);
    }
    public void info(String s) {
        LOG(s);
    }
    public void info(Throwable e) {
        LOG(e);
    }
    public void info(String s, Throwable e) {
        LOG(e, s);
    }
    public void LOG(String p1, String p2, Throwable e) {
        p1 = "BeeInstant: " + p1;
        if (e == null) {
            Log.i(p1, p2);
        } else {
            Log.e(p1, p2, e);
        }
    }
    public void LOG(Throwable e, String... s) {
        if (s == null || s.length <= 0) {
            LOG("", "Error: ", e);
        } else {
            if (s.length == 1) {
                LOG("", s[0], e);
            } else if (s.length > 1) {
                LOG(s[0], s[1], e);
            }
        }
    }
    public void LOG(String... s) {
        LOG(null, s);
    }
}
