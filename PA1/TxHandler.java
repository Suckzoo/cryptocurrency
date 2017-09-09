import java.util.ArrayList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        UTXOPool uniqueUTXOs = new UTXOPool();
        double totalInput = 0, totalOutput = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = this.utxoPool.getTxOutput(utxo);
            if (output == null) return false;
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) return false;
            if (uniqueUTXOs.contains(utxo)) return false;
            uniqueUTXOs.addUTXO(utxo, output);
            totalInput += output.value;
        }
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) return false;
            totalOutput += output.value;
        }
        return totalOutput <= totalInput;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    this.utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    Transaction.Output txOut = tx.getOutput(i);
                    this.utxoPool.addUTXO(utxo, txOut);
                }
                acceptedTxs.add(tx);
            }
        }
        Transaction[] acceptedTxArray = new Transaction[acceptedTxs.size()];
        acceptedTxArray = acceptedTxs.toArray(acceptedTxArray);
        return acceptedTxArray;
    }

}
