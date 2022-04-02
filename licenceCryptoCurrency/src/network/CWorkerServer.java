package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import main.CConfiguration;
import states.EConnectionState;
import states.EConnectionType;

public class CWorkerServer implements Runnable
{	
//	private int threadSleepTime = 3000;
	private ServerSocket	fServerSocket 	= null;
	private CNetworkData	fData 			= null;
	
	/*
	 * constructor
	 */
	public CWorkerServer()
	{
		try{
			InetAddress bindAddr = InetAddress.getByName( CConfiguration.localNodeIP );			
			fServerSocket = new ServerSocket( CConfiguration.serverPort, CConfiguration.backlog, bindAddr );	
			fData = CNetworkData.mGetNetworkDataSingleton();
		}catch( IOException e){
			e.printStackTrace();
		}
	}
	
	/*
	 * returns true is the ip is of a base node
	 */
	private boolean mIsIpBase(String ip)
	{
		for( String ipAddress: CConfiguration.ipBaseNodesList ){
			if( ipAddress.equals( ip ) ){
				return true;
			}
		}
		return false;
	}
	
	/*
	 * returns the type of this connection
	 */
	private EConnectionType mGetConnectionType( String incomingIP )
	{
		EConnectionType connectionType;
		
		if( CConfiguration.isBaseNode ){
			if( mIsIpBase( incomingIP ) ){
				connectionType = EConnectionType.eBaseToBase;
			}else{
				connectionType = EConnectionType.eRegularToBase;
			}
		}else{
			if( mIsIpBase( incomingIP ) ){
				connectionType = EConnectionType.eBaseToRegular;
			}else{
				connectionType = EConnectionType.eRegularToRegular;
			}
		}
		return connectionType;
	}
	
	private void mCreateConnectionForDuplex( String incomingIp, EConnectionType conType )
	{
		try{
			InetAddress address = InetAddress.getByName( incomingIp );
			InetAddress bindAddr = InetAddress.getByName( CConfiguration.localNodeIP );
			System.out.println("remove"  +  bindAddr.toString() + "  "+ CConfiguration.clientPort + ""   + address.toString() + "  "  );
			Socket socket = new Socket( address, 
					CConfiguration.serverPort, 
					bindAddr, 
					CConfiguration.clientPort );
			CClientOutgoing outConn = new CClientOutgoing( socket, conType, EConnectionState.eNormal );
			Thread worker = outConn.mStartThread();
			
			fData.fNetworkBaseClientsOut.addElement( worker );
		}catch( IOException e ){
			e.printStackTrace();
		}
	}
	
	private void mProcessConnection( String incomingIp, Thread workerThread )
	{
//		try{
//			// give a chance to initiated connections to establish
//			Thread.sleep( threadSleepTime );
//		}catch( InterruptedException e ){
//			e.printStackTrace();
//		}		
		EConnectionType conType = mGetConnectionType( incomingIp );
		boolean initiated = false;
		//check the list of initiated connections to know if we need to create the outgoing conn
		for( String ip: fData.fIpListInitiatedConnections ){
			if( ip.equals( incomingIp ) ){
				initiated = true;
				break;
			}
		}		
		if( ! fData.mDataValid() ){
			System.err.println( "not all connections are duplex" );
		}		
		switch( conType )
		{
			case eBaseToBase:
				//add this thread to the list of known connection threads
				fData.fNetworkBaseClientsIn.addElement( workerThread );				
				//create the outgoing connection to complete the duplex connection
				//if not already present
				if( ! initiated ){
					System.out.println("CWorkerServer.mProcessConnection(), remove, create duplex for base");
					mCreateConnectionForDuplex( incomingIp, conType );				
				}
				break;
			case eBaseToRegular:
				if( ! initiated ){
					//this is an error
					System.err.println( "there is no base to regular connection unless if duplex" );
				}else{
					//add this thread to the list of known connection threads
					fData.fNetworkClientsIn.addElement( workerThread );
				}
				break;
			case eRegularToBase:
				//already taken care in CClientIncoming::mProcessEList()
//				fData.fIpRegularNodeList.addElement( incomingIp );
//				//incoming connection from regular to base already established, complete the duplex
//				if( fData.fNetworkClientsOut.size() < CConfiguration.numberOfRegularConnections ){
//					//add this thread to the list of known connection threads
//					fData.fNetworkClientsIn.addElement( workerThread );
//					mCreateConnectionForDuplex( incomingIp, conType );				
//				}
				break;
			case eRegularToRegular:
				//create the outgoing connection to complete the duplex connection
				if( ! initiated ){
					if( fData.fNetworkClientsIn.size() < CConfiguration.numberOfRegularConnections ){
						//add this thread to the list of known connection threads
						fData.fNetworkClientsIn.addElement( workerThread );
						mCreateConnectionForDuplex( incomingIp, conType );
					} //else, the client will close by itself
				}else{
					//add this thread to the list of known connection threads
					fData.fNetworkClientsIn.addElement( workerThread );
				}
				break;
			default:
				System.err.println( "no defined connection type" );
		}
	}
	
	@Override
	public void run()
	{
		//client socket used for incoming messages from other nodes
		Socket inClientSocket = null;
		
		while( true ){
			try{
				inClientSocket = fServerSocket.accept();
			}catch( IOException e ){
				e.printStackTrace();
			}
			String incomingIP = inClientSocket.getInetAddress().getHostAddress();
			EConnectionType connectionType = mGetConnectionType( incomingIP );

			CClientIncoming inConnection = new CClientIncoming( inClientSocket, connectionType );
			Thread workerThread = inConnection.mStartThread();
			
			mProcessConnection( incomingIP, workerThread );
		}
	}
}
