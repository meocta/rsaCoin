package main;

import java.io.File;

import miner.CMiner;
import network.CNetwork;
import wallet.CWallet;

public class CMain
{	
	//nodes
	private CMiner   fMiner   = null;
	private CNetwork fNetwork = null;
	private CWallet  fWallet  = null;
	
	static private CMain nodeSingletonInstance = null;
	
	private CMain() throws CMainException{
		//new CGenericArray< CCreditAddress >();
		File mainFolder		= new File( CConfiguration.coinFolder );
		File walletFolder	= new File( CConfiguration.walletFolder );
		File bcFolder		= new File( CConfiguration.blockchainFolder );
		File keysFolder		= new File( CConfiguration.keyFolder );

		if( ! mainFolder.exists() ){
			if( ! mainFolder.mkdir()) {
				throw new CMainException( "main folder not created!" );
			}
		}		
		if( ! walletFolder.exists() ){
			if( ! walletFolder.mkdir() ) {
				throw new CMainException( "wallet folder not created!" );
			}
		}
		if( ! bcFolder.exists() ){
			if( ! bcFolder.mkdir() ) {
				throw new CMainException( "blockchain folder not created!" );
			}
		}
		if( ! keysFolder.exists() ){
			if( ! keysFolder.mkdir()) {
				throw new CMainException( "keys folder not created!" );
			}
		}
		fMiner   = CMiner.mGetInstance();
		fNetwork = CNetwork.mGetInstance();
		fWallet  = CWallet.mGetInstance();		
	}
	
	static public CMain mGetInstance(){
		if( null == nodeSingletonInstance ){
			try {
				nodeSingletonInstance = new CMain();				
			} catch ( CMainException e ) {
				System.out.print( e.toString() );
			}			
		}
		return nodeSingletonInstance;
	}

	public void mStartAndJoinNodeThreads(){
		Thread miner   = new Thread( fMiner );
		Thread network = new Thread( fNetwork );
		Thread wallet  = new Thread( fWallet );
		
		miner.start();
		network.start();
		wallet.start();
		
		try{
			miner.join();
			network.join();
			wallet.join();
		}catch( InterruptedException e ){
			System.out.println( e.toString() );
		}
	}
	// main program of the application
	public static void main(String args[]){
		CMain mainNode = CMain.mGetInstance();
		mainNode.mStartAndJoinNodeThreads();
		System.out.println(" -- test -- end ");
	}
}
