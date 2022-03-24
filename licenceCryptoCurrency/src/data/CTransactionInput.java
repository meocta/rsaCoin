package data;

import java.security.KeyPair;
import java.security.PublicKey;

import cryptography.CCryptoSunrsasign;

class CTransactionInput extends CSerializableSuper{
	private static final long serialVersionUID = CSerializableSuper.serialVersionUID;
	private byte[] fTransactionHash;
	private int fOutputIndex;
	private PublicKey fPublicKey;
	
	public CTransactionInput( byte[] transactionHash, int outputIndex, String alias ){
		fTransactionHash = transactionHash;
		fOutputIndex = outputIndex;
		KeyPair keys = CCryptoSunrsasign.mGetKeyFromAlias( alias );
		fPublicKey = keys.getPublic();
	}

	public byte[] getTransactionHash(){
		return fTransactionHash;
	}
	
	public int getOutputIndex(){
		return fOutputIndex;
	}
	
	public PublicKey getPublicKey(){
		return fPublicKey;
	}
}
