package org.iiitb.wsl.inference.firststep;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FinalResults extends JPanel implements ActionListener
{
	private static final long serialVersionUID = -4700502570538107984L;
	JButton ccs,dcs,buc,tdc,rep1,rep2;
	GridBagConstraints gc;
	
	public FinalResults()
	{
		int i=1;
		gc=new GridBagConstraints();
		setLayout(new GridBagLayout());
		addComp(i++,1,1,1,new JLabel("   Final Scores:   "));
		ccs=new JButton("Candidate Class- CCS");
		ccs.addActionListener(this);
		addComp(i++,1,1,1,new JLabel("  "));
		addComp(i++,1,1,1,ccs);
		dcs=new JButton("Domain Class- DCS");
		dcs.addActionListener(this);
		addComp(i++,1,1,1,new JLabel("  "));
		addComp(i++,1	,1,1,dcs);
		buc=new JButton("Bottom Up- CSB");
		buc.addActionListener(this);
		addComp(i++,1,1,1,new JLabel("  "));
		addComp(i++,1	,1,1,buc);
		tdc=new JButton("Top Down- CST");
		tdc.addActionListener(this);
		addComp(i++,1,1,1,new JLabel("  "));
		addComp(i++,1	,1,1,tdc);
		rep2=new JButton("Report - CSB & CST");
		rep2.addActionListener(this);
		addComp(i++,1,1,1,new JLabel("  "));
		addComp(i++,1	,1,1,rep2);
		rep1=new JButton("Final Report - CCS & DCS");
		rep1.addActionListener(this);
		addComp(i++,1,1,1,new JLabel("  "));
		addComp(i++,1	,1,1,rep1);
	}

	public void addComp(int r,int c,int w ,int h,Component cc)
	{
		gc.gridx=c;
		gc.gridy=r;
		gc.gridwidth=w;
		gc.gridheight=h;
		gc.fill=GridBagConstraints.BOTH;
		add(cc,gc);
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) 
	{
		if(ae.getSource()==ccs)
		{
			new LoadTableResults("ccs",3);
		}
		else if(ae.getSource()==dcs)
		{
			new LoadTableResults("dcs",5);
		}
		else if(ae.getSource()==buc)
		{
			new LoadTableResults("buc",3);
		}
		else if(ae.getSource()==tdc)
		{
			new LoadTableResults("tdc",3);
		}
		else if(ae.getSource()==rep1)
		{
			new LoadTableResults("rep1", 5);
		}
		else if(ae.getSource()==rep2)
		{
			new LoadTableResults("rep2", 4);			
		}
	}
}
