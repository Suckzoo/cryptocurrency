import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    final int TRUST_ONLY;

    private Map<Transaction, Integer> vote;

    Set<Transaction> pendingTransactions;

    double p_graph;
    double p_malicious;
    double p_txDistribution;
    int numRounds;
    int currentRound;
    int numFollowees;
    int numBlacklist;
    
    boolean[] followees;
    boolean[] blacklist;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
        this.currentRound = 0;
        TRUST_ONLY = numRounds * 8 / 10;

        this.vote = new HashMap<Transaction, Integer>();
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        this.blacklist = new boolean[followees.length];
        this.numFollowees = 0;
        this.numBlacklist = 0;
        for (int i = 0; i < followees.length; i++) {
            if (followees[i]) this.numFollowees++;
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        this.currentRound++;
        return this.pendingTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        Set<Integer> senders = new HashSet<Integer>();
        for (Candidate candidate : candidates) {
            senders.add(candidate.sender);
        }

        for (int i = 0; i < this.followees.length; i++) {
            if(this.followees[i] && !senders.contains(i)) {
                this.blacklist[i] = true;
                this.numBlacklist++;
            }
        }

        for (Candidate candidate : candidates) {
            if (this.followees[candidate.sender] && !this.blacklist[candidate.sender]) {
                if (currentRound < TRUST_ONLY) {
                    this.pendingTransactions.add(candidate.tx);
                } else {
                    Integer voteCount = this.vote.get(candidate.tx);
                    if (voteCount == null) voteCount = 0;
                    this.vote.put(candidate.tx, voteCount + 1);
                }
            }
        }

        if (currentRound >= TRUST_ONLY) {
            for (Transaction tx : this.vote.keySet()) {
                int voteCount = this.vote.get(tx);
                if (voteCount >= (this.numFollowees * 8 / 10) - this.numBlacklist) {
                    this.pendingTransactions.add(tx);
                }
            }
            this.vote.clear();
        }
    }
}
