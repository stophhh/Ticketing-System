package com.example.ticketing.entity;

public enum QueueStatus {
    WAITING,    // 대기 중
    ALLOWED,    // 입장 허용
    EXPIRED,    // 입장 시간 만료
    CANCELED,   // 사용자 취소
    BLOCKED// 부정 예매 등으로 차단
}