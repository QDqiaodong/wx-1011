SET NAMES utf8mb4;
SET character_set_client = utf8mb4;

CREATE DATABASE IF NOT EXISTS paddling_base DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE paddling_base;

CREATE TABLE IF NOT EXISTS rack (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '支架编号',
    capacity DECIMAL(10,2) NOT NULL COMMENT '承重(kg)',
    mileage_range VARCHAR(20) NOT NULL COMMENT '适配里程区间: SHORT/MEDIUM/LONG',
    daily_mileage_quota DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '日里程配额(km/天)，月结算=覆盖天数×该配额',
    description VARCHAR(500) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_mileage_range (mileage_range)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岸边停靠支架表';

CREATE TABLE IF NOT EXISTS team (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '队伍名称',
    member_count INT DEFAULT 0 COMMENT '人数',
    training_mileage VARCHAR(20) NOT NULL COMMENT '常规训练里程: SHORT/MEDIUM/LONG',
    description VARCHAR(500) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_training_mileage (training_mileage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='皮划艇训练队伍表';

CREATE TABLE IF NOT EXISTS binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rack_id BIGINT NOT NULL COMMENT '支架ID',
    team_id BIGINT NOT NULL COMMENT '队伍ID',
    start_date DATE NOT NULL COMMENT '绑定开始日期',
    end_date DATE COMMENT '绑定结束日期（NULL=生效中；换绑/解绑后为截止日，当日仍计入旧段）',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (rack_id) REFERENCES rack(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    INDEX idx_rack_id (rack_id),
    INDEX idx_team_id (team_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支架队伍绑定表（月中换架会拆成首尾相接的多行段）';

CREATE TABLE IF NOT EXISTS binding_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    binding_id BIGINT NOT NULL COMMENT '绑定ID（换架时旧段与新段各留一条）',
    old_rack_id BIGINT COMMENT '旧支架ID',
    new_rack_id BIGINT NOT NULL COMMENT '新支架ID',
    change_reason VARCHAR(500) COMMENT '变更原因',
    operator VARCHAR(100) DEFAULT 'system' COMMENT '操作人',
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    FOREIGN KEY (binding_id) REFERENCES binding(id) ON DELETE CASCADE,
    INDEX idx_binding_id (binding_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定变更历史表';

-- 月度里程结算单（封账单头）：(team_id, period_month) 唯一，并发重复封账由数据库拒绝
CREATE TABLE IF NOT EXISTS monthly_settlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL COMMENT '队伍ID',
    team_name VARCHAR(100) NOT NULL COMMENT '队伍名称快照',
    period_month VARCHAR(7) NOT NULL COMMENT '结算月份 yyyy-MM',
    training_mileage VARCHAR(20) NOT NULL COMMENT '队伍训练档位快照 SHORT/MEDIUM/LONG',
    training_mileage_label VARCHAR(20) NOT NULL COMMENT '档位中文快照',
    target_mileage DECIMAL(10,2) NOT NULL COMMENT '月达标里程阈值快照(km)',
    total_mileage DECIMAL(12,2) NOT NULL COMMENT '当月各绑定段贡献里程合计(km)',
    segment_count INT NOT NULL DEFAULT 0 COMMENT '生效绑定段数',
    qualified TINYINT(1) NOT NULL COMMENT '是否达标: 总里程>=档位达标线',
    sealed_at DATETIME NOT NULL COMMENT '封账时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_team_period (team_id, period_month),
    INDEX idx_period_month (period_month),
    INDEX idx_team_id (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度里程结算单（封账后不可变）';

-- 结算单分段明细（整段快照）：封账后即使换绑/解绑/改支架配额，本行数值也不变
CREATE TABLE IF NOT EXISTS settlement_segment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL COMMENT '所属结算单ID',
    segment_no INT NOT NULL COMMENT '段序号，从1开始',
    binding_id BIGINT COMMENT '来源绑定ID（仅溯源，不做外键）',
    rack_id BIGINT NOT NULL COMMENT '支架ID快照',
    rack_code VARCHAR(50) NOT NULL COMMENT '支架编号快照',
    rack_mileage_range VARCHAR(20) COMMENT '支架里程区间快照',
    segment_start DATE NOT NULL COMMENT '该段在结算月内生效切片起始日',
    segment_end DATE NOT NULL COMMENT '该段在结算月内生效切片结束日',
    covered_days INT NOT NULL COMMENT '覆盖天数（含首尾）',
    daily_mileage_quota DECIMAL(8,2) NOT NULL COMMENT '日里程配额快照(km/天)',
    contributed_mileage DECIMAL(12,2) NOT NULL COMMENT '段贡献里程=覆盖天数×配额(km)',
    binding_start_date DATE NOT NULL COMMENT '来源绑定原始开始日快照',
    binding_end_date DATE COMMENT '来源绑定原始结束日快照',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_segment_settlement FOREIGN KEY (settlement_id)
        REFERENCES monthly_settlement(id) ON DELETE CASCADE,
    INDEX idx_settlement_id (settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单分段绑定明细快照';

INSERT INTO rack (code, capacity, mileage_range, daily_mileage_quota, description) VALUES
('RACK001', 200.00, 'SHORT', 4.00, '短距离训练专用支架A'),
('RACK002', 200.00, 'SHORT', 4.50, '短距离训练专用支架B'),
('RACK003', 250.00, 'SHORT', 5.00, '短距离训练专用支架C'),
('RACK004', 300.00, 'MEDIUM', 10.00, '中距离训练专用支架A'),
('RACK005', 300.00, 'MEDIUM', 11.00, '中距离训练专用支架B'),
('RACK006', 350.00, 'MEDIUM', 12.00, '中距离训练专用支架C'),
('RACK007', 350.00, 'MEDIUM', 12.50, '中距离训练专用支架D'),
('RACK008', 400.00, 'LONG', 20.00, '长距离训练专用支架A'),
('RACK009', 400.00, 'LONG', 22.00, '长距离训练专用支架B'),
('RACK010', 450.00, 'LONG', 24.00, '长距离训练专用支架C');

INSERT INTO team (name, member_count, training_mileage, description) VALUES
('第一训练队', 12, 'SHORT', '短距离专项训练队'),
('第二训练队', 15, 'MEDIUM', '中距离专项训练队'),
('第三训练队', 10, 'LONG', '长距离专项训练队'),
('青年队', 20, 'SHORT', '青年组训练队'),
('精英队', 8, 'LONG', '精英组训练队'),
('预备队', 18, 'MEDIUM', '预备组训练队');

-- 演示数据：第三训练队(team=3) 在 2026-08 月中换过支架（RACK008 用到 8/15，8/16 起换 RACK009）
INSERT INTO binding (rack_id, team_id, start_date, end_date, status) VALUES
(1, 1, '2024-01-01', NULL, 'ACTIVE'),
(2, 4, '2024-01-01', NULL, 'ACTIVE'),
(4, 2, '2024-01-01', NULL, 'ACTIVE'),
(5, 6, '2024-01-01', NULL, 'ACTIVE'),
(8, 3, '2026-08-01', '2026-08-15', 'INACTIVE'),
(9, 3, '2026-08-16', NULL, 'ACTIVE');

INSERT INTO binding_history (binding_id, old_rack_id, new_rack_id, change_reason) VALUES
(1, NULL, 1, '初始绑定'),
(2, NULL, 2, '初始绑定'),
(3, NULL, 4, '初始绑定'),
(4, NULL, 5, '初始绑定'),
(5, NULL, 8, '初始绑定'),
(5, 8, 9, '训练计划调整，月中换架'),
(6, 8, 9, '训练计划调整，月中换架');
