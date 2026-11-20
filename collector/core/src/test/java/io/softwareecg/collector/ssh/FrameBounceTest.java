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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Test the behavior of FrameBounce if no values set
 *
 * @since 18.12.13
 */
public class FrameBounceTest {

    @Test
    public void testFrameBounce() {

        FrameBounce frameBounce = new FrameBounce();

        assertThat(frameBounce.getLastCompleteFrameStart(), is(equalTo(-1)));
        assertThat(frameBounce.getLastFrameEnd(), is(equalTo(-1)));

        frameBounce.setLastCompleteFrameStart(33);
        frameBounce.setLastFrameEnd(88);

        assertThat(frameBounce.getLastCompleteFrameStart(), is(equalTo(33)));
        assertThat(frameBounce.getLastFrameEnd(), is(equalTo(88)));
    }
}
