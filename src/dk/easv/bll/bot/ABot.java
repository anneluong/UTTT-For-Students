package dk.easv.bll.bot;

import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 *
 * @author annem
 */
public class ABot implements IBot {

    private UCT uct;
    private Random rand;
    private int botPlayerNo;
    private static final int SCORE = 1;

    public ABot() {
        uct = new UCT();
        rand = new Random();
    }

    @Override
    public String getBotName() {
        return "ABot";
    }

    @Override
    public IMove doMove(IGameState state) {
        //Algorithm run time. 
        long time = System.currentTimeMillis() + state.getTimePerMove();

        //Create root node.
        Node root = new Node(state);

        //Set player number of this bot. 
        botPlayerNo = (state.getMoveNumber() + 1) % 2;

        while (System.currentTimeMillis() < time) {
            //Selection.
            Node promisingNode = selection(root);
            //Expansion.
            if (promisingNode.isLeaf() && !promisingNode.isTerminal()) { //
                expansion(promisingNode);
                root.isLeaf(); //change to not leaf.
            }
            //Simulation.
            Node nodeToExplore = promisingNode;
            if (promisingNode.getChildrenArray().size() > 0) {
                nodeToExplore = promisingNode.getRandomChild();
            }
            int result = simulation(nodeToExplore);
            //Backpropagation.
            backPropagation(nodeToExplore, result);
        }
        //Need to do a move.
        //Node winnerNode = rootNode.getChildWithMaxScore();

        return null;
    }

    /**
     * Selection, where a leaf node is recommended.
     *
     * @param root
     * @return
     */
    private Node selection(Node root) {
        //Return the child node highest UCT. Since 
        Node node = root;
        while (node.isLeaf() && !node.isTerminal()) {
            //while (node.getChildrenArray().size() > 0) {
            node = uct.highestUCTNode(node, root);
        }
        return node;
    }

    /**
     * Expansion of selected leaf node.
     *
     * @param node
     */
    private void expansion(Node node) {
        //Create child nodes for all available moves. Connect to each to its parent node.
        //Add a move to the child node.
        List<IMove> availMoves = node.getState().getField().getAvailableMoves();

        for (IMove availMove : availMoves) {
            Node child = new Node(node.getState());
            child.setParent(node);
            node.getChildrenArray().add(child);

            //Make a move on children, so each child have a different state.
            makeMove(child.getState(), availMove);
        }
    }

    /**
     * Random simulation of a random child node until terminal state is reached.
     *
     * @param node
     * @return Result of simulation represented by the player number.
     */
    private int simulation(Node node) {
        Node tempNode = new Node(node);
        IGameState tempState = tempNode.getState();
        //If bot loses.
        if (tempNode.isTerminal() && (tempState.getMoveNumber() + 1) % 2 != botPlayerNo) {
            tempNode.getParent().setScore(Integer.MIN_VALUE);
            return 3 - botPlayerNo;
        }
        //While the game is ongoing.
        while (!tempNode.isTerminal()) {
            randomizedPlay(tempState);
        }
        return (tempState.getMoveNumber() + 1) % 2;
    }

    private void randomizedPlay(IGameState state) {
        //Taken from RandomBot and adjusted.
        List<IMove> moves = state.getField().getAvailableMoves();

        if (moves.size() > 0) {
            /* get random move from available moves */
            IMove randomMove = moves.get(rand.nextInt(moves.size()));
            makeMove(state, randomMove);
        }
    }

    /**
     *
     * @param node
     * @param playerNo
     */
    private void backPropagation(Node node, int playerNo) {
        //Update the simulation results in the tree.        
        Node tempNode = node;
        while (tempNode != null) {
            tempNode.incrementVisitCount();
            if ((tempNode.getState().getMoveNumber() + 1) % 2 == playerNo) {
                tempNode.increaseScore(SCORE);
            }
            tempNode = tempNode.getParent();
        }
    }

