package com.iot.platform.service;

import com.iot.platform.entity.DeviceData;
import com.iot.platform.mapper.DeviceDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class DeviceDataCacheService {
    @Autowired
    private DeviceDataMapper deviceDataMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "device:data:";  // 手动缓存的 key 前缀
    private static final String NULL_TAG = "NULL";   // 空值标记（区别于真实数据）
    private static final long NULL_TTL = 5;           // 空值缓存 5 分钟
    private static final long DATA_TTL = 30;          // 正常数据缓存 30 分钟

    private static final String LOCK_PREFIX = "device:lock:"; //互斥锁 key前缀
    private static final long LOCK_TTL = 10; //锁超时时间（秒），防死锁

    public DeviceData getByIdSafe(Long id){
        String key = KEY_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);

        // ① 命中空值标记 → 直接返回 null，不查 DB（防穿透）
        if (NULL_TAG.equals(cached)) {
            System.out.println("[防穿透] 命中空值缓存，跳过 DB 查询");
            return null;
        }

        // ② 命中正常数据 → 返回
        if (cached != null){
            System.out.println("[防穿透] 命中数据缓存");
            return (DeviceData) cached;
        }

        // ③ 缓存未命中 → 查 DB
        System.out.println("[防穿透] 缓存未命中，查询数据库...");
        DeviceData data = deviceDataMapper.selectById(id);

        if (data == null){
            // ④ DB 也没有 → 缓存空值标记（短 TTL），防穿透
            redisTemplate.opsForValue().set(key, NULL_TAG, NULL_TTL, TimeUnit.MINUTES);
            System.out.println("[防穿透] DB 未查到，已缓存空值标记 (TTL=" + NULL_TTL + "min)");
            return null;
        }

        // ⑤ DB 有 → 缓存正常数据（长 TTL）
        redisTemplate.opsForValue().set(key, data, DATA_TTL, TimeUnit.MINUTES);
        System.out.println("[防穿透] DB 查到数据，已缓存 (TTL=" + DATA_TTL + "min)");
        return data;
    }

    public DeviceData getByIdWithLock(Long id){
        String key = KEY_PREFIX + id;
        Object cached  = redisTemplate.opsForValue().get(key);

        // ① 命中空值标记 → 直接返回 null
        if (NULL_TAG.equals(cached)) {
            System.out.println("[防击穿] 命中空值缓存");
            return null;
        }

        // ② 命中正常数据 → 返回
        if (cached != null) {
            System.out.println("[防击穿] 命中数据缓存");
            return (DeviceData) cached;
        }

        // ③ 缓存未命中 → 尝试获取互斥锁
        String lockKey = LOCK_PREFIX + id;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_TTL, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            // ④ 获取锁成功 → 查 DB 重建缓存
            try {
                System.out.println("[防击穿] 获取锁成功，查询数据库...");
                DeviceData data = deviceDataMapper.selectById(id);

                if (data == null) {
                    redisTemplate.opsForValue().set(key, NULL_TAG, NULL_TTL, TimeUnit.MINUTES);
                    System.out.println("[防击穿] DB 未查到，已缓存空值标记");
                    return null;
                }

                redisTemplate.opsForValue().set(key, data, DATA_TTL, TimeUnit.MINUTES);
                System.out.println("[防击穿] DB 查到数据，已缓存");
                return data;
            } finally {
                // ⑤ 释放锁（finally 确保一定释放）
                redisTemplate.delete(lockKey);
                System.out.println("[防击穿] 已释放锁");
            }
        } else {
            // ⑥ 获取锁失败 → 等待后重查缓存
            System.out.println("[防击穿] 获取锁失败，等待重试...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getByIdWithLock(id);  // 递归重试
        }
    }
}
