package com.example.android.bluetoothchat;

import android.util.Pair;

import java.util.ArrayList;
//handles internal chess logic such as move legality and current board state
public class Chess
{
    //represents a single square
    class Square
    {
        protected char color;
        private String type;
        private boolean moved;//can't castle after moving your king or rook, can only move pawn forward twice first move
        //empty square
        Square()
        {
            color=' ';
            type="  ";
            moved=true;//don't want to castle with a blank space!
        }
        //initial construction of the board
        Square(char color, String type)
        {
            this.color=color;
            this.type=type;
            moved=false;
        }
        //we just moved into this square
        Square(Square square)
        {
            color=square.color;
            type=square.type;
            moved=true;
        }
        public String toString()
        {
            return color+type;
        }
        public boolean equals(Square p)
        {
            return color==p.color&&type.equals(p.type);
        }
    }
    private ArrayList<Square[][]> boards;//all previous board states, used for 50 move rule and 3-fold repetition
    protected Square[][] board;//current board
    protected char turn, cap;//whose turn is it and who's the other color?
    //king positions, coordinates of legal en passant-able pawn (if applicable)
    //and number of moves without piece capture or pawn move (used for 50 move rule)
    private int wkr, wkc, bkr, bkc, epr, epc, moves;
    protected boolean playing;//are we currently in a game?
    public Chess()
    {
        boards=new ArrayList<>();
        board=new Square[8][8];
        turn='w';//starts as white row
        cap='b';
        //initial king positions
        wkr=0;
        wkc=3;
        bkr=7;
        bkc=3;
        epr=8;//8 signifies no double pawn move occurred last turn
        epc=8;
        moves=0;
        //fill in the board
        String[] p=new String[]{"rk", "kt", "bp", "kg", "qn", "bp", "kt", "rk"};
        for(int c=0; c<8; c++)
        {
            board[0][c]=new Square('w', p[c]);
            board[1][c]=new Square('w', "pn");
            board[6][c]=new Square('b', "pn");
            board[7][c]=new Square('b', p[c]);
        }
        //don't want nulls!
        for(int r=2; r<6; r++)
            for(int c=0; c<8; c++)
                board[r][c]=new Square();
    }
    //creates a deep copy of another board
    public Square[][] copy(Square[][] b)
    {
        Square[][] n=new Square[8][8];
        for(int r=0; r<8; r++)
            for(int c=0; c<8; c++)
                n[r][c]=new Square(b[r][c]);
        return n;
    }
    //given a chess object, construct one with a copy of the board for testing moves on
    public Chess(Chess chess)
    {
        boards=new ArrayList<>();
        board=copy(chess.board);
        turn=chess.turn;
        cap=chess.cap;
        wkr=chess.wkr;
        wkc=chess.wkc;
        bkr=chess.bkr;
        bkc=chess.bkc;
        epr=8;
        epc=8;
    }
    //done at the end of every turn
    public void changeTurns()
    {
        char t=turn;
        turn=cap;
        cap=t;
    }

