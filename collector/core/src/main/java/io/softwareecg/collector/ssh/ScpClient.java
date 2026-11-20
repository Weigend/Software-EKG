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

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClientCreator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * A very basic <code>SCPClient</code> that can be used to copy files from/to
 * the SSH-2 server. On the server side, the "scp" program must be in the PATH.
 * <p>
 * This scp client is thread safe - you can download (and upload) different sets
 * of files concurrently without any troubles.
 *
 * @since 25.12.13
 */
public class ScpClient {

    private final org.apache.sshd.scp.client.ScpClient client;

    /**
     * Constructs and initializes a new ScpClient based on a ClientSession.
     *
     * @param session the Apache Mina SSH ClientSession
     */
    public ScpClient(ClientSession session) {
        ScpClientCreator creator = ScpClientCreator.instance();
        this.client = creator.createScpClient(session);
    }

    /**
     * Create a remote file and copy the contents of the passed byte array into
     * it. Uses mode 0600 for creating the remote file.
     *
     * @param data the data to be copied into the remote file.
     * @param remoteFileName The name of the file which will be created in the remote target directory.
     * @param remoteTargetDirectory Remote target directory. Use an empty string to specify the default directory.
     * @throws IOException thrown if connection get lost or the transfer fails for over reasons
     */
    public final void put(byte[] data, String remoteFileName, String remoteTargetDirectory) throws IOException {
        put(data, remoteFileName, remoteTargetDirectory, "0600");
    }

    /**
     * Create a remote file and copy the contents of the passed byte array into
     * it. The method use the specified mode when creating the file on the
     * remote side.
     *
     * @param data the data to be copied into the remote file.
     * @param remoteFileName  The name of the file which will be created in the remote target directory.
     * @param remoteTargetDirectory Remote target directory. Use an empty string to specify the default directory.
     * @param mode a four digit string (e.g., 0644, see "man chmod", "man open")
     * @throws IOException thrown if connection get lost or the transfer fails for over reasons
     */
    public final void put(byte[] data, String remoteFileName, String remoteTargetDirectory, String mode) throws IOException {
        String remotePath = remoteTargetDirectory.isEmpty() ? remoteFileName : remoteTargetDirectory + "/" + remoteFileName;
        // Create a temporary file, write data to it, then upload
        Path tempFile = Files.createTempFile("scp-upload-", ".tmp");
        try {
            Files.write(tempFile, data);
            Set<PosixFilePermission> permissions = parseMode(mode);
            Files.setPosixFilePermissions(tempFile, permissions);
            client.upload(tempFile, remotePath, org.apache.sshd.scp.client.ScpClient.Option.PreserveAttributes);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Copy a local file to a remote directory, uses mode 0600 when creating the
     * file on the remote side.
     *
     * @param localFile Path and name of local file.
     * @param remoteTargetDirectory Remote target directory. Use an empty string to specify the default directory.
     * @throws IOException thrown if connection get lost or the transfer fails for over reasons
     */
    public final void put(String localFile, String remoteTargetDirectory) throws IOException  {
        put(localFile, remoteTargetDirectory, "0600");
    }

    /**
     * Copy a local file to a remote directory, uses the specified mode when
     * creating the file on the remote side.
     *
     * @param localFile Path and name of local file.
     * @param remoteTargetDirectory Remote target directory. Use an empty string to specify the default directory.
     * @param mode a four digit string (e.g., 0644, see "man chmod", "man open")
     * @throws IOException thrown if connection get lost or the transfer fails for over reasons
     */
    public final void put(String localFile, String remoteTargetDirectory, String mode) throws IOException  {
        Path localPath = Paths.get(localFile);
        Set<PosixFilePermission> permissions = parseMode(mode);
        client.upload(localPath, remoteTargetDirectory, org.apache.sshd.scp.client.ScpClient.Option.PreserveAttributes);
    }

    /**
     * Copy a set of local files to a remote directory, uses mode 0600 when
     * creating files on the remote side.
     *
     * @param localFiles Paths and names of local file names.
     * @param remoteTargetDirectory Remote target directory. Use an empty string to specify the default directory.
     * @throws IOException thrown if connection get lost or the transfer fails for over reasons
     */
    public final void put(String[] localFiles, String remoteTargetDirectory) throws IOException {
        put(localFiles, remoteTargetDirectory, "0600");
    }

    /**
     * Copy a set of local files to a remote directory, uses the specified mode
     * when creating the files on the remote side.
     *
     * @param localFiles Paths and names of the local files.
     * @param remoteTargetDirectory Remote target directory. Use an empty string to specify the default directory.
     * @param mode a four digit string (e.g., 0644, see "man chmod", "man open")
     * @throws IOException thrown if connection get lost or the transfer fails for over reasons
     */
    public final void put(String[] localFiles, String remoteTargetDirectory, String mode) throws IOException {
        for (String localFile : localFiles) {
            put(localFile, remoteTargetDirectory, mode);
        }
    }

    /**
     * Download a file from the remote server to a local directory.
     *
     * @param remoteFile Path and name of the remote file.
     * @param localTargetDirectory Local directory to put the downloaded file.
     * @throws IOException thrown if connection get lost or the transfer fails for over reasons
     */
    public final void get(String remoteFile, String localTargetDirectory) throws IOException {
        Path localPath = Paths.get(localTargetDirectory);
        client.download(remoteFile, localPath, org.apache.sshd.scp.client.ScpClient.Option.PreserveAttributes);
    }

    /**
     * Download a file from the remote server and pipe its contents into an
     * <code>OutputStream</code>. Please note that, to enable flexible usage
     * of this method, the <code>OutputStream</code> will not be closed nor
     * flushed.
     *
     * @param remoteFile Path and name of the remote file.
     * @param target OutputStream where the contents of the file will be sent to.
     * @throws IOException thrown if connection get lost or the transfer fails for over reasons
     */
    public final void get(String remoteFile, OutputStream target) throws IOException {
        // Create a temporary file to download to, then copy to output stream
        Path tempFile = Files.createTempFile("scp-download-", ".tmp");
        try {
            client.download(remoteFile, tempFile);
            Files.copy(tempFile, target);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Parse a Unix-style mode string (e.g., "0644") to a set of PosixFilePermissions.
     */
    private Set<PosixFilePermission> parseMode(String mode) {
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        if (mode == null || mode.length() < 3) {
            return permissions;
        }
        
        // Handle both "0644" and "644" formats
        String octalPart = mode.length() == 4 ? mode.substring(1) : mode;
        
        int owner = Character.getNumericValue(octalPart.charAt(0));
        int group = Character.getNumericValue(octalPart.charAt(1));
        int others = Character.getNumericValue(octalPart.charAt(2));
        
        if ((owner & 4) != 0) permissions.add(PosixFilePermission.OWNER_READ);
        if ((owner & 2) != 0) permissions.add(PosixFilePermission.OWNER_WRITE);
        if ((owner & 1) != 0) permissions.add(PosixFilePermission.OWNER_EXECUTE);
        
        if ((group & 4) != 0) permissions.add(PosixFilePermission.GROUP_READ);
        if ((group & 2) != 0) permissions.add(PosixFilePermission.GROUP_WRITE);
        if ((group & 1) != 0) permissions.add(PosixFilePermission.GROUP_EXECUTE);
        
        if ((others & 4) != 0) permissions.add(PosixFilePermission.OTHERS_READ);
        if ((others & 2) != 0) permissions.add(PosixFilePermission.OTHERS_WRITE);
        if ((others & 1) != 0) permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        
        return permissions;
    }
}
