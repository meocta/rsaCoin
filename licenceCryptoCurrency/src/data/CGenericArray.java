package data;

import java.util.Vector;

public class CGenericArray< Type >
{
	private Vector< Type > fList;

	synchronized public void mAddElement( Type elem )
	{
		if( null == fList ){
			fList = new Vector<Type>();
		}
		fList.addElement( elem );
	}

	synchronized public Vector< Type > mGetArray()
	{
		Vector< Type > list = fList;
		fList = null;
		return list;
	}
}
