package org.iiitb.wsl.inference.firststep;

import java.awt.Color;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class LoadTableResults extends JFrame 
{
	private static final long serialVersionUID = 558008054379852617L;
	private JTable result;
	private TableModel model;
	private int colcnt;
	private boolean mainrepcalflag=false,rep2callflag=false;
	private Map<String,ReportOne> report1result;
	private Map<String,CstCsbReport> report2result;
	
	public LoadTableResults(String type,int colcnt)
	{
		this.colcnt=colcnt;
		setSize(Toolkit.getDefaultToolkit().getScreenSize());
		if(type.equals("ccs"))
		{
			setTitle("Candidate Class Scores - "+MapCSVtoLOD.totalcccnt+" rows");
			String[] cols={"URI","Column","Frequency"};	
			model = new DefaultTableModel(getCCS(), cols) {
		  		private static final long serialVersionUID = 528035956743672555L;
			public Class<?> getColumnClass(int column) {
		        return getValueAt(0, column).getClass();
		      }
		    };
		}
		else if(type.equals("dcs"))
		{
			setTitle("Domain Class Scores");
			String[] cols={"URI","Column","Column D Degree","Total D Degree","Frequency"};
			model = new DefaultTableModel(getDCSFromNeo4J(), cols) {
		  		private static final long serialVersionUID = 528035956743672555L;
			public Class<?> getColumnClass(int column) {
		        return getValueAt(0, column).getClass();
		      }
		    };			
		}
		else if(type.equals("buc"))
		{
			setTitle("Candidate Score For Bottom Up");
			String[] cols={"Candidate Class URI","# Columns Connected","Frequency"};
			model = new DefaultTableModel(getBUCFromNeo4J(), cols) {
		  		private static final long serialVersionUID = 528035956743672555L;
			public Class<?> getColumnClass(int column) {
		        return getValueAt(0, column).getClass();
		      }
		    };
			result=new JTable(getBUCFromNeo4J(),cols);
		}
		else if(type.equals("tdc"))
		{
			setTitle("Candidate Score For Top Down");
			String[] cols={"Domain Class URI","# Columns Connected","Frequency"};			
			model = new DefaultTableModel(getTDCFromNeo4J(), cols) {
		  		private static final long serialVersionUID = 528035956743672555L;
			public Class<?> getColumnClass(int column) {
		        return getValueAt(0, column).getClass();
		      }
		    };
		}
		else if(type.equals("rep1"))
		{
			setTitle("Final Report  - For Dominant classes");
			String[] cols={"Class","Table.Column","CCS","DCS","OS"};
			model = new DefaultTableModel(getReport1(), cols) {
		  		private static final long serialVersionUID = 528035956743672555L;
			public Class<?> getColumnClass(int column) {
		        return getValueAt(0, column).getClass();
		      }
		    };			
		}		
		else if(type.equals("rep2"))
		{
			rep2callflag=true;
			setTitle("Report - CSB & CST");
			String[] cols={"Class","CSB","CST","GCsv"};
			model = new DefaultTableModel(getReport2(), cols) {
		  		private static final long serialVersionUID = 528035956743672555L;
			public Class<?> getColumnClass(int column) {
		        return getValueAt(0, column).getClass();
		      }
		    };			
		}

		result = new JTable(model);
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
	    result.setRowSorter(sorter);

		result.setColumnSelectionAllowed(true);
		//result.setCellSelectionEnabled(true);
		result.setRowSelectionAllowed(true);
		result.setGridColor(Color.red);
		result.setRowHeight(25);
		result.setSelectionBackground(Color.black);
		result.setSelectionForeground(Color.yellow);
		JScrollPane jsp=new JScrollPane(result);
		jsp.setWheelScrollingEnabled(true);
		add(jsp);	
		setVisible(true);
	}
	
	public String[][] getCCS()
	{
		String[][] data=new String[MapCSVtoLOD.totalcccnt][colcnt];
		int row=0;
		for(int tabno=0;tabno<MapCSVtoLOD.noofcsv;tabno++)
		{
			for(int colno=0;colno<MapCSVtoLOD.colnames[tabno].size();colno++)
			{
				for(Entry<String,Integer> entry:MapCSVtoLOD.dataUris.get(tabno)[colno].entrySet())
				{						
					data[row][0]=entry.getKey();
					data[row][1]=MapCSVtoLOD.csvFiles[tabno]+"."+MapCSVtoLOD.colnames[tabno].get(colno);
					float f=(float) (entry.getValue()/((MapCSVtoLOD.totalfreq[tabno].get(colno) * 1.0)));
					data[row][2]=Float.toString(f);
					if(mainrepcalflag)
					{
						ReportOne temp=new ReportOne();
						temp.setCcs(f);
						report1result.put(data[row][1]+"/"+data[row][0], temp);
					}
					row++;
				}
			}
		}
		return data;
	}
	
	@SuppressWarnings("deprecation")
	public String[][] getDCSFromNeo4J()
	{
		String[][] data=new String[MapCSVtoLOD.totaldcnt][colcnt];
		int row=0;
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( Neo4JConnectUtil.DB_PATH);
		Transaction tx = graphDb.beginTx();
		try
		{
			String totdegree="start n=node(*) WITH n, length(n<-[:D]-()) as TOTAL_DEGREE where n.D_Node='true' RETURN n.name AS CLASS_NAME, TOTAL_DEGREE ORDER BY TOTAL_DEGREE DESC";
	    	Result totresult = graphDb.execute(totdegree);
	    	Map<String,String> totmap=new HashMap<String, String>();
	    	while(totresult.hasNext())
	    	{
	    		 Map<String, Object> row1 = totresult.next();
	    		 totmap.put(row1.get(totresult.columns().get(0)).toString(),row1.get(totresult.columns().get(1)).toString());
	    	}
			String coldegree="MATCH (col:Column)-[:CP]->(dbpp:CP)-[:D]->(n:CLASS {D_Node:'true'}) RETURN DISTINCT col.name AS Column_Name,n.name AS Class_Name,count(col.name) AS INDEGREE ORDER BY INDEGREE DESC";
	    	Result colresult = graphDb.execute(coldegree);
  		     while ( colresult.hasNext() )
  		     {
  		         Map<String, Object> row1 = colresult.next();
  		         data[row][0]=row1.get(colresult.columns().get(1)).toString();
  		         data[row][1]=row1.get(colresult.columns().get(0)).toString();
  		         data[row][2]=row1.get(colresult.columns().get(2)).toString();
  		         data[row][3]=totmap.get(data[row][0]).toString();
  		         Float f=new Float( Integer.parseInt(data[row][2])/(Integer.parseInt(data[row][3])*1.0));
  		         data[row][4]=f.toString();
				if(mainrepcalflag)
				{
					String key=data[row][1]+"/"+data[row][0];
					if(report1result.containsKey(key))
						report1result.get(key).setDcs(f);
					else
					{
						ReportOne temp=new ReportOne();
						temp.setDcs(f);
						report1result.put(data[row][1]+"/"+data[row][0], temp);						
					}
				}
 		        row++;
  		     }
			tx.success();
			mainrepcalflag=false;
        }
        finally
        {
            tx.finish();
            graphDb.shutdown();
        }
		return data;
	}
	
	@SuppressWarnings("deprecation")
	public String[][] getBUCFromNeo4J()
	{
		int totcolcnt=0;
		for(int tabno=0;tabno<MapCSVtoLOD.noofcsv;tabno++)
			for(int colno=0;colno<MapCSVtoLOD.colnames[tabno].size();colno++)
				totcolcnt++;

		String[][] data=new String[MapCSVtoLOD.totalcccnt][colcnt];
		int row=0;
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( Neo4JConnectUtil.DB_PATH );
		Transaction tx = graphDb.beginTx();
		try
		{
			String coldegree="MATCH (col:Column)-[:CC]->(cc:CLASS {CC_Node:'true'}) RETURN cc.name AS URI, COUNT(col) AS CON_COLS ORDER BY CON_COLS DESC";
	    	Result colresult = graphDb.execute(coldegree);
  		     while ( colresult.hasNext() )
  		     {
  		         Map<String, Object> row1 = colresult.next();
  		         data[row][0]=row1.get(colresult.columns().get(0)).toString();
  		         data[row][1]=row1.get(colresult.columns().get(1)).toString();
  		         Double f=new Double( Integer.parseInt(data[row][1]) / (totcolcnt*1.0) );
  		         data[row][2]=f.toString();
  		         if(rep2callflag)
  		         {
  		        	 CstCsbReport te=new CstCsbReport();
  		        	 te.setCsb(f);
  		        	 report2result.put(data[row][0], te);
  		         }
  		         row++;
  		     }
			tx.success();
        }
        finally
        {
            tx.finish();
            graphDb.shutdown();
        }
		return data;
	}
	
	@SuppressWarnings("deprecation")
	public String[][] getTDCFromNeo4J()
	{
		int totcolcnt=0;
		for(int tabno=0;tabno<MapCSVtoLOD.noofcsv;tabno++)
			for(int colno=0;colno<MapCSVtoLOD.colnames[tabno].size();colno++)
				totcolcnt++;

		String[][] data=new String[MapCSVtoLOD.totaldcnt][colcnt];
		int row=0;
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( Neo4JConnectUtil.DB_PATH );
		Transaction tx = graphDb.beginTx();
		try
		{
			String coldegree="MATCH (col:Column)-[:CP]->(dbpp)-[:D]->(cc:CLASS) RETURN DISTINCT  cc.name AS URI, col.name AS Column";
	    	Result colresult = graphDb.execute(coldegree);
	    	Map<String,Integer> temp=new HashMap<String,Integer>();
  		     while ( colresult.hasNext() )
  		     {
  		         Map<String, Object> row1 = colresult.next();
  		         if(temp.containsKey(row1.get(colresult.columns().get(0)).toString()))
  		        	 temp.put(row1.get(colresult.columns().get(0)).toString(), temp.get(row1.get(colresult.columns().get(0)).toString())+1);
  		         else
  		        	 temp.put(row1.get(colresult.columns().get(0)).toString(), 1);
  		     }
  		     
  		     for(Map.Entry<String,Integer> entry: temp.entrySet())
  		     {
  		         data[row][0]=entry.getKey();
  		         data[row][1]=entry.getValue().toString();
  		         Double d=new Double( Integer.parseInt(data[row][1]) / (totcolcnt*1.0) );
  		         data[row][2]=d.toString();
 				if(rep2callflag)
 				{
 					if(report2result.containsKey(data[row][0]))
 						report2result.get(data[row][0]).setCst(d);
 					else
 					{
 						CstCsbReport t=new CstCsbReport();
 						t.setCst(d);
 						report2result.put(data[row][0], t);
 					}
 				}
  		         row++;
  		     }
			tx.success();
		     rep2callflag=false;
        }
        finally
        {
            tx.finish();
            graphDb.shutdown();
        }
		return data;
	}
	
	public String[][] getReport1()
	{
		mainrepcalflag=true;
		rep2callflag=true;
		report1result=new HashMap<String, ReportOne>();
		getCCS();
		getDCSFromNeo4J();
		getReport2();
		String[][] repdata=new String[report1result.size()][colcnt];
		int row=0;
		for(Map.Entry<String, ReportOne> entry : report1result.entrySet())
		{
			String val=entry.getKey();
			repdata[row][0]=val.substring(val.indexOf('/')+1,val.length());
			repdata[row][1]=val.substring(0,val.indexOf('/'));
			ReportOne temp=entry.getValue();
			double ccs=(temp.getCcs()==-1)?0.0001:temp.getCcs();
			double dcs=(temp.getDcs()==-1)?0.0001:temp.getDcs();
			repdata[row][2]=new Double(ccs).toString();
			repdata[row][3]=new Double(dcs).toString();
			double os=( Math.sqrt(ccs*ccs + dcs*dcs) ) * ( Math.abs( ( ( (ccs/(ccs+dcs)) * Math.log10(ccs) ) + ( (dcs/(ccs+dcs)) * Math.log10(dcs) ) ) ) ) + report2result.get(repdata[row][0]).getGcsv();
			repdata[row][4]=new Double(os).toString();
			temp.setOs(os);
			row++;
		}
		return repdata;
	}
	
	public String[][] getReport2()
	{
		report2result=new HashMap<String, CstCsbReport>();
		getBUCFromNeo4J();
		getTDCFromNeo4J();
		String cstcsbdata[][]=new String[report2result.size()][colcnt];
		int row=0;
		for(Map.Entry<String, CstCsbReport> entry : report2result.entrySet())
		{
			cstcsbdata[row][0]=entry.getKey();
			CstCsbReport temp=entry.getValue();
			double csb=(temp.getCsb()==-1)?0.0001:temp.getCsb();
			double cst=(temp.getCst()==-1)?0.0001:temp.getCst();
			cstcsbdata[row][1]=new Double(csb).toString();
			cstcsbdata[row][2]=new Double(cst).toString();
			double gcsv=( Math.sqrt(cst*cst + csb*csb) ) * ( Math.abs( ( ( (csb/(csb+cst)) * Math.log10(csb) ) + ( (cst/(cst+csb)) * Math.log10(cst) ) ) ) );
			cstcsbdata[row][3]=new Double(gcsv).toString();
			temp.setGcsv(gcsv);
			row++;			
		}
		return cstcsbdata;
	}
}