/*

 * Copyright (C) 2013 Weigend AM
 * *
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
*/
package io.softwareecg.collector.ssh;

import java.io.IOException;

/**
 * A specialized type of <code>Session</code> implementation that
 * always execute binded commands without using a Shell.
 * It can use to execute applications and getting the direct output.
 *
 * @since 10.03.13
 */
public interface ExecSession extends Session {

    /**
     * Execute the binded command in a pseudo terminal.
     * This is similar to execute the command directly in the bash.
     *
     * @return return the ExecSession for fluent programming style
     * @throws java.io.IOException thrown if the command cannot executed caused by connection problems
     */
    ExecSession executeCommand() throws IOException;

}
