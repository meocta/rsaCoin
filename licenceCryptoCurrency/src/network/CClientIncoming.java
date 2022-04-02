package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import data.CBlock;
import data.CNetworkObject;
import data.CTransaction;
import main.CConfiguration;
import shared.CMinToNetBlockchain;
import shared.CNetToMinBlocks;
import shared.CNetToMinTransactions;
import states.EBlockChainState;
import states.EConnectionState;
import states.EConnectionType;
import states.EObjectType;

/*
 * waits for incoming messages over the network
 * receives data for the node
 */
public class CClientIncoming extends CWorkerClient
{
	private CNetToMinBlocks fInBlocks= null;
	private CNetToMinTransactions fInTransactions = null;
	
	private int threadSleepTime = 10000;
	
	public CClientIncoming( Socket client, EConnectionType conType )
	{
		super(client, conType);
		
		fInBlocks = CNetToMinBlocks.mGetInstance();
		fInTransactions = CNetToMinTransactions.mGetInstance();
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
			CClientOutgoing outConn = new CClientOutgoing( socket, EConnectionType.eRegularToBase, conState );
			return outConn.mStartThread();
		}catch( IOException e ){
			e.printStackTrace();
		}
		return null;
	}
	
	private boolean mProcessEList( ObjectOutputStream outStream ) throws IOException
	{
		boolean repeat = true;
		//valid only for base nodes, the call comes from regular only
		if( CConfiguration.isBaseNode ){
			if( CConfiguration.numberOfRegularConnections > fNetworkData.fNetworkClientsIn.size() ){
				//send the ip list of peers, including this base node
				fNetworkData.fIpRegularNodeList.insertElementAt( CConfiguration.localNodeIP, 0 );
				outStream.writeObject( fNetworkData.fIpRegularNodeList );
				fNetworkData.fIpRegularNodeList.remove( 0 );
				// keep this connection
				fNetworkData.fNetworkClientsIn.addElement( Thread.currentThread() );
				
				String remoteIp = clientSocket.getInetAddress().getHostAddress();
				//create an outgoing pair connection
				Thread outCon = mCreateOutConnection( remoteIp, EConnectionState.eNormal );
				
				fNetworkData.fNetworkBaseClientsOut.addElement( outCon );
				//save this ip in the list of ips of regular nodes
				fNetworkData.fIpRegularNodeList.addElement( remoteIp );
			}else{
				//send the ip list of peers
				outStream.writeObject( fNetworkData.fIpRegularNodeList );
				//close this connection
				repeat = false;
			}							
		}else{
			System.err.println( "current node not base! no peer list available!" );
		}		
		return repeat;
	}
	
	private void mProcessEFirst( ObjectOutputStream outStream ) throws IOException
	{
		CMinToNetBlockchain blockchain = CMinToNetBlockchain.mGetInstance();
		int blocksNumber = fNodeData.mGetBlocksNumber();
		//wake up producer, or wait for other consumer to finish
		blockchain.mSetConsumerActive();

		while( true ){							
			if( 0 == blocksNumber ){
				//blockchain was transfered, exit loop
				//before exiting wake up other consumers
				blockchain.mWakeUpConsumers();
				break;
			}else{
				//send the number of blocks to the peer node
				outStream.writeInt( blocksNumber );
			}
			
			for( int i = 0; i < blocksNumber; i++ ){
				//read the block and send it over the network
				CBlock block = blockchain.mReadBlock();
				if( null == block ){
					System.err.println( "consumer didn't write the next block!" );
				}
				outStream.writeObject( block );
			}			
			//update the number of blocks newly added to the blockchain
			blocksNumber = fNodeData.mGetBlocksNumber() - blocksNumber;							
		}
	}
	
	private void mProcessENormal( ObjectInputStream inStream ) throws IOException, ClassNotFoundException
	{
		while( true ){
			//read continuous flow of blocks and transactions from peer node
			CNetworkObject netObj = ( CNetworkObject )inStream.readObject();
			
			if( EObjectType.eBlock == netObj.mGetType() ){
				CBlock block = ( CBlock )netObj.mGetObject();									
				fInBlocks.mAddBlock( block );
			}else{ // EObjectType.eTransaction
				CTransaction transaction = ( CTransaction )netObj.mGetObject();									
				fInTransactions.mAddTransaction( transaction );
			}							
		}
	}

	@Override
	void mProcessClient( ObjectInputStream inStream, ObjectOutputStream outStream ) throws IOException
	{
		System.out.println( "remove" );

		boolean repeat = true;
		
		while( repeat ){
			if( EBlockChainState.eEmpty == fNodeData.mGetBCState() ||
				EBlockChainState.eHalf  == fNodeData.mGetBCState() )
			{
				System.out.println( "blockchain downloading, ignoring incoming messages" );
				try{
					Thread.sleep( threadSleepTime );
				}catch( InterruptedException e ){
					e.printStackTrace();
				}
			}else{
				//process incoming objects
				try{
					EConnectionState remoteConState = ( EConnectionState )inStream.readObject();
					
					switch( remoteConState )
					{
						case eList:
							//base nodes only. send the regular nodes ip list
							repeat = mProcessEList( outStream );							
							break;
						case eFirst:
							//send the blockchain
							mProcessEFirst( outStream );
							break;
						case eNormal:
							//once we get here, we stay until the connection breaks 
							mProcessENormal( inStream );
							break;
						default:
							System.err.println( "shouldn't be here" );
					}					
				}catch( ClassNotFoundException e ){
					System.err.println( e.toString() );
				}
			}
		}
	}
}
