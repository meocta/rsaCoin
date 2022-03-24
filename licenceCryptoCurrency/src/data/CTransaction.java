package data;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cryptography.CCryptoSunrsasign;
import cryptography.CHelper;
/* a transaction has a list of inputs, which are outputs to other unspent transactions,
 * a timestamp,
 * a list of signed transaction hashes corresponding to all the input transactions,
 * a list of outputs, consisting of a value and a public key hash(256)
 */
public class CTransaction extends CSerializableSuper{
	private static final long	serialVersionUID	= CSerializableSuper.serialVersionUID;
	private static final int	algorithmSize		= 512;
	private static final int	transactionHashSize = 512;
	private static final float	minTotalOutput 		= 0.01F;
	private static final int	insAndOutsLimit 	= 5;
	
	/* the transaction proper */
	private CTransactionSignable	fTransaction	= null;	
	/*list of signatures for all the input transactions, 
	* signed using private keys corresponding to pub keys of the consumed outputs
	* */ 
	private CTransactionSignature[]	fSignatures		= null;
	
	//todo: create a class for inputs and a class for outputs, change array of array to vector of array
	/*
	 * creates a transaction object
	 * inputs:
	 * values - array with the "money" value corresponding to each output
	 * keysHashes - array with hashes of the public keys used in the outputs 
	 * aliases - array of addresses corresponding to each input
	 * outputs - array with the output indexes for this transaction
	 * transactionsHashes - array with the hashes of transactions with the outputs consumed by this transaction's inputs
	 */
	public CTransaction( float[] values, 
						String[] aliases, 
						byte[][] transactionsHashes,
						int[] outputs,
						byte[][] keysHashes,
						boolean isReward ){
		fTransaction = new CTransactionSignable( values, keysHashes, aliases, transactionsHashes, outputs, isReward );
		if( ! isReward ){
			//initialize fSignatures
			mSignTransaction( aliases );
		}			
	}
	
	public Map< byte[], Integer > mGetInputs()
	{
		Map< byte[], Integer > inputs = new HashMap<>();
		for( int i = 0; i < fTransaction.mInputsNumber(); i++ ){
			CTransactionInput input = fTransaction.mGetInputAtIndex( i );
			inputs.put( input.getTransactionHash(), input.getOutputIndex() );
		}
		return inputs;
	}
	
	public int mGetOutputsNumber()
	{
		return fTransaction.mOutputsNumber();
	}
	
	/*
	 * overall transaction verification
	 */
	public boolean mVerifyTransaction()
	{
		//todo: verify if input hash key matches to public key
		if( mVerifyInsAndOutsNumbers() &&
			mGetTotalOutputValue() > minTotalOutput &&
			mVerifySignatures() &&
			mVerifyTimestamp() )
		{
			return true;
		}		
		return false;
	}
	
	/*
	 * returns the total value of the outputs
	 */
	public float mGetTotalOutputValue()
	{
		float sum = 0;
		for( int outputIndex=0; outputIndex < fTransaction.mOutputsNumber() ; outputIndex++ ){
			sum += fTransaction.mGetOutputAtIndex( outputIndex ).getValue();
		}		
		return sum;
	}
	
	/*
	 * returns the SHA512 hash of fTransaction
	 */
	public byte[] mGetTransactionDigest()
	{
		byte[] fTransactionHash = null;
		try{
			fTransactionHash = CCryptoSunrsasign.mMessageDigest( transactionHashSize, CHelper.mGetByteFromSerial( fTransaction ) );
		} catch( NoSuchAlgorithmException e ){
			e.printStackTrace();
		}		
		return fTransactionHash;
	}
	
	/*
	 * returns output number paying val to keyhash in this transaction
	 * -1 if the key is not present
	 */
	public int mKeyHashPayedValue( byte[] keyHash, float val )
	{
		for( int i = 0; i < fTransaction.mOutputsNumber(); i++ ){
			CTransactionOutput out = fTransaction.mGetOutputAtIndex( i );
			if( Arrays.equals( out.getPublicKeyHash(), keyHash ) && ( out.getValue() == val ) ){
				return i;
			}
		}		
		return -1;
	}
	
	public boolean mEquals( CTransaction tx )
	{	
		byte[] thisTx = CHelper.mGetByteFromSerial( this );
		byte[] comparedTx = CHelper.mGetByteFromSerial( tx );
		return Arrays.equals( thisTx, comparedTx );
	}
	
	/* private members */
	
	/*
	 * checks whether or not the number of inputs and outputs is within the limits
	 */
	private boolean mVerifyInsAndOutsNumbers(){
		boolean status = false;
		int inputs =  fTransaction.mInputsNumber();
		int outputs = fTransaction.mOutputsNumber();
		
		if( inputs > 0 && outputs > 0 && inputs < insAndOutsLimit && outputs < insAndOutsLimit ){
			status = true;
		} else {
			System.out.println( " inputs:" + inputs + ", outputs:" + outputs + "numbers not within limit " );
		}		
		return status;
	}
	
	/*
	 * checks if timestamp is not more than one hour in the future of a day old
	 */
	private boolean mVerifyTimestamp(){		
		long currentTime = System.currentTimeMillis() * 1000;		
		if( ( fTransaction.mGetTimestamp() < currentTime + 3600 ) &&
			( fTransaction.mGetTimestamp() > currentTime - 86400 ) )
		{
			return true;
		} else {
			System.out.println( "timestamp not in acceptable range: " 
								+ fTransaction.mGetTimestamp()
								+ ", current time: "
								+ currentTime );
		}
		return false;
	}
	
	/*
	 * verifies that the signed transactions are valid
	 */	
	private boolean mVerifySignatures(){
		//get the hash SHA512 of the inputs and outputs of the transaction
		byte[] hashedTransaction = mGetTransactionDigest();
		boolean status = false;		
		for( int index = 0; index < fTransaction.mInputsNumber(); index++){
			status = CCryptoSunrsasign.mVerifyRSADS( 
						hashedTransaction, 
						fSignatures[index].getSignature(), 
						fTransaction.mGetInputAtIndex(index).getPublicKey(), 
						algorithmSize);			
			if( status == false ){
				System.out.println( " verification failed at transaction input: " + (index+1) );
				break;
			}
		}		
		return status;
	}

	/*
	 * signs the transaction, by signing fTransaction field for each input
	 */
	private void mSignTransaction(String[] aliases){
		if(aliases.length != fTransaction.mInputsNumber()){
			System.out.println(  " number of keys must equal the number of input transactions " );
		}		
		fSignatures = new CTransactionSignature[aliases.length];
		//get the hash SHA512 of the inputs and outputs of the transaction
		byte[] hashedTransaction = mGetTransactionDigest();
		int currentInput = 0;
		for(String alias : aliases){
			//sign the hash of the transaction using RSA with SHA384
			byte[] signedTransaction = CCryptoSunrsasign.mSignRSADS(hashedTransaction, alias, algorithmSize );
			fSignatures[currentInput].setSignature( signedTransaction );
			++currentInput;
		}		
	}	
}
