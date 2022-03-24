package data;

import states.EObjectType;

/*
 * encapsulates CTransactions of CBlocks to be send over the network
 */
public class CNetworkObject extends CSerializableSuper
{
	private static final long serialVersionUID = CSerializableSuper.serialVersionUID;

	private EObjectType fType = EObjectType.eUnknown;
	private Object fObject = null;
	
	public CNetworkObject( Object obj, EObjectType type )
	{
		fObject = obj;
		fType = type;
	}
	
	public EObjectType mGetType()
	{
		return fType;
	}
	
	public Object mGetObject()
	{
		return fObject;
	}
}
