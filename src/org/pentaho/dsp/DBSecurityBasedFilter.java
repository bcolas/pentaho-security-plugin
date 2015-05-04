package org.pentaho.dsp;

public class DBSecurityBasedFilter {
	String sessionVar;
	String paramVar;
	
	public void setSessionVar(String iSessionVar) {
		sessionVar = iSessionVar;
	}
	
	public String getSessionVar() {
		return sessionVar;
	}
	
	public void setParamVar(String iParamVar) {
		paramVar = iParamVar;
	}
	
	public String getParamVar() {
		return paramVar;
	}
}
