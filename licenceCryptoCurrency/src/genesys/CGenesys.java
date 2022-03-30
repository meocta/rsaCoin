package genesys;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import cryptography.CCryptoSunrsasign;
import cryptography.CHelper;
import data.CBlock;
import data.CTransaction;
import data.CTuple;
import main.CConfiguration;
import miner.CBlockChain;
import wallet.CWalletData;

public class CGenesys
{
	private CBlockChain fBC = null;
	private CWalletData fWD = null;
	
//	private String mGetRandAlias()
//	{
//		Long randomizer = System.currentTimeMillis();
//		String alias = randomizer.toString();
//		
//		return alias;
//	}
	
	// reward transaction is the first transaction in a block
	private CTransaction mGetRewardTransaction( String alias )
	{
		float[] reward = { CConfiguration.rewardValue };
		byte[][] rewardKeyHases = { CCryptoSunrsasign.getKeyHashFromAlias( alias ) };
		
		return new CTransaction( reward, null, null, null, rewardKeyHases, true );
	}
	
	private CBlock mCreateBlock( CTransaction rewardTx ) throws UnsupportedEncodingException, NoSuchAlgorithmException
	{
		Vector< CTransaction > txList = new Vector<>();
		Vector< byte[] > txHashes = new Vector<>();
		CBlock block = null;
		
		String initial = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
		byte[] headerHash = CCryptoSunrsasign.mMessageDigest( 512, initial.getBytes( "UTF-8" ));

		// add reward tx hash in front of list
		txHashes.addElement( rewardTx.mGetTransactionDigest() );
		txList.addElement( rewardTx );

		// this is the  place where first block is created
		block = new CBlock( headerHash, txHashes, txList.toArray( new CTransaction[ txList.size() ] ) );
		
		fBC.mAddBlock( block );
		return block;
	}
	
	private CGenesys()
	{
		fBC = CBlockChain.mGetInstance();
		fWD = CWalletData.mGetInstance();
		
		String alias = "firstTransaction";			
		CTransaction rewardTx = mGetRewardTransaction( alias );		
		
		try{
			CBlock block = mCreateBlock( rewardTx );
			byte[] streamedBlock = CHelper.mGetByteFromSerial( block );
			CHelper.mPrintByteArray( streamedBlock );
			//System.out.println( Arrays.toString( streamedBlock ) );
			// add first transaction to this node's credit
			CTuple firstCredit = new CTuple( rewardTx.mGetTransactionDigest(), 0, alias, CConfiguration.rewardValue );
			
			fWD.mAddConfirmedTransaction( firstCredit );
			fWD.mSetCredit( CConfiguration.rewardValue );	
		}catch( Exception e ){
			e.printStackTrace();
		}		
	}
	
	static public void main( String[] args )
	{
		//todo: add genesys bloc into the code
		@SuppressWarnings("unused")
		CGenesys gen = new CGenesys();		
	}
}
