/*
 *(c) 2003-2015 MuleSoft, Inc. This software is protected under international copyright
 *law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 *(or other master license agreement) separately entered into in writing between you and
 *MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.fseport.sftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Contains reusable methods not directly related to usage of the jsch sftp library
 * (they can be found in the class SftpClient).
 *
 * @author Magnus Larsson
 */
public class SftpUtil
{
    /** Logger */
    private static final Logger logger = Logger.getLogger(SftpUtil.class);
    
    public static final String PROPERTY_HOST = "sftp.host";
    public static final String PROPERTY_PORT = "sftp.port";
    public static final String PROPERTY_USERNAME = "sftp.user";
    public static final String PROPERTY_PASSWORD = "sftp.pwd";
    public static final String PROPERTY_UPLOAD_DIRECTORY = "file.sender";
    public static final String PROPERTY_DOWNLOAD_DIRECTORY = "file.receiver";
    public static final String PROPERTY_IN_DIRECTORY = "file.upload";
    public static final String PROPERTY_OUT_DIRECTORY = "file.download";
    
    public static final String PROPERTY_FILENAME = "filename";
    public static final String PROPERTY_ORIGINAL_FILENAME = "originalFilename";
    public static final String PROPERTY_FILE_EXTENSION = "fileExtension";

//    private static final String DUPLICATE_HANDLING_DEFAULT = SftpClient.PROPERTY_DUPLICATE_HANDLING_THROW_EXCEPTION;
    private static final boolean KEEP_FILE_ON_ERROR_DEFAULT = true;
    private static final boolean USE_TEMP_FILE_TIMESTAMP_SUFFIX_DEFAULT = false;
    private static final long SIZE_CHECK_WAIT_TIME_DEFAULT = -1;

    private final static Object lock = new Object();
    
    private String host = "";
    private String port = "";
    private String user = "";
    private String password = "";
    private String send = "";
    private String receive = "";
    private String upload = "";
    private String download = "";

    public SftpUtil()
    {
//        this.connector = (SftpConnector) endpoint.getConnector();
        Properties sftpProps = new Properties();
	    try{
	    	FileInputStream inputFile = new FileInputStream("conf/sftp.properties");
	    	sftpProps.load(inputFile);
	    }catch (Exception e){
	    	e.printStackTrace();
	    	logger.error("Can't read file,base.properties not exist in CLASSPATH");
	    	return;
	    }
	    
	    this.host = sftpProps.getProperty(PROPERTY_HOST);
	    this.port = sftpProps.getProperty(PROPERTY_PORT);
	    this.user = sftpProps.getProperty(PROPERTY_USERNAME);
	    this.password = sftpProps.getProperty(PROPERTY_PASSWORD);
	    this.send = sftpProps.getProperty(PROPERTY_UPLOAD_DIRECTORY);
	    this.receive = sftpProps.getProperty(PROPERTY_DOWNLOAD_DIRECTORY);
	    this.upload = sftpProps.getProperty(PROPERTY_IN_DIRECTORY);
	    this.download = sftpProps.getProperty(PROPERTY_OUT_DIRECTORY);	    	  
    }
    
    public void setHost(String host){
    	this.host = host;
    }
    
    public String getHost(){
    	return host;
    }

    
    

    /**
     * Should be moved to a util class that is not based on an endpoint... TODO: why
     * is this method synchronized?
     * 
     * @param input
     * @param destination
     * @throws IOException
     */
    public synchronized void copyStreamToFile(InputStream input, File destination) throws IOException
    {
        try
        {
            File folder = destination.getParentFile();
            if (!folder.exists())
            {
                throw new IOException("Destination folder does not exist: " + folder);
            }

            if (!folder.canWrite())
            {
                throw new IOException("Destination folder is not writeable: " + folder);
            }

            FileOutputStream output = new FileOutputStream(destination);
            try
            {
                IOUtils.copy(input, output);
            }
            finally
            {
                if (output != null) output.close();
            }
        }
        catch (IOException ex)
        {
            setErrorOccurredOnInputStream(input);
            throw ex;
        }
        catch (RuntimeException ex)
        {
            setErrorOccurredOnInputStream(input);
            throw ex;
        }
        finally
        {
            if (input != null) input.close();
        }
    }

    public void setErrorOccurredOnInputStream(InputStream inputStream)
    {

        
        // If an exception occurs and the keepFileOnError property is
        // true, keep the file on the originating endpoint
        // Note: this is only supported when using the sftp transport on
        // both inbound & outbound
        if (inputStream != null)
        {
            if (inputStream instanceof ErrorOccurredDecorator)
            {
                // Ensure that the SftpInputStream or
                // SftpFileArchiveInputStream knows about the error and
                // dont delete the file
                ((ErrorOccurredDecorator) inputStream).setErrorOccurred();

            }
            else
            {
                logger.warn("Class "
                            + inputStream.getClass().getName()
                            + " did not implement the 'ErrorOccurred' decorator, errorOccured=true could not be set.");
            }
        }
    }
}
