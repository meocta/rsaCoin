package data;

public class CCreditAddress
{
		private String fAlias = null;
		private float fValue = 0;
		
		public CCreditAddress( String alias, float val )
		{
			fAlias = alias;
			fValue = val;
		}
		
		public String mGetAlias()
		{
			return fAlias;
		}
		
		public float mGetValue()
		{
			return fValue;
		}
}
