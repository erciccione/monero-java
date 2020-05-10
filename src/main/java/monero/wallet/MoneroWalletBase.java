/**
 * Copyright (c) 2017-2019 woodser
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package monero.wallet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import common.utils.GenUtils;
import monero.common.MoneroError;
import monero.common.MoneroRpcConnection;
import monero.wallet.model.MoneroAccount;
import monero.wallet.model.MoneroAddressBookEntry;
import monero.wallet.model.MoneroIncomingTransfer;
import monero.wallet.model.MoneroIntegratedAddress;
import monero.wallet.model.MoneroOutgoingTransfer;
import monero.wallet.model.MoneroOutputWallet;
import monero.wallet.model.MoneroTxPriority;
import monero.wallet.model.MoneroTxConfig;
import monero.wallet.model.MoneroSubaddress;
import monero.wallet.model.MoneroSyncResult;
import monero.wallet.model.MoneroTransfer;
import monero.wallet.model.MoneroTransferQuery;
import monero.wallet.model.MoneroTxQuery;
import monero.wallet.model.MoneroTxSet;
import monero.wallet.model.MoneroTxWallet;
import monero.wallet.model.MoneroWalletListenerI;

/**
 * Abstract base implementation of a Monero wallet.
 */
public abstract class MoneroWalletBase implements MoneroWallet {
  
  public void setDaemonConnection(String uri) {
    setDaemonConnection(uri, null, null);
  }
  
  public void setDaemonConnection(String uri, String username, String password) {
    if (uri == null) setDaemonConnection((MoneroRpcConnection) null);
    else setDaemonConnection(new MoneroRpcConnection(uri, username, password));
  }
  
  @Override
  public String getPrimaryAddress() {
    return getAddress(0, 0);
  }
  
  @Override
  public MoneroIntegratedAddress getIntegratedAddress() {
    return getIntegratedAddress(null);
  }
  
  @Override
  public MoneroSyncResult sync() {
    return sync(null, null);
  }
  
  @Override
  public MoneroSyncResult sync(MoneroWalletListenerI listener) {
    return sync(null, listener);
  }
  
  @Override
  public MoneroSyncResult sync(Long startHeight) {
    return sync(startHeight, null);
  }
  
  @Override
  public MoneroSyncResult sync(Long startHeight, MoneroWalletListenerI listener) {
    return sync(startHeight, listener);
  }
  
  @Override
  public List<MoneroAccount> getAccounts() {
    return getAccounts(false, null);
  }
  
  @Override
  public List<MoneroAccount> getAccounts(boolean includeSubaddresses) {
    return getAccounts(includeSubaddresses, null);
  }
  
  @Override
  public List<MoneroAccount> getAccounts(String tag) {
    return getAccounts(false, tag);
  }
  
  @Override
  public MoneroAccount getAccount(int accountIdx) {
    return getAccount(accountIdx, false);
  }
  
  @Override
  public MoneroAccount createAccount() {
    return createAccount(null);
  }
  
  @Override
  public List<MoneroSubaddress> getSubaddresses(int accountIdx) {
    return getSubaddresses(accountIdx, null);
  }
  
  @Override
  public MoneroSubaddress getSubaddress(int accountIdx, int subaddressIdx) {
    List<MoneroSubaddress> subaddresses = getSubaddresses(accountIdx, Arrays.asList(subaddressIdx));
    if (subaddresses.isEmpty()) throw new MoneroError("Subaddress at index " + subaddressIdx + " is not initialized");
    GenUtils.assertEquals("Only 1 subaddress should be returned", 1, subaddresses.size());
    return subaddresses.get(0);
  }
  
  @Override
  public MoneroSubaddress createSubaddress(int accountIdx) {
    return createSubaddress(accountIdx,  null);
  }
  
  @Override
  public MoneroTxWallet getTx(String txHash) {
    return getTxs(txHash).get(0);
  }
  
  @Override
  public List<MoneroTxWallet> getTxs() {
    return getTxs(new MoneroTxQuery());
  }
  
  public List<MoneroTxWallet> getTxs(String... txHashes) {
    return getTxs(new MoneroTxQuery().setTxHashes(txHashes));
  }
  
