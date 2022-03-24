package data;

import cryptography.CCryptoSunrsasign;

public class CTuple extends CSerializableSuper
{
	private static final long serialVersionUID = CSerializableSuper.serialVersionUID;

	private byte[] fTransactionHash = null;
	private int fOutput = -1;
	private String fAlias = "";
	private float fValue = 0;
	
	public CTuple( byte[] txHash, int out, String alias, float val )
	{
		fTransactionHash = txHash;
		fOutput = out;
		fAlias = alias;
		fValue = val;
	}
	
	public float mGetValue()
	{
		return fValue;
	}
	
	public byte[] mGetHashKey()
	{
		return CCryptoSunrsasign.getKeyHashFromAlias( fAlias );
	}
	
	public String mGetAlias()
	{
		return fAlias;
	}
	
	public int mGetOutputNumber()
	{
		return fOutput;
	}
	
	public byte[] mGetTxHash()
	{
		return fTransactionHash;
	}	
}
