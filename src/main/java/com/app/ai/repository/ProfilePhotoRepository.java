package com.app.ai.repository;

import com.app.ai.model.ProfilePhoto;
import com.app.ai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, Long> {
    Optional<ProfilePhoto> findByUser(User user);
}

