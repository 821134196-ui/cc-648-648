package com.leak.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * 使用 SQLite rowid（IDENTITY）生成主键。
 * Panache 默认的 TABLE 序列会在 JTA 事务内开第二个连接更新序列表，
 * 与主事务在 SQLite 上形成写锁自锁（SQLITE_BUSY）；IDENTITY 不占用序列连接。
 */
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
}
