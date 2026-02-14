package com.apirunner.ide.runtime

import com.apirunner.ide.server.ServerProcess

class IntellijServerProcess(
    private val session: ApiRunnerProjectService
) : ServerProcess {

    override fun isRunning(): Boolean {
        return session.getLastPort() != null
    }

    override fun runAndWaitForPort(timeoutMs: Long): Int? {
        return session.getLastPort()
    }
}
