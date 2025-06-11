package com.comprehensive.eureka.admin.repository;

import com.comprehensive.eureka.admin.entity.UserForbiddenWordsChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserForbiddenWordsChatRepository
        extends JpaRepository<UserForbiddenWordsChat, Long> {


    List<UserForbiddenWordsChat> findByUserIdIn(List<Long> userIds);
}
