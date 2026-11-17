package com.wusper.sensorblocker.shell;

interface IShellService {
    void destroy() = 16777114;
    String exec(String command) = 1;
    void cancel() = 2;
}
