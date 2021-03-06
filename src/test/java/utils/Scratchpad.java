package utils;

import java.util.UUID;

import monero.daemon.model.MoneroNetworkType;
import monero.wallet.MoneroWallet;
import monero.wallet.MoneroWalletJni;
import monero.wallet.model.MoneroWalletConfig;

/**
 * Scratchpad for quick scripting.
 */
public class Scratchpad {

  public static void main(String[] args) {
    
    // initialize daemon, wallet, and direct rpc interface
//    MoneroDaemon daemon = TestUtils.getDaemonRpc();
//    MoneroWalletRpc walletRpc = TestUtils.getWalletRpc();
//    MoneroWalletJni walletJni = TestUtils.getWalletJni();
    
    // -------------------------------- SCRATCHPAD ----------------------------
    
    // create wallet from mnemonic
    MoneroWallet walletJni = MoneroWalletJni.createWallet(new MoneroWalletConfig()
      .setPath("./test_wallets/" + UUID.randomUUID().toString())  // leave blank for in-memory wallet
      .setPassword("abctesting123")
      .setNetworkType(MoneroNetworkType.STAGENET)
      .setServerUri("http://localhost:38081")
      .setServerUsername("superuser")
      .setServerPassword("abctesting123")
      .setMnemonic("biggest duets beware eskimos coexist igloo pamphlet lagoon odometer hounded jukebox enough pride cocoa nylon wolf geometry buzzer vivid federal idols gang semifinal subtly coexist")
      .setRestoreHeight(573800l));
    walletJni.sync(new WalletSyncPrinter());
    System.out.println("WASM wallet daemon height: " + walletJni.getDaemonHeight());
    System.out.println("WASM wallet mnemonic: " + walletJni.getMnemonic());
    
//    walletJni.createTx(new MoneroTxConfig()
//            .setAddress("52FnB7ABUrKJzVQRpbMNrqDFWbcKLjFUq8Rgek7jZEuB6WE2ZggXaTf4FK6H8gQymvSrruHHrEuKhMN3qTMiBYzREKsmRKM")
//            .setAmount(walletJni.getUnlockedBalance(0).divide(new BigInteger("4")).multiply(new BigInteger("3")))
//            .setAccountIndex(0)
//            .setRelay(true));
    
    // MEASURE LAST 30 DAYS
//    int numBlocks = 30 * 24 * 60 / 2;
//    
//    List<MoneroBlockHeader> headers = daemon.getBlockHeadersByRange(daemon.getHeight() - (numBlocks * 2), daemon.getHeight() - (numBlocks * 1));
//    long totalSize = 0;
//    int numOutputs = 0;
//    int numTxs = 0;
//    for (MoneroBlockHeader header : headers) {
//      totalSize += header.getSize();
//      numTxs += header.getNumTxs();
//    }
//    
//    for (MoneroBlock block : daemon.getBlocksByRange(daemon.getHeight() - numBlocks, daemon.getHeight() - 1)) {
//      for (MoneroTx tx : block.getTxs()) {
//        numOutputs += tx.getOutputs().size();
//      }
//    }
//    
//    System.out.println("Number of blocks: " + numBlocks);
//    System.out.println("Num txs: " + numTxs);
//    System.out.println("Num outputs: " + numOutputs);
//    System.out.println("Total size: " + totalSize);
    
//    // TIMING TEST
//    String path = TestMoneroWalletJni.getRandomWalletPath();
//    MoneroWalletJni myWallet = MoneroWalletJni.createWalletFromMnemonic(path, TestUtils.WALLET_JNI_PW, MoneroNetworkType.STAGENET, TestUtils.MNEMONIC, TestUtils.getDaemonRpc().getRpcConnection());
//    myWallet.save();
//    long now = System.currentTimeMillis();;
//    myWallet.addListener(new MoneroWalletListener());
//    myWallet.sync(new WalletSyncPrinter());
//    long newNow = System.currentTimeMillis();
//    System.out.println("Sync took " + (((double) newNow - (double) now) / (double) 1000) + " seconds");
    
//    // generate 20 random stagenet wallets
//    MoneroRpcConnection daemonConnection = new MoneroRpcConnection(TestUtils.DAEMON_RPC_URI, TestUtils.DAEMON_RPC_USERNAME, TestUtils.DAEMON_RPC_PASSWORD);
//    List<String> mnemonics = new ArrayList<String>();
//    List<String> addresses = new ArrayList<String>();
//    for (int i = 0; i < 20; i++) {
//      String temp = UUID.randomUUID().toString();
//      walletJni = new MoneroWalletJni(TestUtils.TEST_WALLETS_DIR + "/" + temp, TestUtils.WALLET_JNI_PW, MoneroNetworkType.STAGENET, daemonConnection, "English");
//      mnemonics.add(walletJni.getMnemonic());
//      addresses.add(walletJni.getPrimaryAddress());
//      ((MoneroWalletJni) walletJni).close();
//    }
//    for (int i = 0; i < 20; i++) {
//      System.out.println(mnemonics.get(i));
//      System.out.println(addresses.get(i));
//    }
  }
}
