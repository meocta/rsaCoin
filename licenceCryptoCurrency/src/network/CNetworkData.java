package network;

import java.util.Vector;

import main.CConfiguration;

public class CNetworkData
{
	static private CNetworkData networkData = null;
	
	public Thread fNetworkServer = null;

	public Vector<Thread> fNetworkClientsIn = null;
	public Vector<Thread> fNetworkClientsOut = null;
	
	public Vector<Thread> fNetworkBaseClientsIn = null;
	public Vector<Thread> fNetworkBaseClientsOut = null;
	
	//some of the connections are initiated by current node
	public Vector< String > fIpListInitiatedConnections = null;
	
	//the list of regular nodes tide to this base node
	//todo: clean up offline nodes
	public Vector< String > fIpRegularNodeList = null;
	//sync the ip list for regular nodes
	private boolean isIpRegNodeListComplete = false;	
	
	private CNetworkData()
	{
		fNetworkClientsIn = new Vector<Thread>( CConfiguration.numberOfRegularConnections );
		fNetworkClientsOut = new Vector<Thread>( CConfiguration.numberOfRegularConnections );
		
		if( CConfiguration.isBaseNode ){
			fIpRegularNodeList = new Vector< String >( 1 );
			
			fNetworkBaseClientsIn = new Vector<Thread>( CConfiguration.numberOfBaseNodes - 1 );
			fNetworkBaseClientsOut = new Vector<Thread>( CConfiguration.numberOfBaseNodes - 1 );
		}
		fIpListInitiatedConnections = new Vector<String>( 1 );
	}
	
	static public CNetworkData mGetNetworkDataSingleton()
	{
		if( null == networkData ){
			networkData = new CNetworkData();
		}
		return networkData;
	}
	
	public void setIpListFlag()
	{
		if( ! CConfiguration.isBaseNode ){
			isIpRegNodeListComplete = true;
			notify();
		}
	}
	
	//only synchronized variable from this class that blocks
	synchronized public boolean getIpListFlag()
	{
		boolean status = false;
		if( ! CConfiguration.isBaseNode ){
			while( ! isIpRegNodeListComplete ){
				try{
					wait();	
					status = true;
				}catch( InterruptedException e ){
					e.printStackTrace();
				}
			}
		}		
		return status;
	}
	
	public boolean mDataValid()
	{
		if( ( fNetworkClientsIn.size() == fNetworkClientsOut.size() )&& 
			( fNetworkBaseClientsIn.size() == fNetworkBaseClientsOut.size() ) )
		{
			return true;
		}
		return false;
	}	
}
