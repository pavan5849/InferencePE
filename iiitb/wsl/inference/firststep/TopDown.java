package org.iiitb.wsl.inference.firststep;

import java.util.Map;
import java.util.Set;

public class TopDown 
{
	private Map<String,Integer> cpfreq;
	private Map< String,Map< String,Set<String> > > cpdec;
	
	private String dbpp, d, ec;

	public String getDbpp() 
	{
		return dbpp;
	}

	public void setDbpp(String dbpp) 
	{
		this.dbpp = dbpp;
	}

	public String getD()
	{
		return d;
	}

	public void setD(String d)
	{
		this.d = d;
	}

	public String getEc() 
	{
		return ec;
	}

	public void setEc(String ec)
	{
		this.ec = ec;
	}

	public Map<String,Integer> getCpfreq() {
		return cpfreq;
	}

	public void setCpfreq(Map<String,Integer> cpfreq) {
		this.cpfreq = cpfreq;
	}

	public Map< String,Map< String,Set<String> > > getCpdec() {
		return cpdec;
	}

	public void setCpdec(Map< String,Map< String,Set<String> > > cpdec) {
		this.cpdec = cpdec;
	}
}