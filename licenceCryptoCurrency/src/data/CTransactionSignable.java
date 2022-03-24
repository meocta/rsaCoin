package data;

class CTransactionSignable extends CSerializableSuper{
	private static final long serialVersionUID = CSerializableSuper.serialVersionUID;	
	private CTransactionInput [] fInputs = null;
	private CTransactionOutput [] fOutputs = null;
	//creation time in seconds since Epoch
	private long fTimeStamp;
	
	public CTransactionSignable( float[] values, byte[][] keysHashes, String[] aliases, byte[][] transactionsHashes, int[] outputs, boolean isReward ){
		if( !isReward ){
			fInputs = new CTransactionInput[ aliases.length ];
			
			//iterate through number of inputs
			for( int i = 0; i<aliases.length; i++ ){				
				fInputs[ i ] = new CTransactionInput( transactionsHashes[ i ], outputs[ i ], aliases[ i ] );
			}
		}
		fOutputs = new CTransactionOutput[ values.length ];
		//iterate through number of outputs
		for( int j = 0; j < values.length; j++ ){
			fOutputs[ j ] = new CTransactionOutput( values[ j ], keysHashes[ j ] );
		}		
		fTimeStamp = System.currentTimeMillis() * 1000;
	}
	
	public CTransactionOutput mGetOutputAtIndex( int index ){
		return fOutputs[ index ];
	}
	
	public CTransactionInput mGetInputAtIndex( int index ){
		return fInputs[index];
	}

	public int mInputsNumber(){
		return fInputs.length;
	}
	
	public int mOutputsNumber(){
		return fOutputs.length;
	}
	
	public long mGetTimestamp(){
		return fTimeStamp;
	}
}