    private void makeMove(IGameState state, IMove move) {

    }

    private void updateMicroBoard() {

    }

    private void updateMacroBoard() {

    }

    private boolean isWin() {
        return false;
    }

    private boolean isTie() {
        return false;
    }

    private class Node {

        private IGameState state;
        private Node parent;
        private int score;
        private int visitCount;
        private List<Node> childrenArray;
        private boolean leaf;
        private boolean terminal;

        /**
         * Constructor for root node.
         *
         * @param state
         */
        public Node(IGameState state) {
            this.state = new GameState();

            //Add a microboard to node with current state. 
            String[][] board = new String[9][9];
            for (int i = 0; i < board.length; i++) {
                for (int k = 0; k < board[i].length; k++) {
                    board[i][k] = state.getField().getBoard()[i][k];
                }
            }
            this.state.getField().setBoard(board);

            //Add a macroboard to node with current state.
            String[][] macroBoard = new String[3][3];
            for (int i = 0; i < macroBoard.length; i++) {
                for (int k = 0; k < macroBoard[i].length; k++) {
                    macroBoard[i][k] = state.getField().getMacroboard()[i][k];
                }
            }
            this.state.getField().setMacroboard(macroBoard);

            //Copy move number.
            this.state.setMoveNumber(state.getMoveNumber());

            //Copy round number.
            this.state.setRoundNumber(state.getRoundNumber());

            //Create empty list for children nodes.
            this.childrenArray = new ArrayList<>();

            //Sets initial values.
            this.score = 0;
            this.visitCount = 0;
            this.leaf = true;
            this.terminal = false;
        }

        /**
         * Constructor for a child node.
         *
         * @param node
         */
        public Node(Node node) {
            //Copy current game state from parent node.
            this(node.getState());

            //Create parent-child connection.
            //Note: At the first iteration, root has no parent.
            if (node.getParent() != null) {
                this.parent = node.getParent();
            }

            //Copy children.
            //this.childrenArray = new ArrayList<>();
            List<Node> children = node.getChildrenArray();
            for (Node child : children) {
                this.childrenArray.add(new Node(child));
            }

            this.score = node.getScore();
            this.visitCount = node.getVisitCount();
        }

        public IGameState getState() {
            return state;
        }

        public void setState(IGameState state) {
            this.state = state;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public List<Node> getChildrenArray() {
            return childrenArray;
        }

        public void setChildrenArray(List<Node> childrenArray) {
            this.childrenArray = childrenArray;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public void increaseScore(int score) {
            this.score += score;
        }

        public int getVisitCount() {
            return visitCount;
        }

        public void setVisitCount(int visitCount) {
            this.visitCount = visitCount;
        }

        public void incrementVisitCount() {
            this.visitCount++;
        }

        public Node getRandomChild() {
            return childrenArray.get(rand.nextInt(childrenArray.size()));
        }

        public boolean isLeaf() {
            return leaf;
        }

        public void setLeaf(boolean leaf) {
            this.leaf = leaf;
        }

        public boolean isTerminal() {
            return terminal;
        }

        public void setTerminal(boolean terminal) {
            this.terminal = terminal;
        }
    }

    /**
     * UCT (Upper Confidence Bound 1 for trees).
     */
    private class UCT {

        public double uctVal(double nodeScore, int nodeVisitCount, int rootVisit) {
            if (nodeVisitCount == 0) {
                return Double.POSITIVE_INFINITY;
            }
            return (nodeScore / (double) nodeVisitCount) + Math.sqrt(2) * (Math.sqrt(Math.log(rootVisit) / (double) nodeVisitCount));
        }

        public Node highestUCTNode(Node node, Node rootNode) {
            return Collections.max(node.getChildrenArray(), Comparator.comparing(c -> uctVal(c.getScore(), c.getVisitCount(), rootNode.getVisitCount())));
        }
    }

}
