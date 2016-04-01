package com.fseport.sftp;

public class Sftp {
	
	private String host;
    private int port;
    private String user;
    private String password;
    private String send;
    private String receive;
    private String upload;
    private String download;
    
    
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getSend() {
		return send;
	}
	
	public void setSend(String send) {
		this.send = send;
	}
	
	public String getReceive() {
		return receive;
	}
	
	public void setReceive(String receive) {
		this.receive = receive;
	}
	
	public String getUpload() {
		return upload;
	}
	
	public void setUpload(String upload) {
		this.upload = upload;
	}
	
	public String getDownload() {
		return download;
	}
	
	public void setDownload(String download) {
		this.download = download;
	}

}
