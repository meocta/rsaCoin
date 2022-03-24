package data;

class CTransactionOutput extends CSerializableSuper{
	private static final long serialVersionUID = CSerializableSuper.serialVersionUID;	
	float fValue;
	byte[] fPublicKeyHash; //SHA256 
	
	public CTransactionOutput( float val, byte[] keyHash){
		fValue = val;		
		fPublicKeyHash = keyHash;
	}
	
	public float getValue(){
		return fValue;
	}
	
	public byte[] getPublicKeyHash(){
		return fPublicKeyHash;
	}
}
