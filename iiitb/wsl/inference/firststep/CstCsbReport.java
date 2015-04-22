package org.iiitb.wsl.inference.firststep;

public class CstCsbReport {
	private double csb,cst,gcsv;
	public double getCsb() {
		return csb;
	}
	public void setCsb(double csb) {
		this.csb = csb;
	}
	public double getCst() {
		return cst;
	}
	public void setCst(double cst) {
		this.cst = cst;
	}
	public double getGcsv() {
		return gcsv;
	}
	public void setGcsv(double gcsv) {
		this.gcsv = gcsv;
	}
	public CstCsbReport()
	{
		csb=cst=gcsv=-1;
	}
}
