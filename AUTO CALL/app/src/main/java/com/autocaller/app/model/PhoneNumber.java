package com.autocaller.app.model;

public class PhoneNumber {
    private String number;
    private CallStatus status;
    private int index;
    private long timestamp;

    public enum CallStatus {
        PENDING("대기중"),
        DIALING("다이얼중"),
        COMPLETED("완료"),
        FAILED("실패"),
        ANSWERED("응답됨");

        private final String displayName;

        CallStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public PhoneNumber(String number, int index) {
        this.number = number;
        this.index = index;
        this.status = CallStatus.PENDING;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PhoneNumber{" +
                "number='" + number + '\'' +
                ", status=" + status +
                ", index=" + index +
                '}';
    }
}

