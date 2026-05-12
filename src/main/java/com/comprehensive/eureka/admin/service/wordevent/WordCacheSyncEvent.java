package com.comprehensive.eureka.admin.service.wordevent;

public record WordCacheSyncEvent(
        WordType wordType,
        SyncAction action,
        String word
) {}
