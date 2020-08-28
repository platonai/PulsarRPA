/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>CommandRunner class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class CommandRunner {

    private static final int BUF = 4096;
    private boolean waitForExit = true;
    private String command;
    private int timeout = 10;
    private InputStream stdin;
    private OutputStream stdout;
    private OutputStream stderr;
    private int xit;

    private Throwable _thrownError;

    private CyclicBarrier barrier;

    /**
     * <p>getExitValue.</p>
     *
     * @return a int.
     */
    public int getExitValue() {
        return xit;
    }

    /**
     * <p>Getter for the field <code>command</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCommand() {
        return command;
    }

    /**
     * <p>Setter for the field <code>command</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setCommand(String s) {
        command = s;
    }

    /**
     * <p>setInputStream.</p>
     *
     * @param is a {@link java.io.InputStream} object.
     */
    public void setInputStream(InputStream is) {
        stdin = is;
    }

    /**
     * <p>setStdOutputStream.</p>
     *
     * @param os a {@link java.io.OutputStream} object.
     */
    public void setStdOutputStream(OutputStream os) {
        stdout = os;
    }

    /**
     * <p>setStdErrorStream.</p>
     *
     * @param os a {@link java.io.OutputStream} object.
     */
    public void setStdErrorStream(OutputStream os) {
        stderr = os;
    }

    /**
     * <p>evaluate.</p>
     *
     * @throws java.io.IOException if any.
     */
    public void evaluate() throws IOException {
        this.exec();
    }

    /**
     * <p>exec.</p>
     *
     * @return process exit value (return code) or -1 if timed out.
     * @throws java.io.IOException
     */
    public int exec() throws IOException {
        Process proc = Runtime.getRuntime().exec(command);
        barrier = new CyclicBarrier(3 + ((stdin != null) ? 1 : 0));

        PullerThread so = new PullerThread("STDOUT", proc.getInputStream(), stdout);
        so.setDaemon(true);
        so.start();

        PullerThread se = new PullerThread("STDERR", proc.getErrorStream(), stderr);
        se.setDaemon(true);
        se.start();

        PusherThread si = null;
        if (stdin != null) {
            si = new PusherThread("STDIN", stdin, proc.getOutputStream());
            si.setDaemon(true);
            si.start();
        }

        boolean timedout = false;
        long end = System.currentTimeMillis() + timeout * 1000;

        //
        try {
            if (timeout == 0) {
                barrier.await();
            } else {
                barrier.await(timeout, TimeUnit.SECONDS);
            }
        } catch (TimeoutException ex) {
            timedout = true;
        } catch (BrokenBarrierException | InterruptedException bbe) {
          /* IGNORE */
        }

        // tell the io threads we are finished
        if (si != null) {
            si.interrupt();
        }
        so.interrupt();
        se.interrupt();

        xit = -1;

        if (!timedout) {
            if (waitForExit) {
                do {
                    try {
                        Thread.sleep(1000);
                        xit = proc.exitValue();
                    } catch (InterruptedException ie) {
                        if (Thread.interrupted()) {
                            break; // stop waiting on an interrupt for this thread
                        } else {
                            continue;
                        }
                    } catch (IllegalThreadStateException iltse) {
                        continue;
                    }
                    break;
                } while (!(timedout = (System.currentTimeMillis() > end)));
            } else {
                try {
                    xit = proc.exitValue();
                } catch (IllegalThreadStateException iltse) {
                    timedout = true;
                }
            }
        }

        if (waitForExit) {
            proc.destroy();
        }
        return xit;
    }

    /**
     * <p>getThrownError.</p>
     *
     * @return a {@link java.lang.Throwable} object.
     */
    public Throwable getThrownError() {
        return _thrownError;
    }

    /**
     * <p>Getter for the field <code>timeout</code>.</p>
     *
     * @return a int.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * <p>Setter for the field <code>timeout</code>.</p>
     *
     * @param timeout a int.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * <p>Getter for the field <code>waitForExit</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getWaitForExit() {
        return waitForExit;
    }

    /**
     * <p>Setter for the field <code>waitForExit</code>.</p>
     *
     * @param waitForExit a boolean.
     */
    public void setWaitForExit(boolean waitForExit) {
        this.waitForExit = waitForExit;
    }

    private class PumperThread extends Thread {

        private OutputStream _os;
        private InputStream _is;

        private boolean _closeInput;

        protected PumperThread(String name, InputStream is, OutputStream os,
                               boolean closeInput) {
            super(name);
            _is = is;
            _os = os;
            _closeInput = closeInput;
        }

        public void run() {
            try {
                byte[] buf = new byte[BUF];
                int read = 0;
                while (!isInterrupted() && (read = _is.read(buf)) != -1) {
                    if (read == 0)
                        continue;
                    _os.write(buf, 0, read);
                    _os.flush();
                }
            } catch (InterruptedIOException iioe) {
                // ignored
            } catch (Throwable t) {
                _thrownError = t;
            } finally {
                try {
                    if (_closeInput) {
                        _is.close();
                    } else {
                        _os.close();
                    }
                } catch (IOException ioe) {
          /* IGNORE */
                }
            }
            try {
                barrier.await();
            } catch (InterruptedException ie) {
        /* IGNORE */
            } catch (BrokenBarrierException bbe) {
        /* IGNORE */
            }
        }
    }

    private class PusherThread extends PumperThread {
        PusherThread(String name, InputStream is, OutputStream os) {
            super(name, is, os, false);
        }
    }

    private class PullerThread extends PumperThread {
        PullerThread(String name, InputStream is, OutputStream os) {
            super(name, is, os, true);
        }
    }
    
    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     * @throws java.lang.Exception if any.
     */
    public static void main(String[] args) throws Exception {
        String commandPath = null;
        String filePath = null;
        int timeout = 10;

        String usage = "Usage: CommandRunner [-timeout timeoutSecs] commandPath filePath";

        if (args.length < 2) {
            System.err.println(usage);
            System.exit(-1);
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-timeout")) {
                timeout = Integer.parseInt(args[++i]);
            } else if (i != args.length - 2) {
                System.err.println(usage);
                System.exit(-1);
            } else {
                commandPath = args[i];
                filePath = args[++i];
            }
        }

        CommandRunner cr = new CommandRunner();

        cr.setCommand(commandPath);
        cr.setInputStream(new java.io.FileInputStream(filePath));
        cr.setStdErrorStream(System.err);
        cr.setStdOutputStream(System.out);

        cr.setTimeout(timeout);

        cr.evaluate();

        System.err.println("output value: " + cr.getExitValue());
    }
}
