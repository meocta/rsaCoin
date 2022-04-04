package miner;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import data.CBlock;
import data.CTransaction;
import main.CConfiguration;
import states.EBlockChainState;

public class CMinerData
{
	static private CMinerData minerData = null;
	
	private EBlockChainState fBlockChain = EBlockChainState.eUnknown;
	//the number of blocks held by this node in its copy of blockchain, updated by CMiner
	private int fNumberOfBlocks = 0;
	//holds all the unspent transaction outputs in the blockchain, map< tx hash , outputs vector >
	private Map< byte[], Vector< Integer > > fUnspentTxOutputs = null;
	//holds the transactions that were not integrated yet into the blockchain
	private Vector< CTransaction > fUnboundTransactions = null;
	//holds the blocks for which the parent hasn't arrive yet to this node
	private Vector< CBlock > fOrphanBlocks = null;
	
	private CMinerData()
	{
		fUnspentTxOutputs = new HashMap<>();
		fUnboundTransactions = new Vector<>();
		fOrphanBlocks = new Vector<>();
		
		File bcFolder = new File( CConfiguration.blockchainFolder );
		//count the number of files within the folder. no other files than blocks are there
		
		fNumberOfBlocks = bcFolder.listFiles().length;
		if( 0 < fNumberOfBlocks ){
			mSetBCState( EBlockChainState.eHalf );
		}else{
			mSetBCState( EBlockChainState.eEmpty );
		}
	}
	
	static public CMinerData mGetInstance()
	{
		if( null == minerData ){
			minerData = new CMinerData();
		}
		return minerData;
	}
	
	public synchronized void mSetBCState( EBlockChainState bcState )
	{
		fBlockChain = bcState;
	}
	
	public synchronized EBlockChainState mGetBCState()
	{
		return fBlockChain;
	}
	
	public synchronized void mIncrementBlocksNumber()
	{
		++fNumberOfBlocks;
	}
	
	public synchronized int mGetBlocksNumber()
	{
		return fNumberOfBlocks;
	}
	
	/*
	 * searches for an unspent output 
	 * returns true if output is found, false otherwise
	 */
	public synchronized boolean mSearchUnspentOutput( byte[] txHash, int outputNr )
	{
		for( Map.Entry< byte[], Vector< Integer > > entry : fUnspentTxOutputs.entrySet() ){
			if( Arrays.equals( entry.getKey(), txHash )){
				Vector< Integer > outputs = entry.getValue();
				for( Integer output: outputs ){
					if( output.intValue() == outputNr ){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/*
	 * searches for an unspent output and removes it from the list if found
	 * returns true if output is found, false otherwise
	 */
	public synchronized boolean mRemoveUnspentOutput( byte[] txHash, int outputNr )
	{
		for( Map.Entry< byte[], Vector< Integer > > entry : fUnspentTxOutputs.entrySet()){
			if( Arrays.equals( entry.getKey(), txHash )){
				Vector< Integer > outputs = entry.getValue();
				for( Integer output: outputs ){
					if( output == outputNr ){
						//remove element from value from entry
						outputs.remove( output );
						if( 0 == outputs.size() ){
							//remove empty entry
							fUnspentTxOutputs.remove( entry.getKey() );
						}
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public synchronized void mAddUnspentTransaction( byte[] txHash, Vector< Integer > txOutputs )
	{
		fUnspentTxOutputs.put( txHash, txOutputs );
	}
	
	public synchronized void mAddTransactionList( Vector< CTransaction > txList )
	{
		fUnboundTransactions.addAll( txList );
	}

	/*
	 * returns the index of tx transaction if it is found in the unbound transaction list
	 * else returns -1
	 */
	public synchronized int mGetUnboundTxIndex( CTransaction tx )
	{
		for( int i = 0; i < fUnboundTransactions.size(); i++ ){
			if( tx.mEquals( fUnboundTransactions.elementAt( i ) ) ){
				return i;
			}
		}
		return -1;
	}
	
	/*
	 * returns the transaction at index position from the unbound transaction list
	 */
	public synchronized CTransaction mGetUnboundTransactionAtIndex( int index )
	{
		return fUnboundTransactions.elementAt( index );
	}
	
	/*
	 * returns the unbound transaction list
	 */
	public synchronized Vector< CTransaction > mGetUnboundTransactionsList()
	{
		return fUnboundTransactions;
	}
	
	/*
	 * searches for the tx transaction within the unbound transaction list
	 * if it finds the transaction it removes it from the list returning true
	 * else it will return false
	 */
	public synchronized boolean mRemoveUnboundTransaction( CTransaction tx )
	{
		for( int i = 0; i < fUnboundTransactions.size(); i++ ){
			if( tx.mEquals( fUnboundTransactions.elementAt( i ) ) ){
				fUnboundTransactions.removeElementAt( i );
				return true;
			}
		}
		return false;
	}
	
	/*
	 * removes the transaction from index
	 */
	public synchronized boolean mRemoveUnboundTransaction( int index )
	{
		if( 0 <= index && index< fUnboundTransactions.size() ){
			fUnboundTransactions.removeElementAt( index );
			return true;
		}
		return false;
	}
	
	public synchronized int mGetUnboundTxListSize()
	{
		return fUnboundTransactions.size();
	}
	
	public synchronized void mAddOrphanBlock( CBlock block )
	{
		if( ! fOrphanBlocks.contains( block ) ){//todo: override equals?
			fOrphanBlocks.addElement( block );			
		}
	}
	
	/*
	 * returns an until now orphan block which is the child of prevBlock, or null if it is not found
	 * also removes the block from the list
	 */
	public synchronized CBlock mGetNextBlock( CBlock prevBlock )
	{
		try{
			byte[] prevHash = prevBlock.mGetHeaderHash();
			
			for( int i = 0; i < fOrphanBlocks.size(); i++ ){
				CBlock nextBlock = fOrphanBlocks.elementAt( i );
				
				if( nextBlock.mIsChildOf( prevHash ) ){
					fOrphanBlocks.removeElementAt( i );
					return nextBlock;
				}
			}
		}catch (NoSuchAlgorithmException e){
			e.printStackTrace();
		}
		return null;
	}
}