  public List<MoneroTxWallet> getTxs(List<String> txHashes) {
    return getTxs(new MoneroTxQuery().setTxHashes(txHashes));
  }
  
  @Override
  public List<MoneroTransfer> getTransfers() {
    return getTransfers(null);
  }
  
  @Override
  public List<MoneroTransfer> getTransfers(int accountIdx) {
    MoneroTransferQuery query = new MoneroTransferQuery().setAccountIndex(accountIdx);
    return getTransfers(query);
  }
  
  @Override
  public List<MoneroTransfer> getTransfers(int accountIdx, int subaddressIdx) {
    MoneroTransferQuery query = new MoneroTransferQuery().setAccountIndex(accountIdx).setSubaddressIndex(subaddressIdx);
    return getTransfers(query);
  }
  

  @Override
  public List<MoneroIncomingTransfer> getIncomingTransfers() {
    return getIncomingTransfers(null);
  }

  @Override
  public List<MoneroIncomingTransfer> getIncomingTransfers(MoneroTransferQuery query) {
    
    // copy query and set direction
    MoneroTransferQuery _query;
    if (query == null) _query = new MoneroTransferQuery();
    else {
      if (Boolean.FALSE.equals(query.isIncoming())) throw new MoneroError("Transfer query contradicts getting incoming transfers");
      _query = query.copy();
    }
    _query.setIsIncoming(true);
    
    // fetch and cast transfers
    List<MoneroIncomingTransfer> inTransfers = new ArrayList<MoneroIncomingTransfer>();
    for (MoneroTransfer transfer : getTransfers(_query)) {
      inTransfers.add((MoneroIncomingTransfer) transfer);
    }
    return inTransfers;
  }

  @Override
  public List<MoneroOutgoingTransfer> getOutgoingTransfers() {
    return getOutgoingTransfers(null);
  }

  @Override
  public List<MoneroOutgoingTransfer> getOutgoingTransfers(MoneroTransferQuery query) {
    
    // copy query and set direction
    MoneroTransferQuery _query;
    if (query == null) _query = new MoneroTransferQuery();
    else {
      if (Boolean.FALSE.equals(query.isOutgoing())) throw new MoneroError("Transfer query contradicts getting outgoing transfers");
      _query = query.copy();
    }
    _query.setIsOutgoing(true);
    
    // fetch and cast transfers
    List<MoneroOutgoingTransfer> outTransfers = new ArrayList<MoneroOutgoingTransfer>();
    for (MoneroTransfer transfer : getTransfers(_query)) {
      outTransfers.add((MoneroOutgoingTransfer) transfer);
    }
    return outTransfers;
  }
  
  @Override
  public List<MoneroOutputWallet> getOutputs() {
    return getOutputs(null);
  }
  
  @Override
  public MoneroTxSet createTx(MoneroTxConfig config) {
    if (config == null) throw new MoneroError("Send request cannot be null");
    if (Boolean.TRUE.equals(config.getCanSplit())) throw new MoneroError("Cannot request split transactions with createTx() which prevents splitting; use createTxs() instead");
    config = config.copy();
    config.setCanSplit(false);
    return createTxs(config);
  }
  
  @Override
  public MoneroTxSet createTx(int accountIndex, String address, BigInteger sendAmount) {
    return createTx(accountIndex, address, sendAmount, null);
  }
  
  @Override
  public MoneroTxSet createTx(int accountIndex, String address, BigInteger sendAmount, MoneroTxPriority priority) {
    return createTx(new MoneroTxConfig(accountIndex, address, sendAmount, priority));
  }
  
  @Override
  public MoneroTxSet createTxs(MoneroTxConfig config) {
    if (config == null) throw new MoneroError("Send request cannot be null");
    
    // modify request to not relay
    Boolean requestedDoNotRelay = config.getDoNotRelay();
    config.setDoNotRelay(true);
    
    // invoke common method which doesn't relay
    MoneroTxSet txSet = sendTxs(config);
    
    // restore doNotRelay of request and txs
    config.setDoNotRelay(requestedDoNotRelay);
    if (txSet.getTxs() != null) {
      for (MoneroTxWallet tx : txSet.getTxs()) tx.setDoNotRelay(requestedDoNotRelay);
    }
    
    // return results
    return txSet;
  }
  
