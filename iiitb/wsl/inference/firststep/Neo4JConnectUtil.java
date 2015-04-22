package org.iiitb.wsl.inference.firststep;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class Neo4JConnectUtil 
{
	private Map<String,Node> nodes=new HashMap<String,Node>();
	public static final String DB_PATH = System.getProperty("user.home")+"/InferenceGraph/inference.db";
 	private static GraphDatabaseService graphDb;
 	    
    private static enum RelTypes implements RelationshipType
    {
        COLUMN, CC, EC, D, CP
    }
    
    public void connectAndCreateGraphInNEO4J() 
    {    	
    	graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
    	createDb();
        shutDown();
    }

	@SuppressWarnings({ "deprecation" })
	private void createDb()
    {
        //System.out.println("Graph DB Started");
    	String query="MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r";// Clear the temp data in graph db 
    	try ( Transaction ignored = graphDb.beginTx();  Result result = graphDb.execute( query) )
        {
  			//System.out.println(result.resultAsString());
  			ignored.success();
        }
    	
        Transaction tx = graphDb.beginTx();
        try
        {
        	for(int nooffiles=0;nooffiles<MapCSVtoLOD.csvFiles.length;nooffiles++)
    		{
        		Node tableNode=nodes.get(MapCSVtoLOD.csvFiles[nooffiles]);
        		if(tableNode==null)
        		{	
        			tableNode=graphDb.createNode(DynamicLabel.label("Table"));
        			tableNode.setProperty("name", MapCSVtoLOD.csvFiles[nooffiles]);
        			tableNode.setProperty("type", "TableNode");
        			nodes.put(MapCSVtoLOD.csvFiles[nooffiles], tableNode);
        		}
        		
    			Iterator<String> i=MapCSVtoLOD.colnames[nooffiles].iterator();
    			int colno=0;
    			while(i.hasNext())
    			{
    				String colname=MapCSVtoLOD.csvFiles[nooffiles]+"."+i.next();
    				if(colname!=null)
    				{
    					Node colNode=nodes.get(colname);
    	        		if(colNode==null)
    	        		{	
    	        			colNode=graphDb.createNode(DynamicLabel.label("Column"));
        					colNode.setProperty("name", colname);
        					colNode.setProperty("type", "Column Node");
        					tableNode.createRelationshipTo(colNode, RelTypes.COLUMN);
    	        			nodes.put(colname, colNode);
    	        		}
   									
    					//Adding candidate properties and domain and equivalent classes of the column name
    					for(Entry<String, Map<String, Set<String>>> cpentry : MapCSVtoLOD.colUris.get(nooffiles)[colno].getCpdec().entrySet())
    					{
    						String cp=cpentry.getKey();
							Node cpNode=nodes.get(cp);
							if(cpNode==null)
							{
								cpNode=graphDb.createNode(DynamicLabel.label("CP"));
								cpNode.setProperty("name", cp);
								nodes.put(cp, cpNode);
    							cpNode.setProperty("type", "CP");    						
								colNode.createRelationshipTo(cpNode, RelTypes.CP);
							}
							else if(checkRealtion(colNode,cpNode,RelTypes.CP))
								colNode.createRelationshipTo(cpNode, RelTypes.CP);
							
    						for(Entry<String,Set<String>> dcentry : cpentry.getValue().entrySet())
    						{
								String d=dcentry.getKey();
								Node dNode=nodes.get(d);
								if(dNode==null)
								{
									dNode=graphDb.createNode(DynamicLabel.label("CLASS"));
									dNode.setProperty("name", d);
									dNode.setProperty("D_Node", "true");
									dNode.setProperty("EC_Node", "false");
									dNode.setProperty("CC_Node", "false");
									nodes.put(d, dNode);
									cpNode.createRelationshipTo(dNode, RelTypes.D);
								}
								else if(checkRealtion(cpNode,dNode,RelTypes.D))
									cpNode.createRelationshipTo(dNode, RelTypes.D);

								Iterator<String> ecit=dcentry.getValue().iterator();
								while(ecit.hasNext())
								{
									String ec=ecit.next();
									if(d!=null && cp!=null && ec!=null)
									{    										
										Node ecNode=nodes.get(ec);
										if(ecNode==null)
										{
											ecNode=graphDb.createNode(DynamicLabel.label("CLASS"));
											ecNode.setProperty("name",ec);
											ecNode.setProperty("D_Node", "false");
											ecNode.setProperty("EC_Node", "true");
											ecNode.setProperty("CC_Node", "false");
											nodes.put(ec, ecNode);
											dNode.createRelationshipTo(ecNode, RelTypes.EC);
										}
										else if(checkRealtion(dNode,ecNode,RelTypes.EC))
											dNode.createRelationshipTo(ecNode, RelTypes.EC);									
									}
    							}
    						}	
    					}

    					//Adding candidate classes for data to the graph
    					ListIterator<String> entry =MapCSVtoLOD.topN.get(nooffiles)[colno].listIterator();
    					while(entry.hasNext())
    					{
    						if(entry != null)
    						{
    							String cc=entry.next();
    							Node ccNode=nodes.get(cc);
    							if(ccNode==null)
    							{
    								ccNode=graphDb.createNode(DynamicLabel.label("CLASS"));
    								ccNode.setProperty("name", cc);
    								ccNode.setProperty("D_Node", "false");
    								ccNode.setProperty("EC_Node", "false");
    								ccNode.setProperty("CC_Node", "true");
    								nodes.put(cc, ccNode);
									colNode.createRelationshipTo(ccNode, RelTypes.CC);
    							}
    							else if(checkRealtion(colNode,ccNode,RelTypes.CC))
										colNode.createRelationshipTo(ccNode, RelTypes.CC);
    						}
    					}
    				}
    				colno++;
    			}
    		}
    		tx.success();
        }
        finally
        {
            tx.finish();
        }
    }
        
    private boolean checkRealtion(Node src, Node dest, RelTypes type)
    {
    	Iterable<Relationship> i=src.getRelationships();
    	Iterator<Relationship> it=i.iterator();
    	while(it.hasNext())
    	{
    		Relationship temp=it.next();
    		if(temp.getOtherNode(src).equals(dest))
    			if(temp.isType(type))
    				return false;
    	}
    	return true;
    }
    
    public static void shutDown()
    {
        graphDb.shutdown();
        //System.out.println("graphDB shut down.");   
    }   
}