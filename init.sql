SET NAMES utf8mb4;
SET character_set_client = utf8mb4;

CREATE DATABASE IF NOT EXISTS paddling_base DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE paddling_base;

CREATE TABLE IF NOT EXISTS rack (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '支架编号',
    capacity DECIMAL(10,2) NOT NULL COMMENT '承重(kg)',
    mileage_range VARCHAR(20) NOT NULL COMMENT '适配里程区间: SHORT/MEDIUM/LONG',
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
    end_date DATE COMMENT '绑定结束日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (rack_id) REFERENCES rack(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    INDEX idx_rack_id (rack_id),
    INDEX idx_team_id (team_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支架队伍绑定表';

CREATE TABLE IF NOT EXISTS binding_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    binding_id BIGINT NOT NULL COMMENT '绑定ID',
    old_rack_id BIGINT COMMENT '旧支架ID',
    new_rack_id BIGINT NOT NULL COMMENT '新支架ID',
    change_reason VARCHAR(500) COMMENT '变更原因',
    operator VARCHAR(100) DEFAULT 'system' COMMENT '操作人',
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    FOREIGN KEY (binding_id) REFERENCES binding(id) ON DELETE CASCADE,
    INDEX idx_binding_id (binding_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定变更历史表';

INSERT INTO rack (code, capacity, mileage_range, description) VALUES
('RACK001', 200.00, 'SHORT', '短距离训练专用支架A'),
('RACK002', 200.00, 'SHORT', '短距离训练专用支架B'),
('RACK003', 250.00, 'SHORT', '短距离训练专用支架C'),
('RACK004', 300.00, 'MEDIUM', '中距离训练专用支架A'),
('RACK005', 300.00, 'MEDIUM', '中距离训练专用支架B'),
('RACK006', 350.00, 'MEDIUM', '中距离训练专用支架C'),
('RACK007', 350.00, 'MEDIUM', '中距离训练专用支架D'),
('RACK008', 400.00, 'LONG', '长距离训练专用支架A'),
('RACK009', 400.00, 'LONG', '长距离训练专用支架B'),
('RACK010', 450.00, 'LONG', '长距离训练专用支架C');

INSERT INTO team (name, member_count, training_mileage, description) VALUES
('第一训练队', 12, 'SHORT', '短距离专项训练队'),
('第二训练队', 15, 'MEDIUM', '中距离专项训练队'),
('第三训练队', 10, 'LONG', '长距离专项训练队'),
('青年队', 20, 'SHORT', '青年组训练队'),
('精英队', 8, 'LONG', '精英组训练队'),
('预备队', 18, 'MEDIUM', '预备组训练队');

INSERT INTO binding (rack_id, team_id, start_date, status) VALUES
(1, 1, '2024-01-01', 'ACTIVE'),
(2, 4, '2024-01-01', 'ACTIVE'),
(4, 2, '2024-01-01', 'ACTIVE'),
(5, 6, '2024-01-01', 'ACTIVE'),
(8, 3, '2024-01-01', 'ACTIVE'),
(9, 5, '2024-01-01', 'ACTIVE');

INSERT INTO binding_history (binding_id, new_rack_id, change_reason) VALUES
(1, 1, '初始绑定'),
(2, 2, '初始绑定'),
(3, 4, '初始绑定'),
(4, 5, '初始绑定'),
(5, 8, '初始绑定'),
(6, 9, '初始绑定');