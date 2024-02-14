import java.util.*;
import java.io.*;

public class state {
    int player;
    char board[][] = new char[8][8];
    char movelist[][] = new char[48][12]; 
    int numLegalMoves;

    public state()
    {
    }

    public state(state state)
    {
        playerhelper.memcpy(this.board,state.board);
        this.player=state.player;

        for(int i=0;i<movelist.length;i++)
        {
            for(int j=0;j<movelist[i].length;j++)
            {
                this.movelist[i][j] = state.movelist[i][j];
            }
        }
    }
}

