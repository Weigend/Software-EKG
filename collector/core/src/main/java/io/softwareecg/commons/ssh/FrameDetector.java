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

package io.softwareecg.commons.ssh;

/**
 * FrameDetector interface represents implementations that
 * resolve the start and the end of an frame inside a continuous byte stream.
 * This is required because the asynchronous reading of stream doesn't get
 * the content page per page.
 *
 * @since 17.11.13
 */
public interface FrameDetector {

    /**
     * Scans the given content from ssh session for the start end ending
     * of a frame. A frame is normally the full output an application renders
     * on the screen (like the whole output from top to one specific time)
     * <p/>
     * It can be that the content doesn't contain the start or the end because
     * it is only the a part of the output stream the Reader Threads reads in
     * the last cycle. In this the method will return -1 as start and/or end value.
     *
     * @param sshContent the last read content of the current (async) SSH session
     * @return the start and end of the the frame as wrapped in a FrameBounce instance
     */
    FrameBounce detectFrameBounces(String sshContent);
}
