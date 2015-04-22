package org.iiitb.wsl.inference.firststep;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Inference extends JFrame implements ActionListener
{
	private static final long serialVersionUID = -3120311892067665669L;
	private JButton runAlgo,upload;
	private JFileChooser choose;
	public  JProgressBar jpb;
	private List<String> csvFiles; 
	private JPanel top;
	private JCheckBox lod;
	public JTextArea output;

	public Inference()
	{
		// Initialize Frame
		super("INFERENCE IN LARGE");
		setSize(Toolkit.getDefaultToolkit().getScreenSize());

		choose=new JFileChooser();
		choose.setMultiSelectionEnabled(true);
		choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
		choose.addChoosableFileFilter(new FileNameExtensionFilter("CSV","csv"));
		csvFiles=new ArrayList<String>();
		
		top=new JPanel();		
		top.add(new JLabel("Upload CSV Files: "));
		upload=new JButton("Upload");
		upload.addActionListener(this);
		top.add(upload);
		runAlgo=new JButton("Run");
		runAlgo.setEnabled(false);
		runAlgo.addActionListener(this);
		top.add(runAlgo);
		lod=new JCheckBox("Connect LOD:");
		lod.setSelected(false);
		top.add(lod);
		top.add(new JLabel("  Stop NEO4J Service while running the Application...!!!"));
		add(top,BorderLayout.NORTH);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	public static void main(String[] a)
	{
		new Inference();
	}

	@Override
	public void actionPerformed(ActionEvent ae) 
	{
		if(ae.getSource()==upload)
		{
			int choosen=choose.showOpenDialog(this);
			if(choosen==JFileChooser.APPROVE_OPTION)
			{
				File[]f=choose.getSelectedFiles();
				for(File f1:f)
					csvFiles.add(f1.getAbsolutePath());				
				runAlgo.setEnabled(true);
			}
		}
		else if(ae.getSource()==runAlgo)
		{			
			upload.setEnabled(false);
			runAlgo.setEnabled(false);
			lod.setEnabled(false);
			setEnabled(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			final Inference i=this;
			jpb=new JProgressBar(0,100);
			jpb.setValue(0);
			jpb.setStringPainted(true);
			jpb.setIndeterminate(true);
			top.add(jpb);
			
			output=new JTextArea("Output Logs\n");
			JScrollPane jspoutput=new JScrollPane(output,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			add(jspoutput,BorderLayout.CENTER);
			add(new FinalResults(),BorderLayout.EAST);
			add(new JLabel(" "),BorderLayout.WEST);
			add(new JLabel(" "),BorderLayout.SOUTH);
			setVisible(true);

			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					new MapCSVtoLOD(csvFiles.toArray(new String[0]),i,lod.isSelected());//Runs the Algorithm for all files
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					jpb.setIndeterminate(false);
					jpb.setValue(100);
					csvFiles.clear();
					setEnabled(true);
				}				
			});
		}
	}
}