    //tests legality of a move based on the piece being moved
    public boolean canPawnMove(int r1, int c1, int r2, int c2)
    {
        int r=r1-r2, a=Math.abs(r), c=Math.abs(c1-c2);
        //must move forward, no more than 2 spaces, in the proper direction "forward"
        if(r==0||a>2||(r>0&&turn=='w')||(r<0&&turn=='b'))
            return false;
        //capturing a piece
        if(c==1)
            //can capture one space forward, must capture a piece on the square or through en passant
            return a==1&&(board[r2][c2].color==cap||r1==epr&&c2==epc);
        //not a capture, must not have an actual piece blocking it
        else if(c==0&&board[r2][c2].color==' ')
            //either moving one space forward or two spaces with no piece on the square to move to and haven't moved yet
            return a==1||board[r2+r/2][c2].color==' '&&!board[r1][c1].moved;
        return false;
    }
    public boolean canKnightMove(int r1, int c1, int r2, int c2)
    {
        int r=Math.abs(r1-r2), c=Math.abs(c1-c2);
        return r<3&&c<3&&r+c==3;//must move no more than two spaces horizontally or vertically and 3 spaces total
    }
    public boolean canRookMove(int r1, int c1, int r2, int c2)
    {
        //must move vertically or horizontally
        if(r1==r2&&c1==c2||r1!=r2&&c1!=c2)
            return false;
        //coordinates should be ordered for less loop conditions
        if(r1>r2)
        {
            int t=r1;
            r1=r2;
            r2=t;
        }
        if(c1>c2)
        {
            int t=c1;
            c1=c2;
            c2=t;
        }
        //vertical movement
        if(r1<r2)
        {
            //check for no pieces blocking the path
            for(int r=r1+1; r<r2; r++)
                if(board[r][c2].color!=' ')
                    return false;
        }
        else
            for(int c=c1+1; c<c2; c++)
                if(board[r2][c].color!=' ')
                    return false;
        return true;
    }
    public boolean canBishopMove(int r1, int c1, int r2, int c2)
    {
        //put coordinates in order
        if(r1>r2)
        {
            //rows and columns must be swapped to maintain the some diagonal
            int t=r1;
            r1=r2;
            r2=t;
            t=c1;
            c1=c2;
            c2=t;
        }
        int r=Math.abs(r1-r2), c=c1-c2;
        //must move same amount of spaces horizontally and vertically
        if(r!=Math.abs(c))
            return false;
        //moving up and to the right or down and to the left
        if(c<0)
        {
            for(int i=1; i<r; i++)
                if(board[r1+i][c1+i].color!=' ')
                    return false;
        }
        else
            for(int i=1; i<r; i++)
                if(board[r1+i][c1-i].color!=' ')
                    return false;
        return true;
    }
    public boolean canKingMove(int r1, int c1, int r2, int c2)
    {
        int r=Math.abs(r1-r2), c=c1-c2;
        //can move one space in any direction
        if(Math.abs(c)<2)
            return r<2;
        //we must be castling, can't move vertically, have moved yet or anything other than 2 spaces horizontally
        if(r>0||board[r1][c1].moved||Math.abs(c)!=2)
            return false;
        //kingside castling
        if(c==2)
            //can't have any pieces between you and the rook and the rook can't have moved yet
            return board[r2][c1-1].color==' '&&board[r2][c2].color==' '&&!board[r2][0].moved;
        return board[r2][c1+1].color==' '&&board[r2][c2].color==' '&&board[r2][c2+1].color==' '&&!board[r2][7].moved;
    }
    //given the coordinates to move between and whether the board is hypothetical (checking king safety),
    //determine the legality of the move
    public boolean canMove(int r1, int c1, int r2, int c2, boolean real)
    {
        //must move one of your pieces to a square that doesn't have one of your pieces
        if(board[r1][c1].color!=turn||board[r2][c2].color==turn)
            return false;
        //check for king safety!
        if(real)
        {
            //temporary board for testing on
            Chess chess=new Chess(this);
            //we're trying to castle
            if(board[r1][c1].type.equals("kg")&&Math.abs(c1-c2)==2)
            {
                //check to see if we're trying to castle out of check or through check
                chess.changeTurns();
                if(chess.anyCanMove(r1, c1, false))//don't check for check when checking for check, infinite recursion!
                    return false;
                chess.changeTurns();
                int c=c1+(c2-c1)/2;
                chess.move(r1, c1, r2, c);
                if(chess.anyCanMove(r2, c, false))
                    return false;
                chess.move(r2, c, r1, c1);
            }
            //if we're in-check after doing a move, it's illegal!
            chess.move(r1, c1, r2, c2);
            if(turn=='w')
            {
                if(chess.anyCanMove(chess.wkr, chess.wkc, false))
                    return false;
            }
            else if(chess.anyCanMove(chess.bkr, chess.bkc, false))
                return false;
        }
        //basic checking to for move legality, doesn't account for check
        if(board[r1][c1].type.equals("pn"))
            return canPawnMove(r1, c1, r2, c2);
        else if(board[r1][c1].type.equals("kt"))
            return canKnightMove(r1, c1, r2, c2);
        else if(board[r1][c1].type.equals("rk"))
            return canRookMove(r1, c1, r2, c2);
        else if(board[r1][c1].type.equals("bp"))
            return canBishopMove(r1, c1, r2, c2);
        else if(board[r1][c1].type.equals("qn"))
            return canRookMove(r1, c1, r2, c2)||canBishopMove(r1, c1, r2, c2);
        return canKingMove(r1, c1, r2, c2);
    }
    //mainly used to check to see if a player is in check
    public boolean anyCanMove(int r2, int c2, boolean real)
    {
        for(int r=0; r<8; r++)
            for(int c=0; c<8; c++)
                if(canMove(r, c, r2, c2, real))
                    return true;
        return false;
    }
    //used to check for checkmate or stalemate
    public boolean anyCanMove()
    {
        for(int r=0; r<8; r++)
            for(int c=0; c<8; c++)
                if(anyCanMove(r, c, true))
                    return true;
        return false;
    }
    //does a move
    public void move(int r1, int c1, int r2, int c2)
    {
        boards.add(copy(board));//add a copy of the board to the list of states encountered
        //king is being moved, update its position
        if(board[r1][c1].type.equals("kg"))
        {
            //definitely cannot en passant after a king move!
            epr=8;
            if(turn=='w')
            {
                wkr=r2;
                wkc=c2;
            }
            else
            {
                bkr=r2;
                bkc=c2;
            }
            //must be castling
            if(Math.abs(c1-c2)==2)
                //king-side castling
                if(c2==1)
                {
                    //move the rook in addition to the king
                    board[r2][2]=new Square(board[r2][0]);
                    board[r2][0]=new Square();
                }
                else
                {
                    board[r2][4]=new Square(board[r2][7]);
                    board[r2][7]=new Square();
                }
        }
        else if(board[r1][c1].type.equals("pn")) {
            //no moves completed without a pawn move (gets incremented after the method)
            moves=-1;
            //moved two spaces, can be captured en passant
            if (Math.abs(r1 - r2) == 2) {
                epr = r2;
                epc = c2;
            } else {
                //capturing en passant
                if (r1 == epr && c2 == epc)
                    board[r1][c2] = new Square();
                //en passant no longer available
                epr = 8;
            }
        }
        else
            epr=8;//random piece moved, definitely cannot en passant afterwards
        //no piece captured, increment number of moves completed without pawn move or captured
        if(board[r2][c2].color==' ')
            moves++;
        else
            moves=0;
        //move the piece!
        board[r2][c2]=new Square(board[r1][c1]);
        board[r1][c1]=new Square();
        changeTurns();//done at the end of every move, hypothetical or not
    }
    //returns a list of all legal moves a piece has
    public ArrayList<Pair<Integer, Integer>> moves(int r1, int c1)
    {
        ArrayList<Pair<Integer, Integer>> m=new ArrayList<>();
        for(int r2=0; r2<8; r2++)
            for(int c2=0; c2<8; c2++)
                if(canMove(r1, c1, r2, c2, true))
                    m.add(new Pair<>(r2, c2));
        return m;
    }
    public boolean equals(Square[][] b)
    {
        for(int r=0; r<8; r++)
            for(int c=0; c<8; c++)
                if(!board[r][c].equals(b[r][c]))
                    return false;
        return true;
    }
    //returns the toast message to be displayed on screen
    public String state()
    {
        //100 moves for each player means 50 moves
        if(moves==100) {
            playing=true;
            return "Draw by 50-move rule";
        }
        //people don't pay attention while playing the chess app, grabs the user's attention
        String s="It is "+(turn=='w'?"white":"black")+"'s turn";
        changeTurns();//must be done for move legality checking to work for the opposite player
        //the king could be captured!
        if(turn=='w'&&anyCanMove(bkr, bkc, false)||turn=='b'&&anyCanMove(wkr, wkc, false))
            s="Check";//more important than knowing whose turn it is
        changeTurns();
        if(!anyCanMove()) {
            playing = false;
            return s.equals("Check") ? "Checkmate" : "Stalemate";
        }
        //count how many times this position has occurred
        int r=0;
        for(Square[][] b: boards)
            if(equals(b))
                r++;
        //2 previous instances of this board, 3-fold repetition
        if(r==2) {
            playing = false;
            return "Draw by 3-fold repetition";
        }
        int w=0, b=0;
        for(int i=0; i<8; i++)
            for(int j=0; j<8; j++)
                //major pieces are still on the board (pawn can promote), draw is unlikely
                if(board[i][j].type.equals("rk")||board[i][j].type.equals("qn")||board[i][j].type.equals("pn"))
                    return s;
                //found a minor piece (knight or bishop), can still win with at least 2 of them
                else if(board[i][j].color=='w')
                    w++;
                else if(board[i][j].color=='b')
                    b++;
        //0-1 knights and/or bishops per player and nothing else? No way to win, sorry
        if(w<2&&b<2)
        {
            playing=false;
            return "Draw by insufficient material";
        }
        return s;
    }
    //promotes the pawn at these coordinates to the piece the player selected in MainActivity
    public String promote(int r, int c, int w)
    {
        board[r][c].type=w==0?"rk":w==1?"kt":w==2?"bp":"qn";
        return board[r][c].toString();//returned out of convenience for MainActivity
    }
    //ASCII representation of the board, used for testing before this file's integration with the rest of the app
    public String toString()
    {
        String s="";
        for(int r=0; r<8; r++)
        {
            for(int c=0; c<8; c++)
                s+=board[r][c];
            s+="\n";
        }
        return s;
    }
}