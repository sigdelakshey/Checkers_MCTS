import java.util.Random;
import java.util.Stack;

public class player {
    static int playouts = 0;

    static class Node {
        state state;
        Node[] branches;
        Node parent;
        double won = 0;
        double played = 0;
        int depth = 0;

        public boolean gameOver() {
            return state.numLegalMoves == 0;
        }

        public boolean isLeaf() {
            return branches[0] == null;
        }

        public Node(state state, Node parent) {
            this.state = state;
            branches = new Node[state.numLegalMoves];
            this.parent = parent;
            if (parent != null) {
                depth = this.parent.depth + 1;
            }
        }
    }

    static Random random = new Random();
    static int savedBestMoveIndex;

    static void PerformMove(state state, int moveIndex) {
        playerhelper.PerformMove(state.board, state.movelist[moveIndex],
                playerhelper.MoveLength(state.movelist[moveIndex]));
        state.player = state.player % 2 + 1;
        playerhelper.FindLegalMoves(state);
    }

    static void setupASstate(state state, int player, char[][] board) {
        state.player = player;
        playerhelper.memcpy(state.board, board);

        playerhelper.FindLegalMoves(state);
    }

    public static void FindBestMove(int p, char[][] b, char[] bm) {
        MCTSThread fbmt = new MCTSThread(p, b, bm);
        fbmt.start();

        try {
            fbmt.join((long) (playerhelper.SecPerMove * 1000 - 200));
        } catch (InterruptedException e) {
        }
        fbmt.stop = true;
        try {
            fbmt.join();
        } catch (InterruptedException e) {
        }
    }

    private static class MCTSThread extends Thread {
        int player;
        char[][] board;
        char[] bestmove;
        public boolean stop = false;

        public MCTSThread(int p, char[][] b, char[] bm) {
            super();
            player = p;
            board = b;
            bestmove = bm;
        }

        public int playout(state state, int maxDepth, int maxMoves) {
            int myBestMoveIndex = 0;
            if (state.numLegalMoves == 0){
                return -1;
            }
            if (maxMoves == 0)
                return 0;

            myBestMoveIndex = random.nextInt(state.numLegalMoves);

            state nextstate = new state(state);
            PerformMove(nextstate, myBestMoveIndex);
            return -playout(nextstate, maxDepth, maxMoves - 1);
        }

        public void expand(Node curr) {
            for (int i = 0; i < curr.state.numLegalMoves; i++) {
                state nextState = new state(curr.state);
                PerformMove(nextState, i);

                Node newnode = new Node(nextState, curr);
                // System.err.println("Playing as " + nextState.player);
                int temp = playout(nextState, 3, 50);
                // System.err.println("Got score for playout:" + temp);

                if (temp > 0)
                    newnode.won = 1.0;
                else
                    newnode.won = 0;

                newnode.played = 1;
                newnode.parent = curr;
                curr.branches[i] = newnode;
                playouts += 1;
            }

            // Use iterative backpropagation instead of recursive one.
            for (int i = 0; i < curr.state.numLegalMoves; i++) {
                // System.err.println("backpropping.");
                Node currnode = curr.branches[i];
                double win = currnode.won;

                while (currnode.parent != null) {
                    win = 1 - win;
                    currnode.parent.played += 1;
                    currnode.parent.won += win;
                    currnode = currnode.parent;
                }
            }

        }

        public double utility(Node curr, Node parent) {
            return ((double) (curr.played - curr.won) / curr.played) + 1.4 * Math.sqrt(Math.log(parent.played) / curr.played);
        }

        public void select(Node curr) {
            if (curr.gameOver()) {
                double played = 1;
                double won = 0;
                while (curr != null) {
                    curr.won += won;
                    curr.played += played;
                    won = 1.0 - won;
                    curr = curr.parent;
                }
                return;
            }

            if (curr.isLeaf()) {
                expand(curr);
                return;
            }

            double bestUtility = utility(curr.branches[0], curr);
            Node selected = curr.branches[0];
            for (int i = 1; i < curr.branches.length; i++) {
                double tempUtility = utility(curr.branches[i], curr);
                if (tempUtility > bestUtility) {
                    bestUtility = tempUtility;
                    selected = curr.branches[i];
                }
            }

            select(selected);
        }

        public void run() {
            state state = new state();
            setupASstate(state, player, board);
            // printBoard(state);

            Node root = new Node(state, null);
            while (!stop)
                select(root);

            // System.err.println("Select ends here");
            // Dump all node scores.
            // Stack<Node> nodes = new Stack<>();
            // nodes.push(root);
            // while (nodes.size() > 0) {
            //     Node popped_node = nodes.pop();
            //     if (popped_node == null)
            //         continue;
            //     for (int i = 0; i < popped_node.depth; i++)
            //         System.err.print(" ");

            //     System.err.printf("Nodevalues: (%f/%f).\n", popped_node.won,
            //             popped_node.played);
            //     for (Node childnode : popped_node.branches)
            //         nodes.push(childnode);
            // }
            // end of dump.
            System.err.println("Total playouts throughout the entire game: " + playouts);

            // We are taking the most-played branch only.
            // Ref: https://www.youtube.com/watch?v=Fbs4lnGLS8M
            // double bestUtility = (root.branches[0].won - root.branches[0].played) / root.branches[0].played;
            double bestUtility = root.branches[0].played;
            int myBestMoveIndex = 0;
            for (int i = 0; i < root.branches.length; i++) {
                Node branch = root.branches[i];
                double tempUtility = branch.played;
                System.err.printf("Branch %d has won %f and played %f with utility %f\n", i, branch.won, branch.played, tempUtility);
                if (tempUtility > bestUtility) {
                    bestUtility = tempUtility;
                    myBestMoveIndex = i;
                }
            }
            System.err.printf("Move with utility %f/%f\n", root.branches[myBestMoveIndex].won,
                    root.branches[myBestMoveIndex].played);
            playerhelper.memcpy(bestmove, state.movelist[myBestMoveIndex],
                    playerhelper.MoveLength(state.movelist[myBestMoveIndex]));
        }

        static void printBoard(state state) {
            int y, x;

            for (y = 0; y < 8; y++) {
                for (x = 0; x < 8; x++) {
                    if (x % 2 != y % 2) {
                        if (playerhelper.empty(state.board[y][x])) {
                            System.err.print(" ");
                        } else if (playerhelper.king(state.board[y][x])) {
                            if (playerhelper.color(state.board[y][x]) == 2)
                                System.err.print("B");
                            else
                                System.err.print("A");
                        } else if (playerhelper.piece(state.board[y][x])) {
                            if (playerhelper.color(state.board[y][x]) == 2)
                                System.err.print("b");
                            else
                                System.err.print("a");
                        }
                    } else {
                        System.err.print("@");
                    }
                }
                System.err.print("\n");
            }
        }
    }
}