  @Override
  public String relayTx(String txMetadata) {
    return relayTxs(Arrays.asList(txMetadata)).get(0);
  }
  
  @Override
  public String relayTx(MoneroTxWallet tx) {
    return relayTx(tx.getMetadata());
  }
  
  // TODO: this method is not tested
  @Override
  public List<String> relayTxs(List<MoneroTxWallet> txs) {
    List<String> txHexes = new ArrayList<String>();
    for (MoneroTxWallet tx : txs) txHexes.add(tx.getMetadata());
    return relayTxs(txHexes);
  }
  
  @Override
  public MoneroTxSet sendTx(MoneroTxConfig request) {
    if (request == null) throw new MoneroError("Send request cannot be null");
    if (Boolean.TRUE.equals(request.getCanSplit())) throw new MoneroError("Cannot request split transactions with sendTx() which prevents splitting; use sendTxs() instead");
    request = request.copy();
    request.setCanSplit(false);
    return sendTxs(request);
  }
  
  @Override
  public MoneroTxSet sendTx(int accountIndex, String address, BigInteger sendAmount) {
    return sendTx(accountIndex, address, sendAmount, null);
  }
  
  @Override
  public MoneroTxSet sendTx(int accountIndex, String address, BigInteger sendAmount, MoneroTxPriority priority) {
    return sendTx(new MoneroTxConfig(accountIndex, address, sendAmount, priority));
  }
  
  @Override
  public MoneroTxSet sendTxs(int accountIndex, String address, BigInteger sendAmount) {
    return sendTxs(new MoneroTxConfig(accountIndex, address, sendAmount));
  }
  
  @Override
  public MoneroTxSet sendTxs(int accountIndex, String address, BigInteger sendAmount, MoneroTxPriority priority) {
    return sendTxs(new MoneroTxConfig(accountIndex, address, sendAmount, priority));
  }
  
  @Override
  public MoneroTxSet sweepOutput(String address, String keyImage) {
    return sweepOutput(address, keyImage, null);
  }
  
  @Override
  public MoneroTxSet sweepOutput(String address, String keyImage, MoneroTxPriority priority) {
    MoneroTxConfig config = new MoneroTxConfig(address).setPriority(priority);
    config.setKeyImage(keyImage);
    return sweepOutput(config);
  }
  
  @Override
  public MoneroTxSet sweepSubaddress(int accountIdx, int subaddressIdx, String address) {
    MoneroTxConfig config = new MoneroTxConfig(address);
    config.setAccountIndex(accountIdx);
    config.setSubaddressIndices(subaddressIdx);
    List<MoneroTxSet> txSets = sweepUnlocked(config);
    GenUtils.assertEquals("Only one tx set should be created when sweeping from a subaddress", 1, (int) txSets.size());
    return txSets.get(0);
  }
  
  @Override
  public MoneroTxSet sweepAccount(int accountIdx, String address) {
    MoneroTxConfig config = new MoneroTxConfig(address);
    config.setAccountIndex(accountIdx);
    List<MoneroTxSet> txSets = sweepUnlocked(config);
    GenUtils.assertEquals("Only one tx set should be created when sweeping from an account", 1, (int) txSets.size());
    return txSets.get(0);
  }
  
  @Override
  public List<MoneroTxSet> sweepWallet(String address) {
    return sweepUnlocked(new MoneroTxConfig(address));
  }
  
  @Override
  public MoneroTxSet sweepDust() {
    return sweepDust(false);
  }
  
  @Override
  public String getTxProof(String txHash, String address) {
    return getTxProof(txHash, address, null);
  }
  
  @Override
  public String getSpendProof(String txHash) {
    return getSpendProof(txHash, null);
  }
  
  @Override
  public String getTxNote(String txHash) {
    return getTxNotes(Arrays.asList(txHash)).get(0);
  }
  
  @Override
  public void setTxNote(String txHash, String note) {
    setTxNotes(Arrays.asList(txHash), Arrays.asList(note));
  }
  
  @Override
  public List<MoneroAddressBookEntry> getAddressBookEntries() {
    return getAddressBookEntries(null);
  }
  
  @Override
  public boolean isMultisig() {
    return getMultisigInfo().isMultisig();
  }
  
  @Override
  public void close() {
    close(false); // close without saving
  }
}
