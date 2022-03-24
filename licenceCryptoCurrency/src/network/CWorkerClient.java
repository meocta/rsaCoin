package network;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import miner.CMinerData;
import states.EConnectionType;

public abstract class CWorkerClient implements Runnable
{
	protected EConnectionType connectioType = EConnectionType.eUnknown;
	protected Socket       clientSocket = null;
	protected CNetworkData fNetworkData = null;
	protected CMinerData   fNodeData    = null;
	
	/*
	 * main activity of the client
	 */
	abstract void mProcessClient( ObjectInputStream in, ObjectOutputStream out ) throws IOException;
	
	public CWorkerClient( Socket client, EConnectionType conType )
	{
		clientSocket = client;
		connectioType = conType;
		fNetworkData = CNetworkData.mGetNetworkDataSingleton();
		fNodeData = CMinerData.mGetInstance();
	}
	
	public Thread mStartThread()
	{
		Thread thread = new Thread( this );		
		thread.start();		
		return thread;
	}
	
	@Override
	public void run()
	{
		System.out.println("CWorkerClient.run(), remove");
		try(InputStream input = clientSocket.getInputStream();
			OutputStream output = clientSocket.getOutputStream();
			ObjectInputStream inStream = new ObjectInputStream( input );
			ObjectOutputStream outStream = new ObjectOutputStream( output ))
		{
			mProcessClient( inStream, outStream );
		}catch( Exception e ){
			e.printStackTrace();
		}
	}
}
