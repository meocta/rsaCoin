package states;

public enum EConnectionState
{
	// uninitialized
	eUnknown,
	// traffic goes as expected, incoming tx and blks and outgoing tx and blks
	eNormal,
	// the blockchain is downloaded from another node, the other actions are suspended
	eFirst,
	// the list of regular nodes is received from a base node by a regular
	eList
}
