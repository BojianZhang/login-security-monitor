package com.security.monitor.repository;

import com.security.monitor.model.User;
import com.security.monitor.model.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户位置Repository
 */
@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    /**
     * 根据用户查询位置，按登录次数倒序
     */
    List<UserLocation> findByUserOrderByLoginCountDesc(User user);

    /**
     * 查询用户的可信位置
     */
    List<UserLocation> findByUserAndIsTrustedTrue(User user);

    /**
     * 根据用户和位置查询
     */
    Optional<UserLocation> findByUserAndCountryAndRegionAndCity(User user, String country, String region, String city);

    /**
     * 查询用户的位置统计
     */
    @Query("SELECT COUNT(ul) FROM UserLocation ul WHERE ul.user = :user")
    long countByUser(@Param("user") User user);

    /**
     * 查询用户最常用的位置
     */
    @Query("SELECT ul FROM UserLocation ul WHERE ul.user = :user ORDER BY ul.loginCount DESC")
    List<UserLocation> findTopLocationsByUser(@Param("user") User user);

    /**
     * 删除用户的所有位置记录
     */
    void deleteByUser(User user);
}