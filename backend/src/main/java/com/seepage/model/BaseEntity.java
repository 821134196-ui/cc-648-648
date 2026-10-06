package com.seepage.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * 实体基类。SQLite 用 IDENTITY（rowid）生成主键：
 * 表式 ID 生成器会在独立事务/连接中取号，与当前写事务互相等待导致 SQLITE_BUSY 自死锁。
 */
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
}
