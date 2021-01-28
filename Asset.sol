pragma solidity ^0.4.24;
pragma experimental ABIEncoderV2;

import "./Table.sol";

contract Asset {
    // event
    event RegisterEvent(int256 ret, string account, int256 asset_value);
    event TransferEvent(int256 ret, string from_account, string to_account, int256 amount);
    event AddTransactionEvent(int256 ret, string id, string acc1, string acc2, int256 money);
    event UpdateTransactionEvent(int256 ret, string id, int256 money);
    event SplitTransactionEvent(int256 ret, string old_id, string new_id, string acc, int256 money);

    constructor() public{
        createTable();
    }

    function createTable() private{
        TableFactory tf = TableFactory(0x1001);
        tf.createTable("Organization", "org_name", "org_pass,org_type,org_tolmoney");
        tf.createTable("Receipt", "money_sender", "money_receiver, status, money, start_data,end_data, info");
    }

    function openOrgTable()private returns(Table){
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("Organization");
        return table;
    }
    
    function openRcpTable()private returns(Table){
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("Receipt");
        return table;
    }

    function NameExist(string memory name)  public returns (bool){
        Table table = openOrgTable();
        // 查询
        Entries entries = table.select(name, table.newCondition());
        if (0 == uint256(entries.size())) {
            return -1;
        } else {
            return 1;
        }
    }

    function isEqual(string memory a, string memory b) internal returns (bool) {
        if (bytes(a).length != bytes(b).length) {
            return false;
        }
        for (uint i = 0; i < bytes(a).length; i ++) {
            if(bytes(a)[i] != bytes(b)[i]) {
                return false;
            }
        }
        return true;
    }

    function register(string memory name, string memory pass , string memory  org_type) public returns(uint){
         int256 ret=0;
         int256 money=1000;
         bool type;
        if(isEqual(name,"")  ||  isEqual(pass,"") ||  isEqual(org_type, "")     )
            ret=-2;
        if(NameExist(name)==-1)
            ret=-1;
        else{
            Table table = openOrgTable();
            Entry entry = table.newEntry();
            entry.set("org_name", name);
            entry.set("org_pass", pass);
            if(isEqual(org_type,"1")) {type = true;} 
            else {type = false;}
            entry.set("org_type", type);
            entry.set("org_tolmoney", money);
            int count = table.insert(name, entry);
            if (count==1) {ret = 0;}
            else {ret=-2;}
        }

        emit RegisterEvent(ret, name, pass, org_type);
        return ret;

     }  

     function Receipt_trans(string memory company1, string memory company2, uint money, uint id)  public returns(uint, uint){
        //check if its enough
        uint sum = 0;
        uint ret1,ret2;
        Table table1 = openRcpTable();
        Entries entry1 = table1.select(id, table1.newCondition());
        
        if(isEqual(entry1.getByte32("money_receiver"),company1) && !isEqual(entry1.getByte32("money_receiver"),company2) && (entry1.getInt("status") == 0 || entry1.getInt("status") == 1)){
            if(entry1.getInt("money") == money){
                Entry entry0 = table1.newEntry();
                entry0.set("money_receiver",company2);
                int count1 = table1.update(id, entry0, table1.newCondition());
                sum += money;
                emit Receipt_transEvent(uint sum, uint id,string  company1, string  company2, uint money);
                return (sum, id);
                     
            } 
            else if(entry1.getInt("money") > money){
                int newmoney = entry1[i].getInt("money")-money;
                string memory temp = entry1[i].getByte32("money_sender");
                Entry entry2 = table1.newEntry();
                uint newid = id+1000;
                entry2.set("id", newid);
                entry2.set("money_sender", temp);
                entry2.set("money_receiver", company2);
                entry2.set("status", 0);
                entry2.set("money", newmoney);
                entry2.set("start_data", entry1.getInt("start_data"));
                entry2.set("end_data", entry1.getInt("end_data"));
                entry2.set("info", entry1.getInt("info"));
                int count2 = table.insert(newid, entry2);
                sum += money;
                emit Receipt_transEvent(uint sum, uint newid,string  company1, string  company2, uint money);
                return (sum, newid);
            } 
            else {
                uint newid = 9999999;
                emit Receipt_transEvent(uint sum, uint newid,string  company1, string  company2, uint money);
                return (sum, newid);
            }
        }
    }

    function select(string account) public constant returns(int256, int256) {
        Table table = openRecTable();
        Entries entries = table.select(account, table.newCondition());
        int256 asset_value = 0;
        if (0 == uint256(entries.size())) {
            return (-1, asset_value);
        } else {
            Entry entry = entries.get(0);
            return (0, int256(entry.getInt("asset_value")));
        }
    }
    
    function buyGoods(string memory from_account, string memory to_account, uint amount)  public returns(uint){
        int ret_code = 0;
        int256 ret = 0;
        int256 from_asset_value = 0;
        int256 to_asset_value = 0;
        
        (ret, from_asset_value) = select(from_account);
        if(ret != 0) {
            ret_code = -1;
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;

        }
        (ret, to_asset_value) = select(to_account);
        if(ret != 0) {
            ret_code = -2;
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;
        }

        if(from_asset_value < amount) {
            ret_code = -3;
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;
        } 

        if (to_asset_value + amount < to_asset_value) {
            ret_code = -4;
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;
        }

        Table table = openAssetTable();

        Entry entry0 = table.newEntry();
        entry0.set("org_name", from_account);
        entry0.set("org_tolmoney", int256(from_asset_value - amount));
        int count = table.update(from_account, entry0, table.newCondition());
        if(count != 1) {
            ret_code = -5;
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;
        }

        Entry entry1 = table.newEntry();
        entry1.set("org_name", to_account);
        entry1.set("org_tolmoney", int256(to_asset_value + amount));
        table.update(to_account, entry1, table.newCondition());

        emit TransferEvent(ret_code, from_account, to_account, amount);

        return ret_code;
    }

    function payReceipt(string memory company)  public returns(uint, uint){
        
    }


function updateTransaction(string id, int256 money) public returns(int256, string[]){
        int256 ret_code = 0;
        bytes32[] memory str_list = new bytes32[](2);
        int256[] memory int_list = new int256[](3);
        string[] memory acc_list = new string[](2);
        (int_list, str_list) = select_transaction(id);
        acc_list[0] = byte32ToString(str_list[0]);
        acc_list[1] = byte32ToString(str_list[1]);

        if(int_list[0] == 0) {
            if(int_list[2] < money){
                ret_code = -2;
                emit UpdateTransactionEvent(ret_code, id, money);
                return (ret_code, acc_list);
            }
            Table table = openTransactionTable();
            Entry entry0 = table.newEntry();
            entry0.set("id", id);
            entry0.set("acc1", byte32ToString(str_list[0]));
            entry0.set("acc2", byte32ToString(str_list[1]));
            entry0.set("money", int_list[1]);
            entry0.set("status", (int_list[2] - money));
            int count = table.update(id, entry0, table.newCondition());
            if(count != 1) {
                ret_code = -3;
                emit UpdateTransactionEvent(ret_code, id, money);
                return (ret_code,acc_list);
            }

            int256 temp = transfer(byte32ToString(str_list[0]),byte32ToString(str_list[1]),money);
            if(temp != 0){
                ret_code = -4 * 10 + temp;
                emit UpdateTransactionEvent(ret_code, id, money);
                return (ret_code,acc_list);
            }

            ret_code = 0;
      
        } else { 
            ret_code = -1;
        }
        emit UpdateTransactionEvent(ret_code, id, money);

        return (ret_code,acc_list);
    }


    function byte32ToString(bytes32 x) public constant returns (string) {
       
       bytes memory bytesString = new bytes(32);
        uint charCount = 0;
        for (uint j = 0; j < 32; j++) {
            byte char = byte(bytes32(uint(x) * 2 ** (8 * j)));
            if (char != 0) {
                bytesString[charCount] = char;
                charCount++;
            }
        }
        bytes memory bytesStringTrimmed = new bytes(charCount);
        for (j = 0; j < charCount; j++) {
            bytesStringTrimmed[j] = bytesString[j];
        }
        return string(bytesStringTrimmed);
   }
}