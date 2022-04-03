package network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

import data.CBlock;
import data.CNetworkObject;
import shared.CNetToMinBlocks;
import shared.CMinToNetObject;
import states.EBlockChainState;
import states.EConnectionState;
import states.EConnectionType;

/*
 * waits for changes made to the shared objects
 * sends data processed by the node
 */
public class CClientOutgoing extends CWorkerClient
{
	private EConnectionState fConState =  null;
	private CMinToNetObject fOutObj = null;
	
	public CClientOutgoing(Socket client, EConnectionType conType, EConnectionState cState)
	{
		super(client, conType);
		fConState = cState;
		fOutObj = CMinToNetObject.mGetInstance();
	}
	
	@SuppressWarnings("unchecked")
	private void mDownloadPeerList( ObjectOutputStream outStream, ObjectInputStream inStream ) throws IOException
	{
		//first send the connection state so the server reacts appropriately 
		outStream.writeObject( fConState );
		
		//receive the ip list in a vector
		try{
			fNetworkData.fIpRegularNodeList = ( Vector<String> )inStream.readObject();					
		}catch( ClassNotFoundException e ){
			System.err.println( e.toString() );
		}
		fNetworkData.setIpListFlag();
	}

	private void mDownloadBlockChain( ObjectOutputStream outStream, ObjectInputStream inStream ) throws IOException
	{
		//start updating the blockchain
		fNodeData.mSetBCState( EBlockChainState.eHalf );
		//blocks transfer structure
		CNetToMinBlocks blockList = CNetToMinBlocks.mGetInstance();
		//todo: send the current block number this host has since last active session 
		outStream.writeObject( fConState );
		int blocksNumber = 0;
		
		while( true ){
			//get the number of blocks the peer has at this very moment
			//server must send only the number of blocks added since the last chunk was sent
			blocksNumber = inStream.readInt();
			
			if( 0 == blocksNumber ){
				//blockchain transfer complete
				fNodeData.mSetBCState( EBlockChainState.eFull );
				break;
			}else{
				while( 0 != blocksNumber-- ){
					try{
						CBlock block = ( CBlock )inStream.readObject();
						blockList.mAddBlock( block );
					}catch( ClassNotFoundException e ){
						System.err.println( e.toString() );
					}
				}
			}
		}
	}
	
	private void mSendBlocksToPeer( ObjectOutputStream outStream ) throws IOException
	{
		while( true ){
			fOutObj.mStartReading();
			CNetworkObject netObj = fOutObj.mGetNetObj();
			fOutObj.mFinishReading();

			outStream.writeObject( netObj );
		}
	}
	
	/*
	 * downloads the ip list of peers
	 * downloads the blockchain
	 * starts the normal traffic
	 */
	@Override
	void mProcessClient( ObjectInputStream inStream, ObjectOutputStream outStream ) throws IOException
	{
		boolean repeat = true;

		while( repeat ){
			switch( fConState )
			{
				case eList:
					System.out.println("elist --------------");
					mDownloadPeerList( outStream, inStream );
					
					//check if first element is base ip
					String baseIp = clientSocket.getInetAddress().getHostAddress();
					if( fNetworkData.fIpRegularNodeList.elementAt( 0 ).equals( baseIp ) ){
						//save the connection
						fNetworkData.fNetworkClientsOut.addElement( Thread.currentThread() );
						fConState = EConnectionState.eFirst;
					}else{
						//todo: make sure that in the list must be at least one active host
						//stop the loop here and exit the thread, closing the connection to base
						repeat = false;
					}
					break;
				case eFirst:
					System.out.println("efirst --------------");
					mDownloadBlockChain( outStream, inStream );
					fConState = EConnectionState.eNormal;
					break;
				case eNormal:
					System.out.println("enormal --------------");
					// normal handling of traffic
					outStream.writeObject( fConState );
					//registration to the shared object used to load transactions and blocks from CMiner and send them over the network
					fOutObj.mRegisterConsumerThread( Thread.currentThread() );
					//main loop of the outgoing connection
					mSendBlocksToPeer( outStream );					
				default:
					System.err.println( "shouldn't be here" );
			}
		}
	}
}
