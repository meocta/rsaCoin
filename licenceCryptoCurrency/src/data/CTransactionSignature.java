package data;

public class CTransactionSignature extends CSerializableSuper{
	private static final long serialVersionUID = CSerializableSuper.serialVersionUID;	
	private byte[] fSignature = null;
	
	public byte[] getSignature(){
		return fSignature;
	}
	
	public void setSignature( byte[] signature ){
		fSignature = signature;
	}
}
