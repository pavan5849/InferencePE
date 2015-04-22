package org.iiitb.wsl.inference.firststep;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class MapCSVtoLOD 
{
	private static final int DATA_READ_COUNT=5;
	static String[] csvFiles;
	static final String DBPEDIA_SERVICE = "http://dbpedia.org/sparql";
	static final String LOD_SERVICE = "http://lod.openlinksw.com/sparql";
	static final int TOPN_KEYS = 10;// Used for selecting top N candidates of bottom up approach 
	static List<String>[] colnames;// Contains column names of each CSV file
	static List<Map<String, Integer>[]> dataUris;// contains the bottom up URI's of each column of each table
	static List<TopDown[]> colUris;// contains top down URI's of each column of each table
	static Map<String, Integer>[] hypothesis;// contains hypothesis of each table
	static List<List<String>[]> topN;// contains top N candidate classes of bottom up approach of the column of each table
	static List<Integer>[] totalfreq;
	static int noofcsv;
	static int totalcccnt;
	static int totaldcnt;
	Inference inference;
	boolean lodconflag;

	BufferedReader br = null;
	String line = "";
	String cvsSplitBy = ",";
	QueryExecution qe=null, qe1 = null, qe2 = null;
	QuerySolution q = null,q1=null, q2 = null;
	
	public MapCSVtoLOD(String[] csvFiles,final Inference inf,boolean lodconflag)
	{
		MapCSVtoLOD.csvFiles=csvFiles;
		this.inference=inf;
		this.lodconflag=lodconflag;
		noofcsv=csvFiles.length;
		
		intializeFirstLevelVairables();
		for (int filenum = 0; filenum < noofcsv; filenum++)
		{			
			setDataToOutput("\n------------------------Table Name:"+csvFiles[filenum]+"------------------------");
			colnames[filenum] = new ArrayList<String>();
			hypothesis[filenum] = new LinkedHashMap<String, Integer>();
			totalfreq[filenum]=new ArrayList<Integer>();
			try 
			{
				br = new BufferedReader(new FileReader(csvFiles[filenum]));
				csvFiles[filenum]=csvFiles[filenum].substring(csvFiles[filenum].lastIndexOf("/")+1,csvFiles[filenum].lastIndexOf("."));
			}
			catch (FileNotFoundException e) 
			{
				JOptionPane.showMessageDialog(null, "CSV File Not found "+ csvFiles[filenum] + " -- " + e.getMessage(), "Error",JOptionPane.CANCEL_OPTION);
				e.printStackTrace();
			}
			readColumnNames(filenum);
			bottomUpApproach(filenum);
			topDownApproach(filenum);
			
			setDataToOutput("\n\n-----------Hypothesis Vector-----------\n");
			for(Entry<String,Integer> entry:hypothesis[filenum].entrySet())				
				setDataToOutput("\n"+entry.getKey()+"-"+entry.getValue());
			setDataToOutput("\n\n=======================================================================================\n\n");
		}
		
		new Neo4JConnectUtil().connectAndCreateGraphInNEO4J(); //To enable Persistence of graph in Neo4j
	}

	// Initialization of main variables
	@SuppressWarnings("unchecked")
	public void intializeFirstLevelVairables() 
	{
		colnames = new List[csvFiles.length];
		hypothesis = new LinkedHashMap[csvFiles.length];
		totalfreq=new List[csvFiles.length];
		dataUris = new ArrayList<Map<String, Integer>[]>(csvFiles.length);
		colUris = new ArrayList<TopDown[]>(csvFiles.length);
		topN = new ArrayList<List<String>[]>(csvFiles.length);
	}
	
	// Read column names and initialize the second level variables
	@SuppressWarnings("unchecked")
	public void readColumnNames(int fileno) 
	{
		try 
		{
			if ((line = br.readLine()) != null) 
			{
				String[] colcount = line.split(cvsSplitBy);
				colUris.add(new TopDown[colcount.length]);
				dataUris.add(new LinkedHashMap[colcount.length]);
				topN.add(new ArrayList[colcount.length]);
				for (int i = 0; i < colcount.length; i++)
				{
					totalfreq[fileno].add(i, 0);
					colnames[fileno].add(colcount[i]);
					dataUris.get(fileno)[i] = new LinkedHashMap<String, Integer>();					
				}
			}
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null,"Unable to read column names from CSV file in readColumnNames() -- "+ e.getMessage(), "Error",JOptionPane.CANCEL_OPTION);
			e.printStackTrace();
		}
	}

	// Bottom Up approach
	public void bottomUpApproach(int fileno) 
	{
		int uricnt=0,readcnt=0;
		Random r=new Random();
		String uri="";
		try
		{
			while ((line = br.readLine()) != null && readcnt<DATA_READ_COUNT )
			{
				if(r.nextInt()%2!=0)
					continue;
				readcnt++;
				String[] data = line.split(cvsSplitBy);
				for (int i = 0; i < data.length; i++) 
				{
					try
					{
						Integer.parseInt(data[i]);
					}
					catch(NumberFormatException nmf)
					{		
						StringBuilder mainquery = new StringBuilder("SELECT ?s WHERE {?s <http://www.w3.org/2000/01/rdf-schema#label> \""+ data[i] + "\"@en }");
						try 
						{
							qe1 = QueryExecutionFactory.sparqlService(DBPEDIA_SERVICE,mainquery.toString());
							ResultSet rs1 = qe1.execSelect();
							while (rs1.hasNext()) 
							{
								q = rs1.nextSolution();
								StringBuilder subquery = new StringBuilder( "select ?o where {<" + q.get("s").toString() + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o}");
								
								try 
								{
									qe2 = QueryExecutionFactory.sparqlService(DBPEDIA_SERVICE, subquery.toString());
									ResultSet rs2 = qe2.execSelect();
									while (rs2.hasNext()) 
									{
										uricnt = 1;
										q2 = rs2.nextSolution();
										uri = q2.get("o").toString();
										if (dataUris.get(fileno)[i].containsKey(uri))
											uricnt = dataUris.get(fileno)[i].get(uri) + 1;
										dataUris.get(fileno)[i].put(uri, uricnt);
									}
								}
								catch (Exception e) 
								{
									JOptionPane.showMessageDialog(null,	"Unable to connect to DBPedia -- "+ e.getMessage(), "Error",	JOptionPane.CANCEL_OPTION);
									e.printStackTrace();
								}								
							}
							totalfreq[fileno].add(i, (totalfreq[fileno].get(i)+1));
						} 
						catch (Exception e1)
						{
							JOptionPane.showMessageDialog(null,	"Unable to connect to DBPedia -- "+ e1.getMessage(), "Error",	JOptionPane.CANCEL_OPTION);
							e1.printStackTrace();
						}
					}
				}
			}
			
			for(int temp=0;temp<colnames[fileno].size();temp++)
			{
				setDataToOutput("\n\n---------------Bottom Up Candidate classes for Column :"+colnames[fileno].get(temp)+"---------------\n");
				for(Entry<String,Integer> entry:dataUris.get(fileno)[temp].entrySet())
				{
					setDataToOutput("\n"+entry.getKey()+" - "+entry.getValue());
					totalcccnt++;
				}
			}			
		} 
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(null,	"Unable to read data from CSV file in bottomUpApproach() -- "+ e.getMessage(), "Error",	JOptionPane.CANCEL_OPTION);
			e.printStackTrace();
		}
		for(int i=0;i<colnames[fileno].size();i++)
			topN.get(fileno)[i]=topNKeys(dataUris.get(fileno)[i],TOPN_KEYS);
	}
	
	// Top down approach with 2 pass algorithm
	public void topDownApproach(int fileno) 
	{
		pass1(fileno);
		//pass2(fileno);
	}
	
	public void pass1(int fileno)
	{
		for(int i=0;i<colnames[fileno].size();i++)	
			callDBPediaAndLOD(fileno,colnames[fileno].get(i),i);
	}
	
	public void callDBPediaAndLOD(int fileno, String colname , int colno)
	{
		Map<String,Integer> cpmap=new HashMap<String,Integer>();
		Map< String,Map< String,Set<String> > > cpdec = new HashMap<String, Map<String,Set<String>>>();
		
		if(!colname.contains("_")) //One Step column name with out "_"
		{
			callDBandLODforColumnName(fileno, colno, colname, cpmap, cpdec);
		}
		else // Three Step Process column name as "a_b" "ab" "a b"
		{
			setDataToOutput("\n Running Topdown for column (With _) "+colname+"\n");
			callDBandLODforColumnName(fileno, colno, colname, cpmap, cpdec); // with a_b
			String spacecolname=colname.replace('_',  ' '); 
			setDataToOutput("\n Running Topdown for column (With space) "+spacecolname+"\n");
			callDBandLODforColumnName(fileno, colno, spacecolname, cpmap, cpdec);// with "a b"
			String concatcolname=colname.replace("_",""); 
			setDataToOutput("\n Running Topdown for column (With concat) "+concatcolname+"\n");
			callDBandLODforColumnName(fileno, colno, concatcolname, cpmap, cpdec);// with "ab"			
		}
		
		TopDown td=new TopDown();
		td.setCpfreq(cpmap);
		td.setCpdec(cpdec);
		colUris.get(fileno)[colno]=td;

		// Print data to output Text Field
		setDataToOutput("\n\n---------Top Down classes for Column :"+colnames[fileno].get(colno)+"---------\n");
		for(Map.Entry<String, Map <String, Set<String> > > entry : cpdec.entrySet())
		{
			setDataToOutput("\nProperty- "+entry.getKey()+"= Frequancy: "+cpmap.get(entry.getKey())+"\n");
			for(Map.Entry<String, Set<String> > entry1 : entry.getValue().entrySet())
			{
				if(entry1.getValue().size()==0)
					setDataToOutput("\n"+entry.getKey()+" - "+ entry1.getKey()+" - NONE ");
				else
				{
					Iterator<String> ecit=entry1.getValue().iterator();
					while(ecit.hasNext())
						setDataToOutput("\n"+entry.getKey()+" - "+ entry1.getKey()+" - "+ecit.next() );
				}
				totaldcnt++;				
			}
		}
	}
	
	public void callDBandLODforColumnName(int fileno, int colno, String colname, Map<String, Integer> cpmap,  Map<String, Map<String, Set<String> > > cpdec)
	{
		QueryExecution rangeqexe=null;
		QuerySolution rangeq=null;
		ResultSet  rangers=null;

		String labelquery="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  PREFIX owl: <http://www.w3.org/2002/07/owl#> select distinct ?s where {{{?s a owl:DatatypeProperty} UNION{?s a owl:ObjectProperty} UNION{?s a rdf:Property}}.?s rdfs:label ?l. FILTER regex(?l , \""+colname+"\", \"i\")}";
		getCandidatePropertiesFromService(DBPEDIA_SERVICE, cpmap, labelquery, colname);
		String commentquery="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  PREFIX owl: <http://www.w3.org/2002/07/owl#>  select distinct ?s where {{{?s a owl:DatatypeProperty} UNION{?s a owl:ObjectProperty} UNION{?s a rdf:Property}}.?s rdfs:comment ?l. FILTER regex(?l , \""+colname+"\", \"i\")}";
		getCandidatePropertiesFromService(DBPEDIA_SERVICE, cpmap, commentquery, colname);
		
		if(lodconflag)
		{
			getCandidatePropertiesFromService(LOD_SERVICE, cpmap, labelquery, colname);
			getCandidatePropertiesFromService(LOD_SERVICE, cpmap, commentquery, colname);
		}
		
		for(Map.Entry<String, Integer> entry : cpmap.entrySet()) // Get the Range class from LOD and check in Bottom up CC's for that particular column of a table
		{
			String s=entry.getKey();
			try 
			{
				String range=" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  select ?r where {<"+s+"> rdfs:range ?r}";
				rangeqexe=(lodconflag)?QueryExecutionFactory.sparqlService(LOD_SERVICE,range):QueryExecutionFactory.sparqlService(DBPEDIA_SERVICE,range);
				rangers= rangeqexe.execSelect();
				if(rangers.hasNext())
				{
					rangeq=rangers.nextSolution();
					String ran=rangeq.get("r").toString();
					if(dataUris.get(fileno)[colno].containsKey(ran)) // Check in Bottom Up CC, IF present then find domain and its equivalent classes
					{
						fillDEC(fileno, cpdec, s);
					}
					else{}//Remove the property from cpmap because it is not part of bottom up CC
				}
				else// If there is no range associated with the candidate property
				{
					fillDEC(fileno, cpdec, s);
				}
			} 
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(null,	"Unable to connect to LOD -- "+ e.getMessage(), "Error",	JOptionPane.CANCEL_OPTION);
				e.printStackTrace();
			}
		}		
	}
	
	public void fillDEC(int fileno, Map< String,Map< String,Set<String> > > cpdec, String s)
	{
		QueryExecution dexec=null,ecexec=null;
		QuerySolution dq=null,ecq=null;
		ResultSet  drs=null, ecrs=null;
		boolean domainflag=false;
		Map<String,Set<String>> dmap=new HashMap<String,Set<String>>();

		try 
		{
			String getd=" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  select distinct ?d where { "
							+"{<"+s+"> rdfs:domain ?d} UNION "
							+"{<"+s+"> rdfs:isDefinedBy ?d } } ";
			dexec=(lodconflag)?QueryExecutionFactory.sparqlService(LOD_SERVICE,getd):QueryExecutionFactory.sparqlService(DBPEDIA_SERVICE,getd);
			drs= dexec.execSelect();
			while(drs.hasNext())
			{
				domainflag=true;
				dq=drs.nextSolution();
				String dclass=dq.get("d").toString();
				
				Set<String> ecset=new HashSet<String>();
				dmap.put(dclass,ecset);
				
				try 
				{
					String getec="PREFIX owl: <http://www.w3.org/2002/07/owl#>  select distinct ?ec where { <"+dclass+"> owl:equivalentClass ?ec  }";
					ecexec=(lodconflag)?QueryExecutionFactory.sparqlService(LOD_SERVICE,getec):QueryExecutionFactory.sparqlService(DBPEDIA_SERVICE,getec);
					ecrs= ecexec.execSelect();
					
					while(ecrs.hasNext())
					{
						ecq=ecrs.nextSolution();
						String eclass=ecq.get("ec").toString();
						ecset.add(eclass);
						
						if(hypothesis[fileno].containsKey(eclass))
							hypothesis[fileno].put(eclass, hypothesis[fileno].get(eclass)+1);
						else
							hypothesis[fileno].put(eclass, 1);
					}
				} 
				catch (Exception e)
				{
					JOptionPane.showMessageDialog(null,	"Unable to connect to LOD -- "+ e.getMessage(), "Error",	JOptionPane.CANCEL_OPTION);
					e.printStackTrace();
				}
										
				if(hypothesis[fileno].containsKey(dclass))
					hypothesis[fileno].put(dclass, hypothesis[fileno].get(dclass)+1);
				else
					hypothesis[fileno].put(dclass, 1);
			}
		} 
		catch (Exception e) 
		{
			JOptionPane.showMessageDialog(null,	"Unable to connect to LOD -- "+ e.getMessage(), "Error",	JOptionPane.CANCEL_OPTION);
			e.printStackTrace();
		}
		if(domainflag)// Add only the property which has at least one domain class 
			cpdec.put(s, dmap);
	}

	public void getCandidatePropertiesFromService(String service, Map<String,Integer> cpmap, String query, String colname)
	{
		QueryExecution cpqexe=null;
		QuerySolution cpq= null;
		ResultSet cprs=null;
	
		try {
			cpqexe=QueryExecutionFactory.sparqlService(service,query);
			cprs = cpqexe.execSelect();
			
			while(cprs.hasNext())
			{
				cpq=cprs.nextSolution();
				String s=cpq.get("s").toString();
				if(!cpmap.containsKey(s))
					cpmap.put(s, 1);
				else
					cpmap.put(s, cpmap.get(s)+1);
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,	"Unable to connect to DBPedia -- "+ e.getMessage(), "Error",	JOptionPane.CANCEL_OPTION);
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<String> topNKeys(final Map<String, Integer> map, int n) {
	    PriorityQueue<String> topN = new PriorityQueue<String>(n, new Comparator<String>() {
	        public int compare(String s1, String s2) {
	            return Double.compare(map.get(s1), map.get(s2));
	        }
	    });

	    for(String key:map.keySet()){
	        if (topN.size() < n)
	            topN.add(key);
	        else if (map.get(topN.peek()) < map.get(key)) {
	            topN.poll();
	            topN.add(key);
	        }
	    }
	    return new ArrayList( Arrays.asList(topN.toArray()));
	}
	
	public void setDataToOutput(final String data)
	{
		System.out.println(data);
		inference.output.setText(inference.output.getText() + data);				
	}
}
