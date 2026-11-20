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

/**
 * A container that hold the start and end-index
 * of frames
 *
 * @since 17.11.13
 */
public class FrameBounce {

    /**
     * the start position where the last complete frame has
     * started. The init value is -1 to signal that isn't
     * set before
     */
    private int lastCompleteFrameStart = -1;

    /**
     * the end position where the last complete frame has
     * started. The init value is -1 to signal that isn't
     * set before
     */
    private int lastFrameEnd = -1;

    /**
     * Default constructor without
     * any initialisation
     */
    public FrameBounce() {
        // NoOp
    }

    /**
     * Parameterized Constructor that initialize the class with the given
     * position of the frame start and the position of the frame end.
     * In both cases the position means the index of start/end in the current
     * command output.
     *
     * @param lastCompleteFrameStart the index of the frame start in the command output
     * @param lastFrameEnd           the index of the frame start in the command output
     */
    public FrameBounce(int lastCompleteFrameStart, int lastFrameEnd) {
        this.lastCompleteFrameStart = lastCompleteFrameStart;
        this.lastFrameEnd = lastFrameEnd;
    }

    /**
     * Returns the index of the position where the last complete
     * frame starts in the standard command output
     *
     * @return the start index of the frame
     */
    public final int getLastCompleteFrameStart() {
        return lastCompleteFrameStart;
    }

    /**
     * Set the start index of the last complete frame. If no frame beginning
     * available the value should be -1
     *
     * @param lastCompleteFrameStart the start index of the frame
     */
    public final void setLastCompleteFrameStart(int lastCompleteFrameStart) {
        this.lastCompleteFrameStart = lastCompleteFrameStart;
    }

    public final int getLastFrameEnd() {
        return lastFrameEnd;
    }

    public final void setLastFrameEnd(int lastFrameEnd) {
        this.lastFrameEnd = lastFrameEnd;
    }
}
