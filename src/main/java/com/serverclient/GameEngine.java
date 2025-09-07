package com.serverclient;

import java.io.Serializable;
import java.util.*;

public class GameEngine implements Serializable {
    private int W, H;
    private Random rng;
    private CellColor[][] board;

    public CellColor[][] getBoard() {
        return board;
    }

    public GameEngine(int W, int H, long seed) {
        this.W = W;
        this.H = H;
        this.board = new CellColor[H][W];
        this.rng = new Random(seed);
        regenerateBoard();
    }

    // 보드를 새로 깔고, 유효한 수(클릭 가능한 회색 칸)가 생길 때까지 반복해서 만든다
    public void regenerateBoard(){
        do {
            for ( int y = 0; y < H; y++ ){
                for ( int x = 0; x < W; x++ ){
                    board[y][x] = randomColor();
                }
            }
        } while ( !hasAnyClick() ); // 유효 수 없으면 다시 생성
    }

    // 랜덤 컬러 생성용
    private CellColor randomColor(){
        int r = rng.nextInt(5);
        if ( r == 0 ) return CellColor.GRAY;
        if ( r == 1 ) return CellColor.RED;
        if ( r == 2 ) return CellColor.BLUE;
        if ( r == 3 ) return CellColor.GREEN;
        return CellColor.YELLOW;
    }

    // 보드에 하나라도 유효한 클릭이 있는지 확인
    public boolean hasAnyClick(){
        for ( int y = 0; y < H; y++ ){
            for ( int x = 0; x < W; x++ ){
                if ( board[y][x] == CellColor.GRAY && canClick(x,y) ) return true;
            }
        }
        return false;
    }

    // 처음 만난 유색 칸 좌표랑 색 저장
    private static class FirstHit {
        int x, y;
        CellColor color;
        FirstHit(int x, int y, CellColor c) {
            this.x = x;
            this.y = y;
            this.color = c;
        }
    }

    // (x,y)가 회색일 때, 4방향의 회색이 아닌 색 중 같은 색이 2개 이상인지 검사
    private boolean canClick(int x, int y){
        FirstHit e = firstNonGray(x,y, +1, 0);
        FirstHit w = firstNonGray(x,y, -1, 0);
        FirstHit n = firstNonGray(x,y, 0, -1);
        FirstHit s = firstNonGray(x,y, 0, +1);

        // 4방향 결과를 배열로 모으고, 색 개수를 카운트
        FirstHit[] hits = new FirstHit[]{ e, w, n, s };

        int countRed = 0, countBlue = 0, countGreen = 0, countYellow = 0;

        for ( int i = 0; i < hits.length; i++ ) {
            FirstHit h = hits[i];
            if ( h == null ) continue;
            if ( h.color == CellColor.RED ) countRed++;
            else if ( h.color == CellColor.BLUE ) countBlue++;
            else if ( h.color == CellColor.GREEN ) countGreen++;
            else if ( h.color == CellColor.YELLOW ) countYellow++;
        }

        // 같은 색이 2개 이상 존재하면 true
        if ( countRed >= 2 ) return true;
        if ( countBlue >= 2 ) return true;
        if ( countGreen >= 2 ) return true;
        if ( countYellow >= 2 ) return true;

        return false;
    }

    // 4방향으로 나아가며 처음 만나는 유색 칸을 찾아서 좌표/색을 반환 (없으면 null)
    private FirstHit firstNonGray(int x, int y, int dx, int dy){
        int cx = x + dx, cy = y + dy;
        while ( inBounds(cx,cy) ){
            CellColor c = board[cy][cx];
            if ( c != CellColor.GRAY ) {
                return new FirstHit(cx, cy, c);
            }
            cx += dx; cy += dy;
        }
        return null;
    }

    private boolean inBounds(int x, int y){
        return x >= 0 && x < W && y >= 0 && y < H;
    }

    // 누르는 칸은 회색. 클릭한 칸 쪽으로 뻗어나가서 같은 색 개수 2개 이상이면 해당 색을 회색으로 변경
    public ClickResult tryClick(int x, int y) {
        if ( !inBounds(x, y) ) {
            return ClickResult.fail("범위 밖 클릭!");
        }
        if ( board[y][x] != CellColor.GRAY ) {
            return ClickResult.fail("회색 칸이 아님!");
        }

        FirstHit e = firstNonGray(x, y, +1, 0);
        FirstHit w = firstNonGray(x, y, -1, 0);
        FirstHit n = firstNonGray(x, y, 0, -1);
        FirstHit s = firstNonGray(x, y, 0, +1);

        FirstHit[] hits = new FirstHit[]{e, w, n, s};

        int countRed = 0, countBlue = 0, countGreen = 0, countYellow = 0;
        for ( int i = 0; i < hits.length; i++ ) {
            FirstHit h = hits[i];
            if ( h == null ) continue;
            if ( h.color == CellColor.RED ) countRed++;
            else if ( h.color == CellColor.BLUE ) countBlue++;
            else if ( h.color == CellColor.GREEN ) countGreen++;
            else if ( h.color == CellColor.YELLOW ) countYellow++;
        }

        // 사라지게 할 색상 결정
        CellColor targetColor = null;
        if ( countRed >= 2 ) targetColor = CellColor.RED;
        else if ( countBlue >= 2 ) targetColor = CellColor.BLUE;
        else if ( countGreen >= 2 ) targetColor = CellColor.GREEN;
        else if ( countYellow >= 2 ) targetColor = CellColor.YELLOW;

        if ( targetColor == null ) {
            return ClickResult.fail("색 부족!");
        }

        // 사라지게 할 색상으로 처음 걸린 블록들만 회색으로 바꿈
        List<Coord> changed = new ArrayList<>();
        for ( int i = 0; i < hits.length; i++ ) {
            FirstHit h = hits[i];
            if ( h == null ) continue;
            if ( h.color == targetColor ) {
                board[h.y][h.x] = CellColor.GRAY;
                changed.add(new Coord(h.x, h.y));
            }
        }

        // 더 이상 유효한 클릭이 없으면 보드를 새로 깐다
        if ( !hasAnyClick() ) {
            regenerateBoard();
        }

        return ClickResult.ok(changed);
    }
}