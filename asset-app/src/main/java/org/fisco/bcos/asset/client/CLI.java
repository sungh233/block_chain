package org.fisco.bcos.asset.client;

import java.util.*;
import java.io.*;

public class CLI{

    private Map<String, String> map;
    private Scanner scanner;
    private boolean status;
    private String current;
    private String path;

    public CLI(){
        path = "info.txt";
        status = true;
        scanner = new Scanner(System.in);
        map = new HashMap<String, String>();
        readFile();
    }

    public boolean getStatus(){
        return this.status;
    }

    public String getCurrent(){
        return this.current;
    }

    public void setCurrentNull(){
        this.current = null;
    }

    public Map<String, String> getMap(){
        return this.map;
    }

    public void readFile(){
        try{
            FileReader fd = new FileReader(path);
            BufferedReader br = new BufferedReader(fd);
            String s1 = null;
            while((s1 = br.readLine()) != null) {
                String[] temp = s1.split("  ");
                map.put(temp[0],temp[1]);
            }
           br.close();
           fd.close();
        } catch (IOException e) {
            System.out.println("Error:" + e.getMessage());
        }
    }

	public void writeToFile()
	{
		try{
            File file = new File(path);
            FileWriter fw = new FileWriter(file,false);
            for (String key : map.keySet()) {
                String temp = key+"  "+map.get(key);
                fw.write(temp+"\n");
            }

            fw.flush();
            fw.close();    

        } catch (IOException e) {
            System.out.println("Error:" + e.getMessage());
        }
	}

    public boolean login()
    {
        int choice;
        String account, password, again;
        Console console = System.console();
        System.out.println("\n-----------------------------------------------------------------------");
        System.out.println("#    # ###### #       ####   ####  #    # ###### ");
        System.out.println("#    # #      #      #    # #    # ##  ## #");
        System.out.println("#    # #####  #      #      #    # # ## # ##### ");
        System.out.println("# ## # #      #      #      #    # #    # # ");
        System.out.println("##  ## #      #      #    # #    # #    # #");
        System.out.println("#    # ###### ######  ####   ####  #    # ######");
        System.out.println("-----------------------------------------------------------------------\n");
        System.out.println("Please enter number to select function");
        System.out.print("------------------------------------------------------------------------\n|      0:LOG IN    |        1:REGISTER       |        2:QUIT           |\n------------------------------------------------------------------------\n");
        if (scanner.hasNextInt()){
            choice = scanner.nextInt();
            switch(choice){

                case 0:
                    account = (String)scanner.nextLine();
                    System.out.print("------LOG IN------\nPlease enter your ID: ");
                    account = (String)scanner.nextLine();
                    System.out.print("Please enter your password:");
                    password = new String(console.readPassword());
                    if (map.get(account)!=null && map.get(account).compareTo(password) == 0) {
                        current = account;
                        System.out.print("Log in successfully! Wait for key...");
                        again = (String)scanner.nextLine();
                        return true;
                    } else {
                        System.out.print("This account doesnot exist or wrong password! Waiting...");
                        again = (String)scanner.nextLine();
                        return false;
                    } 

                case 1:
                    account = (String)scanner.nextLine();
                    System.out.print("------REGISTER------\nPlease enter your ID: ");
                    account = (String)scanner.nextLine();
                    System.out.print("Please enter your password:");
                    password = new String(console.readPassword());
                    System.out.print("Please enter again:");
                    again = new String(console.readPassword());
                    if (password.compareTo(again)==0 && map.get(account)==null) {
                        map.put(account, password);
                        writeToFile();
                        readFile();
                        System.out.print("Register success! Waiting...");
                        again = (String)scanner.nextLine();
                        return false;
                    } else {
                        System.out.print("Register failed! Waiting...");
                        again = (String)scanner.nextLine();
                        return false;
                    }

                case 2:
                    this.status = false;
                    return false;

                default:
                    System.out.print("Invalid input! Waiting...");
                    again = (String)scanner.nextLine();
                    return false;
            }
        }
        else {
            System.out.print("Invalid input! Waiting...");
            return false;
        }
    }

    public void clearFile(){
        for (int i = 0; i < 20; ++i) System.out.print("\n");
    }

    public void msg(){
        System.out.print("Hi! "+current+", please select next action\n");
        System.out.println("-------------------------------\n| 1 | Balance  inquiry        |\n| 2 | Conduct a transaction   |\n| 3 | Financing/Loan from Bank|\n| 4 | IOU split               |\n| 5 | Transfer accounts       | \n| 6 | Query transaction       |\n| 0 | Exit                    |\n-------------------------------\n");

    }

}