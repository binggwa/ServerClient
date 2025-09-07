package com.serverclient;

import java.io.Serializable;
import java.util.List;

enum Info {
    JOIN, EXIT, SEND,
    READY,
    LOBBY_UPDATE,
    START_GAME,
    // 게임 조작
    CLICK_REQ, CLICK_OK, CLICK_REJECT,
    BOARD_SNAPSHOT, BOARD_DIFF, SCORE_UPDATE, TIMER_SYNC,
    // 게임 마무리
    GAME_OVER
}

public class InfoDTO implements Serializable {
    private String nickName;
    private String message;
    private Info command;

    // Ready 상태 전송용 추가
    private Boolean ready;

    // 로비 상태 브로드캐스트용
    private List<PlayerStatus> lobby;   // PlayerStatus(nickname, ready)

    // START_GAME
    private Long seed;                  // 같은 시드로 같은 보드 생성
    private Integer durationSec;        // 게임 시작시 초기시간 지정
    private Integer width;
    private Integer height;

    // CLICK 관련
    private Integer x;
    private Integer y;
    private Integer clientSeq;
    private Long serverSeq;
    private String reason;

    // 점수 정리
    private CellColor[][] board;    // 스냅샷용
    private List<Coord> diff;       // 변경 좌표 리스트
    private String player;          // 점수 변경 플레이어
    private Integer score;          // 점수 변경량

    // 타이머
    private Long endsAtMillis;      // 라운드 종료 시각을 보내 타이머를 정확히 맞춤

    // 종료 결과
    private List<PlayerScore> results;

    public String getNickName(){
        return nickName;
    }

    public Info getCommand(){
        return command;
    }

    public String getMessage(){
        return message;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setCommand(Info command) {
        this.command = command;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    public List<PlayerStatus> getLobby() {
        return lobby;
    }

    public void setLobby(List<PlayerStatus> lobby) {
        this.lobby = lobby;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getClientSeq() {
        return clientSeq;
    }

    public void setClientSeq(Integer clientSeq) {
        this.clientSeq = clientSeq;
    }

    public Long getServerSeq() {
        return serverSeq;
    }

    public void setServerSeq(Long serverSeq) {
        this.serverSeq = serverSeq;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CellColor[][] getBoard() {
        return board;
    }

    public void setBoard(CellColor[][] board) {
        this.board = board;
    }

    public List<Coord> getDiff() {
        return diff;
    }

    public void setDiff(List<Coord> diff) {
        this.diff = diff;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Long getEndsAtMillis() {
        return endsAtMillis;
    }

    public void setEndsAtMillis(Long endsAtMillis) {
        this.endsAtMillis = endsAtMillis;
    }

    public List<PlayerScore> getResults() {
        return results;
    }

    public void setResults(List<PlayerScore> results) {
        this.results = results;
    }
}