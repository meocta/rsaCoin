package states;

public enum EBlockChainState
{
	// state undefined
	eUnknown,
	//when a node is first started will have an empty blockchain
	eEmpty,
	//when a node is downloading the blockchain or restarts after some off time
	eHalf,
	//after the node has finished downloading the blockchain and is fully functional
	eFull
}
