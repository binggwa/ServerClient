package com.serverclient;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.function.BiConsumer;

public class GameController {
    @FXML private GridPane boardGrid;       // 행렬 좌표를 기준으로 UI 배치 가능
    @FXML private Label scoreLabel;
    @FXML private Label timerLabel;

    private Rectangle[][] rects;
    private int W, H;
    private Timeline timer;     // 1초마다 줄어드는 타이머용
    private long endsAtMillis = 0L;

    private Stage myStage;

    // 서버로 CLICK_REQ를 보내기 위함
    // 컨트롤러에서 클라이언트로 클릭 이벤트가 발생한 좌표를 넘기고, 클라이언트에서 서버로 송부
    private BiConsumer<Integer,Integer> clickSender;

    public void setClickSender(BiConsumer<Integer,Integer> sender) {
        this.clickSender = sender;
    }

    // 게임 시작용
    public void init(int W, int H, int seconds, Stage stage) {
        this.W = W;
        this.H = H;
        this.myStage = stage;

        // 보드 생성
        rects = new Rectangle[H][W];
        boardGrid.getChildren().clear();
        int cell = 24;
        for ( int y = 0; y < H; y++ ) {
            for ( int x = 0; x < W; x++ ) {
                Rectangle r = new Rectangle(cell, cell);
                r.setStroke(Color.web("#eeeeee"));       // 사각형 외곽선 색 밝은 회색(eeeeee)
                r.setStrokeWidth(0.75);                     // 외곽선 두께
                final int fx = x;
                final int fy = y;
                r.setOnMouseClicked(e -> {          // 마우스 클릭 시, 해당 좌표를 clicksender로 넘김
                    clickSender.accept(fx, fy);
                });
                rects[fy][fx] = r;
                boardGrid.add(r, fx, fy);
            }
        }

        // 시간을 받아와 타이머 설정
        if (timer != null) timer.stop();            // 타이머 초기화
        timerLabel.setText("남은 시간 : " + seconds);       // 초기 남은 시간 표시
        // 1초마다 줄어드는 타임라인으로 타이머 생성
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int remain;
            long leftMs = endsAtMillis - System.currentTimeMillis();    // 서버가 설정한 종료시간에서, 현재 시간을 빼 남은 시간 결정
            remain = (int)(leftMs / 1000);      // 밀리초를 초로 변환
            timerLabel.setText("남은 시간 : " + remain);    // 현재 남은 시간 1초마다 갱신
            if ( remain <= 0 ) {
                timer.stop();
                myStage.close();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);       // Timeline 횟수 무제한
        timer.playFromStart();                          // timer를 항상 처음부터 실행

        // 스코어 설정
        scoreLabel.setText("점수 : 0");                   // 점수 초기값 표시
    }

    // 창 닫기용
    public void closeWindow() {
        myStage.close();
    }

    // 보드 전체를 서버에서 받아 UI에 갱신하는 메서드
    public void applySnapshot(CellColor[][] board) {
        if ( board == null ) return;
        for ( int y = 0; y < H; y++ ) {
            for ( int x = 0; x < W; x++ ) {
                rects[y][x].setFill(map(board[y][x]));
            }
        }
    }

    // 보드에 변경이 발생하면, 회색으로 바꿈
    public void applyDiff(List<Coord> diff) {
        if ( diff == null ) return;
        for ( Coord c : diff ) {
            if ( c.y >= 0 && c.y < H && c.x >= 0 && c.x < W ) {
                rects[c.y][c.x].setFill(map(CellColor.GRAY));
            }
        }
    }

    // 스코어 UI를 현재 스코어로 업데이트
    public void updateMyScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    // 서버에서 뿌린 종료시간 클라이언트 동기화용
    public void syncEndsAt(long endsAtMillis) {
        this.endsAtMillis = endsAtMillis;
    }

    // 각 색깔 ENUM 에 실제 색 매칭
    private Color map(CellColor c) {
        return switch (c) {
            case GRAY -> Color.web("#D3D3D3");
            case RED -> Color.web("#ff6b6b");
            case BLUE -> Color.web("#4dabf7");
            case GREEN -> Color.web("#51cf66");
            case YELLOW -> Color.web("#ffd43b");
        };
    }
}
