package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;

import main.CConfiguration;
import miner.CMinerData;
import states.EBlockChainState;
import states.EConnectionState;
import states.EConnectionType;

public class CNetwork implements Runnable
{
	static private CNetwork networkSingletonInstance = null;
	
	private CMinerData nodeData = null;
	private CNetworkData networkData = null;	
	//for regular nodes, this is the ip of the base node to which the first connection was established
	private String fBaseNodeIp = null;
	
	private CNetwork()
	{
		nodeData = CMinerData.mGetInstance();
		networkData = CNetworkData.mGetNetworkDataSingleton();
	}
	
	static public CNetwork mGetInstance()
	{
		if( null == networkSingletonInstance ){
			networkSingletonInstance = new CNetwork();
		}
		return networkSingletonInstance;
	}
	
	/*
	 * waiting for incoming connection requests from peers
	 * creates incoming connection threads
	 * if there is none, creates outgoing connection threads for this peer
	 * if base node, it listens for all queries, both from other bases and from regulars
	 */
	private void mStartServer()
	{
		networkData.fNetworkServer = new Thread( new CWorkerServer(), "WorkerServer_thread" );
		networkData.fNetworkServer .start();
		System.out.println("WorkerServer_thread created");
	}
	
	private Thread mCreateOutConnection(String remoteIP, EConnectionState conState )
	{
		try{
			InetAddress remoteAddr = InetAddress.getByName( remoteIP );
			InetAddress localAddr = InetAddress.getByName( CConfiguration.localNodeIP );
			Socket socket = new Socket( remoteAddr, 
					CConfiguration.serverPort, 
					localAddr, 
					CConfiguration.clientPort );
			//todo: EConnectionType change from basetobase
			CClientOutgoing outConn = new CClientOutgoing( socket, EConnectionType.eBaseToBase, conState );
			return outConn.mStartThread();
		}catch( IOException e ){
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * base nodes connect between themselves
	 */
	private void mBaseNodeConnect()
	{
		//first connection, blockchain will be updated
		EConnectionState conState = EConnectionState.eFirst;
		
		for( String baseIP: CConfiguration.ipBaseNodesList ){
			if( baseIP.equals( CConfiguration.localNodeIP )){
				continue;
			}else{
				//initialize connection
				Thread worker = mCreateOutConnection( baseIP, conState );
				if( null == worker ){
					System.out.println("CNetwork.mBaseNodeConnect(), connection to" + baseIP + " failed ");
				}else{
					System.out.println("CNetwork.mBaseNodeConnect(), connection to" + baseIP + " succeded ");
					conState = EConnectionState.eNormal;					
					networkData.fIpListInitiatedConnections.addElement( baseIP );
					networkData.fNetworkBaseClientsOut.addElement( worker );
				}				
			}
		}
	}	
	
	private Thread mGetConnectionToBase()
	{
		//first connection, ip list  is filled in
		EConnectionState conState = EConnectionState.eList;		
		Thread worker = null;		
		//todo: add random access to the list to even the load on base nodes
		for( String baseIP: CConfiguration.ipBaseNodesList ){
			worker = mCreateOutConnection( baseIP, conState );
			if( null == worker ){
				//if connection was unsuccessful, try other nodes
				continue;
			}else{
				fBaseNodeIp = baseIP;				
				//once a connection is established with a base node, terminate
				break;
			}
		}
		return worker;
	}
	
	/*
	 * connect to regular nodes
	 * if the first ip in list is base, connect to base
	 * all connections are done in separate worker threads
	 */
	private void mConnectToPeers( Thread baseConnection )
	{
		//wait for the list to be populated
		if( networkData.getIpListFlag() ){
			EConnectionState conState = EConnectionState.eFirst;
			int connections = 0;
			
			// todo: randomize the list/search
			for( String currentIP: networkData.fIpRegularNodeList ){
				if( currentIP.equals( fBaseNodeIp ) ){
					//skip creating a connection of base ip since the initial connection is kept
					++connections;					
					//base will transfer the blockchain, as the connection with base is kept
					nodeData.mSetBCState( EBlockChainState.eHalf );
					conState = EConnectionState.eNormal;
					continue;
				}				
				Thread worker = mCreateOutConnection( currentIP, conState );
				if( null != worker ){
					++connections;
					networkData.fNetworkClientsOut.addElement( worker );
					if( connections == 1 ){
						//first connection will bring in the blockchain
						conState = EConnectionState.eNormal;
						nodeData.mSetBCState( EBlockChainState.eHalf );							
					}
					if( connections == CConfiguration.numberOfRegularConnections ){
						//maximum number of connections reached, exit loop
						break;
					}
				}
			}
		}
	}	
	
	private void mJoinWorkers( Vector< Thread > workers  )
	{
		for( Thread worker : workers ){
			try{
				//todo: join the workers that are created later as well
				worker.join();					
			}catch( InterruptedException e ){
				e.toString();
			}
		}		
	}
	
	@Override
	public void run()
	{		
		mStartServer();		
		if( CConfiguration.isBaseNode ){
			mBaseNodeConnect();			
			mJoinWorkers( networkData.fNetworkBaseClientsOut );	
		}else{
			//obtain the IP list of other regular peers
			Thread baseConnectionThread = mGetConnectionToBase();
			//current thread will block until the IP list of regular nodes is populated
			mConnectToPeers( baseConnectionThread );
			mJoinWorkers( networkData.fNetworkClientsOut );
		}		
	}
}
