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

import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @since 25.12.13
 */
public class ScpFileTransferTest extends AbstractSshTest {

    private static final String TEMP_DIR;

    static {
        if (System.getProperty("java.io.tmpdir").startsWith("/")) {
            TEMP_DIR = System.getProperty("java.io.tmpdir");
        } else {
            TEMP_DIR = "/" + System.getProperty("java.io.tmpdir");
        }
    }

    @Test
    public void testScpTransfer() throws Exception {
        connectAndAuthenticate();
        ScpClient client = connection.createScpClient();

        File transferredFile = new File(TEMP_DIR, "test.txt");
        if (transferredFile.exists()) {
            transferredFile.delete();
        }

        assertThat(transferredFile.exists(), is(false));
        client.put("testdata".getBytes(), "test.txt", TEMP_DIR);
        assertThat(transferredFile.exists(), is(true));
    }
}
