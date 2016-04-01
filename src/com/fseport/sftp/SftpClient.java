package com.fseport.sftp;

//import org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import org.apache.log4j.Logger;

public class SftpClient {	
	
	/** Logger */
    private static final Logger logger = Logger.getLogger(SftpClient.class);
    
    
	public static final String CHANNEL_SFTP = "sftp";
    public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";
    public static final String PREFERRED_AUTHENTICATION_METHODS = "PreferredAuthentications";
    
 // Keep track of the current working directory for improved logging.
    private String currentDirectory = "";
    
    private ChannelSftp channelSftp;
    private Session session;
    private JSch jsch;
    private final String host;
    private int port = 22;
    private String home;

    public SftpClient(String host)
    {
    	this.host = host;
    	jsch = new JSch();
       
    } 
    
    public void login(String user, String password)
    {
        try
        {
            Properties hash = new Properties();
            hash.put(STRICT_HOST_KEY_CHECKING, "no");
//            if (!StringUtils.isEmpty(preferredAuthenticationMethods))
//            {
//                hash.put(PREFERRED_AUTHENTICATION_METHODS, preferredAuthenticationMethods);
//            }
            session = jsch.getSession(user, host);
            session.setConfig(hash);
            session.setPort(port);
            session.setPassword(password);
            session.setTimeout(60000);
            session.connect();

            Channel channel = session.openChannel(CHANNEL_SFTP);
            channel.connect();

            channelSftp = (ChannelSftp) channel;
            setHome(channelSftp.pwd());
        }
        catch (JSchException e)
        {
//            logAndThrowLoginError(user, e);
        }
        catch (SftpException e)
        {
//            logAndThrowLoginError(user, e);
        }
    }
    
    /**
     * Converts a relative path to an absolute path according to
     * http://tools.ietf.org/html/draft-ietf-secsh-scp-sftp-ssh-uri-04.
     *
     * @param path relative path
     * @return Absolute path
     */
    public String getAbsolutePath(String path)
    {
        if (path.startsWith("/~"))
        {
            return home + path.substring(2, path.length());
        }

        // Already absolute!
        return path;
    }
    
    public void changeWorkingDirectory(String wd) throws IOException
    {
        currentDirectory = wd;

        try
        {
            wd = getAbsolutePath(wd);
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to cwd to: " + wd);
            }
            channelSftp.cd(wd);
        }
        catch (SftpException e)
        {
            String message = "Error '" + e.getMessage() + "' occurred when trying to CDW to '" + wd + "'.";
            logger.error(message);
            throw new IOException(message);
        }
    }
    
    /**
     * Creates a directory
     *
     * @param directoryName The directory name
     * @throws IOException If an error occurs
     */
    public void mkdir(String directoryName) throws IOException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Will try to create directory " + directoryName);
            }
            channelSftp.mkdir(directoryName);
        }
        catch (SftpException e)
        {
            // Don't throw e.getmessage since we only get "2: No such file"..
            throw new IOException("Could not create the directory '" + directoryName + "', caused by: "
                                  + e.getMessage());
            // throw new IOException("Could not create the directory '" +
            // directoryName + "' in '" + currentDirectory + "', caused by: " +
            // e.getMessage());
        }
    }
    
    public void deleteFile(String fileName) throws IOException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Will try to delete " + fileName);
            }
            channelSftp.rm(fileName);
        }
        catch (SftpException e)
        {
            throw new IOException(e);
        }
    }
    
    public void storeFile(String fileNameLocal, String fileNameRemote) throws IOException
    {
        storeFile(fileNameLocal, fileNameRemote, WriteMode.OVERWRITE);
    }

    public void storeFile(String fileNameLocal, String fileNameRemote, WriteMode mode) throws IOException
    {
        try
        {
            channelSftp.put(fileNameLocal, fileNameRemote, mode.intValue());
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
        }
    }
    
    public void rename(String filename, String dest) throws IOException
    {

        String absolutePath = getAbsolutePath(dest);
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Will try to rename " + currentDirectory + "/" + filename + " to "
                             + absolutePath);
            }
            channelSftp.rename(filename, absolutePath);
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
            // throw new IOException("Error occured when renaming " +
            // currentDirectory + "/" + filename + " to " + absolutePath +
            // ". Error Message=" + e.getMessage());
        }
    }
    
    private void logAndThrowLoginError(String user, Exception e) throws IOException
    {
        logger.error("Error during login to " + user + "@" + host, e);
        throw new IOException("Error during login to " + user + "@" + host + ": " + e.getMessage());
    }
    
    public void disconnect()
    {
        if (channelSftp != null)
        {
            channelSftp.disconnect();
        }
        if ((session != null) && session.isConnected())
        {
            session.disconnect();
        }
    }
    
    public String[] listFiles() throws IOException
    {
        return listFiles(".");
    }

    public String[] listFiles(String path) throws IOException
    {
        return listDirectory(path, true, false);
    }

    public String[] listDirectories() throws IOException
    {
        return listDirectory(".", false, true);
    }

    public String[] listDirectories(String path) throws IOException
    {
        return listDirectory(path, false, true);
    }

    private String[] listDirectory(String path, boolean includeFiles, boolean includeDirectories) throws IOException
    {
        try
        {
            Vector<LsEntry> entries = channelSftp.ls(path);
            if (entries != null)
            {
                List<String> ret = new ArrayList<String>();
                for (LsEntry entry : entries)
                {
                    if (includeFiles && !entry.getAttrs().isDir())
                    {
                        ret.add(entry.getFilename());
                    }
                    if (includeDirectories && entry.getAttrs().isDir())
                    {
                        if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                        {
                            ret.add(entry.getFilename());
                        }
                    }
                }
                return ret.toArray(new String[ret.size()]);
            }
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage(), e);
        }
        return null;
    }

    public InputStream retrieveFile(String fileName) throws IOException
    {
        // Notify sftp get file action
        long size = getSize(fileName);

        try
        {
            return channelSftp.get(fileName);
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage() + ".  Filename is " + fileName);
        }
    }
    
    public void retrieveFile(String fileName, String getPath) throws IOException
    {
        // Notify sftp get file action
//        long size = getSize(fileName);

        try
        {
            channelSftp.get(fileName, getPath);
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage() + ".  Filename is " + fileName);
        }
    }
    
    public long getSize(String filename) throws IOException
    {
        try
        {
            return channelSftp.stat(filename).getSize();
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage() + " (" + currentDirectory + "/" + filename + ")");
        }
    }

    /**
     * @param filename File name
     * @return Number of seconds since the file was written to
     * @throws IOException If an error occurs
     */
    public long getLastModifiedTime(String filename) throws IOException
    {
        try
        {
            SftpATTRS attrs = channelSftp.stat("./" + filename);
            return attrs.getMTime() * 1000L;
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
        }
    }
    
    public void setPort(int port)
    {
        this.port = port;
    }
    
    /**
     * Setter for 'home'
     *
     * @param home The path to home
     */
    void setHome(String home)
    {
        this.home = home;
    }
    
    /**
     * @return the ChannelSftp - useful for some tests
     */
    public ChannelSftp getChannelSftp()
    {
        return channelSftp;
    }
    
    public enum WriteMode
    {
        APPEND
                {
                    @Override
                    public int intValue()
                    {
                        return ChannelSftp.APPEND;
                    }
                },
        OVERWRITE
                {
                    @Override
                    public int intValue()
                    {
                        return ChannelSftp.OVERWRITE;
                    }
                };

        public abstract int intValue();
    }
}
