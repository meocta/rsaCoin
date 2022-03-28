package main;

import java.io.File;

public class CConfiguration
{
	//base node ip list
	static final public String[] ipBaseNodesList = {"192.168.0.105"};
	//number of base nodes
	static final public int numberOfBaseNodes = 1;
	//number of allowed regular connections
	static final public int numberOfRegularConnections = 5;
	//server ip 
	static final public String localNodeIP = "192.168.0.105";
	//true if base node, false if regular node
	static final public boolean isBaseNode = true;
	//server port
	static final public int serverPort = 11111;
	//client port
	static final public int clientPort = 11113;
	//maximum length of queue for incoming connections
	static final public int backlog = 20;
	// the value of the first transaction in a block
	static final public float rewardValue = 20;

	// path to the coin's folder
	static final public String coinFolder		= System.getProperty("user.home") + File.separator + "RSAC" ;
	static final public String walletFolder		= CConfiguration.coinFolder + File.separator + "wallet" + File.separator;
	static final public String blockchainFolder = CConfiguration.coinFolder + File.separator + "Blockchain" + File.separator;
	static final public String keyFolder		= CConfiguration.coinFolder + File.separator + "Keys" + File.separator;
}
