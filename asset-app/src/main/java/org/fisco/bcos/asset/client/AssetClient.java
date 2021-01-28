package org.fisco.bcos.asset.client;

import java.math.BigInteger;
import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fisco.bcos.asset.contract.Asset;
import org.fisco.bcos.asset.contract.Asset.RegisterEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.TransferEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.AddTransactionEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.UpdateTransactionEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.SplitTransactionEventEventResponse;
import org.fisco.bcos.asset.client.CLI;
import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tuples.generated.Tuple2;
import org.fisco.bcos.web3j.tx.gas.StaticGasProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class AssetClient {

	static Logger logger = LoggerFactory.getLogger(AssetClient.class);

	private Web3j web3j;

	private Credentials credentials;

	public Web3j getWeb3j() {
		return web3j;
	}

	public void setWeb3j(Web3j web3j) {
		this.web3j = web3j;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public void recordAssetAddr(String address) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		prop.setProperty("address", address);
		final Resource contractResource = new ClassPathResource("contract.properties");
		FileOutputStream fileOutputStream = new FileOutputStream(contractResource.getFile());
		prop.store(fileOutputStream, "contract address");
	}

	public String loadAssetAddr() throws Exception {
		// load Asset contact address from contract.properties
		Properties prop = new Properties();
		final Resource contractResource = new ClassPathResource("contract.properties");
		prop.load(contractResource.getInputStream());

		String contractAddress = prop.getProperty("address");
		if (contractAddress == null || contractAddress.trim().equals("")) {
			throw new Exception(" load Asset contract address failed, please deploy it first. ");
		}
		logger.info(" load Asset address from contract.properties, address is {}", contractAddress);
		return contractAddress;
	}
	
	public void initialize() throws Exception {

		// init the Service
		@SuppressWarnings("resource")
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		Service service = context.getBean(Service.class);
		service.run();

		ChannelEthereumService channelEthereumService = new ChannelEthereumService();
		channelEthereumService.setChannelService(service);
		Web3j web3j = Web3j.build(channelEthereumService, 1);

		// init Credentials
		Credentials credentials = Credentials.create(Keys.createEcKeyPair());

		setCredentials(credentials);
		setWeb3j(web3j);

		logger.debug(" web3j is " + web3j + " ,credentials is " + credentials);
	}

	private static BigInteger gasPrice = new BigInteger("30000000");
	private static BigInteger gasLimit = new BigInteger("30000000");

	public void deployAssetAndRecordAddr() {

		try {
			Asset asset = Asset.deploy(web3j, credentials, new StaticGasProvider(gasPrice, gasLimit)).send();
			System.out.println(" deploy Asset success, contract address is " + asset.getContractAddress());

			recordAssetAddr(asset.getContractAddress());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println(" deploy Asset contract failed, error message is  " + e.getMessage());
		}
	}

	public boolean queryAssetAmount(String assetAccount) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			Tuple2<BigInteger, BigInteger> result = asset.select(assetAccount).send();
			if (result.getValue1().compareTo(new BigInteger("0")) == 0) {
				System.out.printf("----------line of credit----------\n|     money     |       "+result.getValue2()+"     |\n----------------------------------\n");
				return true;
			} else {
				System.out.printf(" %s asset account is not exist \n", assetAccount);
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(" queryAssetAmount exception, error message is {}", e.getMessage());

			System.out.printf(" query asset account failed, error message is %s\n", e.getMessage());
		}
		return false;
	}

	public void queryAssetTransaction(String t_id) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			Tuple2<List<BigInteger>, List<byte[]>> result = asset.select_transaction(t_id).send();
			if (result.getValue1().get(0).compareTo(new BigInteger("0")) == 0) {
				String temp1 = new String(result.getValue2().get(0));
				String temp2 = new String(result.getValue2().get(1));
				System.out.print("----------------------------------------------------------------------------------------------------------------------------\n|     Transaction ID    |        Debtee       |        Debtor           |        Money           |         Current          |\n----------------------------------------------------------------------------------------------------------------------------\n|         "+t_id+"       |          "+temp1+"          |        "+temp2+"              |      "+ result.getValue1().get(1)+"             |        "+result.getValue1().get(2)+"                 |\n----------------------------------------------------------------------------------------------------------------------------\n");
				System.out.printf("Transaction\n ID: " + t_id + ";Debtee: " + temp1 + "; Debtor: " + temp2 + "; Money: " + result.getValue1().get(1) + "; Current: " + result.getValue1().get(2) + "\n");
			} else {
				System.out.printf("Transaction %s is not exist \n", t_id);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(" queryAssetAmount exception, error message is {}", e.getMessage());

			System.out.printf(" query asset account failed, error message is %s\n", e.getMessage());
		}
	}

	public void registerAssetAccount(String assetAccount, BigInteger amount) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.register(assetAccount, amount).send();
			List<RegisterEventEventResponse> response = asset.getRegisterEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" register asset account success => asset: %s, value: %s \n", assetAccount,
							amount);
				} else {
					System.out.printf(" register asset account failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
			System.out.printf(" register asset account failed, error message is %s\n", e.getMessage());
		}
	}

	public void addAssetTransaction(String t_id, String acc1, String acc2, BigInteger money) {
		try {
			String contractAddress = loadAssetAddr();

			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.addTransaction(t_id, acc1, acc2, money).send();
			List<AddTransactionEventEventResponse> response = asset.getAddTransactionEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf("---------- transaction ----------\n|    t_id    |     "+t_id+"     |\n---------------------------------\n|     money     |      "+money+"     |\n---------------------------------\n");
				} else {
					System.out.printf(" Add transaction failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
			System.out.printf(" register asset account failed, error message is %s\n", e.getMessage());
		}
	}

	public void updateAssetTransaction(String t_id, BigInteger money) {
		try {
			String contractAddress = loadAssetAddr();
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.updateTransaction(t_id, money).send();
			List<UpdateTransactionEventEventResponse> response = asset.getUpdateTransactionEventEvents(receipt);
			// Tuple2<BigInteger, List<String>> result = asset.getUpdateTransactionOutput(asset.updateTransaction(t_id, money).send());
			
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" Update transaction success.\n" );
				} else {
					System.out.printf(" Update transaction failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
			System.out.printf(" register asset account failed, error message is %s\n", e.getMessage());
		}
	}

	public void transferAsset(String fromAssetAccount, String toAssetAccount, BigInteger amount) {
		try {
			String contractAddress = loadAssetAddr();
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.transfer(fromAssetAccount, toAssetAccount, amount).send();
			List<TransferEventEventResponse> response = asset.getTransferEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" transfer success => from_asset: %s, to_asset: %s, amount: %s \n",
							fromAssetAccount, toAssetAccount, amount);
				} else {
					System.out.printf(" transfer asset account failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
			System.out.printf(" register asset account failed, error message is %s\n", e.getMessage());
		}
	}

	public void splitAssetTransaction(String old_id, String new_id, String acc, BigInteger money) {
		try {
			String contractAddress = loadAssetAddr();
			Asset asset = Asset.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = asset.splitTransaction(old_id, new_id, acc, money).send();
			List<SplitTransactionEventEventResponse> response = asset.getSplitTransactionEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" Split transaction success! \n---------- IOU Split ----------\n|   old bill  id  |      "+old_id+"    | \n-------------------------------\n|   new bill  id  |      "+new_id+"    |\n-------------------------------\n|   to user    |      "+acc+"    |\n-------------------------------\n|    money     |      "+money+"    |\n-------------------------------\n");
					System.out.printf(" Split transaction failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
			System.out.printf(" register asset account failed, error message is %s\n", e.getMessage());
		}
	}


	public static void Usage() {
		System.out.println(" Usage:");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient deploy");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient query account");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient register account value");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient transfer from_account to_account amount");
		System.exit(0);
	}

	public static void main(String[] args) throws Exception {
		Scanner scanner = new Scanner(System.in);
		CLI test = new CLI();
		

		AssetClient client = new AssetClient();
		client.initialize();
		client.deployAssetAndRecordAddr();
		int transaction_id = 150;



		while(test.getStatus() == true) {
			while(test.getStatus() == true && test.login() == false);
			if(test.getStatus()==false) break;
			String x;
			Map<String,String> map = test.getMap();
			for (String key : map.keySet()) {
				if(client.queryAssetAmount(key)==false){
					if(key.compareTo("bank") != 0){
						if(key.compareTo("acc0") == 0){
							x = "10000";
						}
						else{
						x = "1000";
						}
					} else{
						x = "1000000";
					}
					client.registerAssetAccount(key,new BigInteger(x));
				}
			}

			test.clear();
			boolean judge = true;
            while(judge){
                test.msg();
				int choice, int1, int2;
				String str1,str2,str3, str4;
                if(scanner.hasNextInt()){
                    choice = scanner.nextInt();
                    switch(choice){
                        case 0:
							test.setCurrentNull();
							judge = false;
                            break;
				case 1:
              System.out.println("------Balance  inquiry------\n");
              client.queryAssetAmount(platform.getCurrent());
              //System.out.print("Waiting...");
              str4 = (String)scanner.nextLine();

              break;
            case 2:
              System.out.println("------Conduct a transaction------\n");
              str1 = (String)scanner.nextLine();
              System.out.print("Trader Account: ");
              str1 = (String)scanner.nextLine();
              System.out.print("Transaction Amount: ");
              int1 = scanner.nextInt();
              str3 = int1+"";
              str2 = transaction_id +"";
              transaction_id += 1;
              client.addAssetTransaction(str2,str1, platform.getCurrent() ,new BigInteger(str3));
              //System.out.print("Waiting...");
              str4 = (String)scanner.nextLine();

              break;

            case 3:
              System.out.println("------Financing/Loan from Bank------\n");
              str1 = (String)scanner.nextLine();
              System.out.print("Financing/Loan Amount: ");
              int1 = scanner.nextInt();
              str1 = transaction_id +"";
              transaction_id += 1;
              str2 = int1+"";
              client.addAssetTransaction(str1, "bank", platform.getCurrent(),new BigInteger(str2));
              //System.out.print("Waiting...");
              str4 = (String)scanner.nextLine();

              break;

            case 4:
              System.out.println("------IOU Spilt------\n");
              str1 = (String)scanner.nextLine();
              System.out.print("Beneficiary: ");
              str1 = (String)scanner.nextLine();
              System.out.print("Original ID: ");
              str2 = (String)scanner.nextLine();
              System.out.print("Transfer amount: ");
              int1 = scanner.nextInt();
              str3 = transaction_id+"";
              transaction_id+=1;
              str4 = int1 + "";
              client.splitAssetTransaction(str2,str3,str1,new BigInteger(str4));
              //System.out.print("Waiting...");
              str4 = (String)scanner.nextLine();
              break;
            
            case 5:
              System.out.println("------Transfer/Loan Repayment------\n");
              str1 = (String)scanner.nextLine();
              System.out.print("Trade ID: ");
              str1 = (String)scanner.nextLine();
              System.out.print("Transfer Amount: ");
              int1 = scanner.nextInt();
              str2 = transaction_id +"";
              transaction_id += 1;
              str3 = int1+"";
              client.updateAssetTransaction( str1, new BigInteger(str3));
              //System.out.print("Waiting...");
              str4 = (String)scanner.nextLine();

              break;
            
            case 6:
              System.out.print("------Query transaction------\n");
              str1 = (String)scanner.nextLine();
              System.out.print("Original ID: ");
              str1 = (String)scanner.nextLine();
              client.queryAssetTransaction(str1);
             // System.out.print("Waiting...");
              str4 = (String)scanner.nextLine();
            
            default:
              System.out.print("Invalid input! Waiting...");
              str1 = (String)scanner.nextLine();
              break;
          }
        }
      }
    }
    
		platform.clearFile();
    platform.writeToFile();
    System.out.println("\n-----------------------------------------------------------------------");
    System.out.println(" ####   ####   ####  #####  #####  #   # ###### ");
    System.out.println("#    # #    # #    # #    # #    #  # #  #");
    System.out.println("#      #    # #    # #    # #####    #   ##### ");
    System.out.println("#  ### #    # #    # #    # #    #   #   # ");
    System.out.println("#    # #    # #    # #    # #    #   #   #");
    System.out.println(" ####   ####   ####  #####  #####    #   ######");
    System.out.println("-----------------------------------------------------------------------\n");
    System.exit(0);
  }
}
