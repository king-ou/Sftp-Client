package com.fseport.sftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class CiqTransformFile {
	
	static{				
		PropertyConfigurator.configure("conf/log4j.properties");	
	}
	static Logger log = Logger.getLogger(CiqTransformFile.class.getName());
	
	public static final String PROPERTY_HOST = "sftp.host";
    public static final String PROPERTY_PORT = "sftp.port";
    public static final String PROPERTY_USERNAME = "sftp.user";
    public static final String PROPERTY_PASSWORD = "sftp.pwd";
    public static final String PROPERTY_UPLOAD_DIRECTORY = "file.sender";
    public static final String PROPERTY_DOWNLOAD_DIRECTORY = "file.receiver";
    public static final String PROPERTY_IN_DIRECTORY = "file.upload";
    public static final String PROPERTY_OUT_DIRECTORY = "file.download";    
    
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
	private static String now="";
	
	public static void main(String[] args) {
		log.info(sdf.format(new Date())  + now + "");
		Properties sftpProps = new Properties();
	    try{
	    	FileInputStream inputFile = new FileInputStream("conf/sftp.properties");
	    	sftpProps.load(inputFile);
	    	inputFile.close();
	    }catch (Exception e){
	    	e.printStackTrace();
	    	log.error("Can't read file,base.properties not exist in CLASSPATH");
	    	return;
	    }
	    
	    /*Send file to CIQ Sftp server directory */
	    String sendDir = sftpProps.getProperty(PROPERTY_UPLOAD_DIRECTORY);
	    String uploadDir = sftpProps.getProperty(PROPERTY_IN_DIRECTORY);
	    File fp = new File(sendDir);
	    File[] fplist = fp.listFiles();
	    SftpClient client = new SftpClient(sftpProps.getProperty(PROPERTY_HOST));
	    client.login(sftpProps.getProperty(PROPERTY_USERNAME), sftpProps.getProperty(PROPERTY_PASSWORD));
	    if(fplist.length>0){	    	
	    	for (File file : fplist)
            {
//	    		log.info(sdf.format(new Date()) + now + " filename:"+file.getName());
	    		try {
	    			client.storeFile(sendDir+"/"+file.getName(), uploadDir);					
	    			client.rename(uploadDir+"/"+file.getName(), uploadDir+"/"+file.getName());
	    		} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error(e.getMessage());
				}
	    		File srcfile =  new File(sendDir+"/"+file.getName());
	    		srcfile.delete();
	    		log.info(file.getName()+ " upload to ciq successfully on ["+sdf.format(new Date())+"].");
            }
	    }else{
	    	log.info("No file to upload!");
	    }
	    
	    /*Download File from CIQ Sftp server directory*/
	    try {
	    	String downloadDir = sftpProps.getProperty(PROPERTY_OUT_DIRECTORY);
//			client.changeWorkingDirectory(downloadDir);
//			String[] fdlist = client.listFiles();
			String[] fdlist = client.listFiles(downloadDir);
			
			if(fdlist.length>0){
				for (String file : fdlist){
					log.info(file);
					client.changeWorkingDirectory(downloadDir);
					client.retrieveFile(file, sftpProps.getProperty(PROPERTY_DOWNLOAD_DIRECTORY));
					client.deleteFile(file);
					log.info(file + " download to local successfully on ["+sdf.format(new Date())+"].");
				}
			}else{
				log.info("No file to download!");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage());
		}
	    client.disconnect();
	}
}
