package io.tl.nekopanel.server;

interface INekoDaemon {
    int getPid();
    long getUptimeMs();
    void ping();
}
