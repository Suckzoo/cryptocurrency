import java.util.ArrayList;
import java.util.Collection;

public class MaxFeeTxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;

    public MaxFeeTxHandler(UTXOPool utxoPool) {
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
    public static boolean isValidTxOnPool(UTXOPool currentPool, Transaction tx) {
        UTXOPool uniqueUTXOs = new UTXOPool();
        double totalInput = 0, totalOutput = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = currentPool.getTxOutput(utxo);
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

    public boolean isValidTx(Transaction tx) {
        return isValidTxOnPool(this.utxoPool, tx);
    }
    /**
     * Calculates fee of a transaction
     */
    public double calculateTxFee(Transaction tx) {
        double totalInput = 0, totalOutput = 0;
        if (isValidTx(tx)) {
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                Transaction.Output output = this.utxoPool.getTxOutput(utxo);
                totalInput += output.value;
            }
        }
        for (Transaction.Output output : tx.getOutputs()) {
            totalOutput += output.value;
        }
        return totalInput - totalOutput;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    private class FeeMaximizer {
        private UTXOPool utxoPool;
        private Transaction[] txs;
        private ArrayList<Transaction> acceptedTxs, currentTxs;
        private double maxFee = 0;

        FeeMaximizer(UTXOPool utxoPool, Transaction[] possibleTxs) {
            this.utxoPool = new UTXOPool(utxoPool);
            this.txs = possibleTxs;
            this.acceptedTxs = new ArrayList<>();
            this.currentTxs = new ArrayList<>();
            this.maximizeFee(0, 0);
        }

        private void maximizeFee(int depth, double currentFee) {
            if (depth == txs.length) {
                if (currentFee > maxFee) {
                    acceptedTxs = new ArrayList<>(currentTxs);
                    maxFee = currentFee;
                }
                return;
            }
            // NOT TO USE THIS TX
            maximizeFee(depth + 1, currentFee);
            // TO USE THIS TX
            Transaction tx = this.txs[depth];
            UTXOPool restorePool = new UTXOPool();
            if (isValidTxOnPool(this.utxoPool, tx)) {
                double txFee = 0;
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    Transaction.Output output = this.utxoPool.getTxOutput(utxo);
                    txFee += output.value;
                    this.utxoPool.removeUTXO(utxo);
                    restorePool.addUTXO(utxo, output);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    Transaction.Output txOut = tx.getOutput(i);
                    txFee -= txOut.value;
                    this.utxoPool.addUTXO(utxo, txOut);
                }
                currentTxs.add(tx);

                maximizeFee(depth + 1, currentFee + txFee);

                for (int i = 0; i < tx.numOutputs(); i++) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    this.utxoPool.removeUTXO(utxo);
                }
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    Transaction.Output output = restorePool.getTxOutput(utxo);
                    this.utxoPool.addUTXO(utxo, output);
                }
                currentTxs.remove(currentTxs.size() - 1);
            }
        }
        
        public ArrayList<Transaction> getAcceptedTxs() {
            return acceptedTxs;
        }
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        FeeMaximizer maximizer = new FeeMaximizer(this.utxoPool, possibleTxs);
        ArrayList<Transaction> acceptedTxs = maximizer.getAcceptedTxs();
        for (Transaction tx : acceptedTxs) {
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                this.utxoPool.removeUTXO(utxo);
            }
            for (int i = 0; i < tx.numOutputs(); i++) {
                UTXO utxo = new UTXO(tx.getHash(), i);
                Transaction.Output txOut = tx.getOutput(i);
                this.utxoPool.addUTXO(utxo, txOut);
            }
        }
        Transaction[] acceptedTxArray = new Transaction[acceptedTxs.size()];
        acceptedTxArray = acceptedTxs.toArray(acceptedTxArray);
        return acceptedTxArray;
    }

}
