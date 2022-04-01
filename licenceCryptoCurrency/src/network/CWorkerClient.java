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
	
	static private int nr = 0;
	
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
		Thread thread = new Thread( this, "WorkerClient" + nr + "_thread" );
		nr++;
		thread.start();
		System.out.println("Worker_thread created");
		return thread;
	}
	
	@Override
	public void run()
	{
		System.out.println("CWorkerClient.run(), remove , before");
		try(OutputStream output = clientSocket.getOutputStream();
			ObjectOutputStream outStream = new ObjectOutputStream( output );
			InputStream input = clientSocket.getInputStream();
			ObjectInputStream inStream = new ObjectInputStream( input ))
		
		{
			System.out.println("CWorkerClient.run(), remove, after");
			mProcessClient( inStream, outStream );
		}catch( Exception e ){
			e.printStackTrace();
		}
	}
}
