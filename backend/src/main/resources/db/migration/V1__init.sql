-- ============================================================
-- GSECP 迭代 0 初始化脚本
-- 目标：MySQL 8.0.16+ / utf8mb4_0900_ai_ci
-- 说明：数据库 gsecp 需预先创建；本脚本由 Flyway 在应用启动时执行。
--       核心表一次建好，迭代 2~4 只加功能不改表。
-- ============================================================

-- ------------------------------------------------------------
-- A. 系统基础
-- ------------------------------------------------------------

CREATE TABLE sys_file (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  storage_type  VARCHAR(16)  NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/MINIO/OSS',
  storage_path  VARCHAR(512) NOT NULL COMMENT '存储相对路径或对象键',
  content_type  VARCHAR(128) DEFAULT NULL,
  size_bytes    BIGINT UNSIGNED NOT NULL DEFAULT 0,
  md5           CHAR(32)     DEFAULT NULL,
  uploader_id   BIGINT UNSIGNED DEFAULT NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_file_md5 (md5),
  KEY idx_file_uploader (uploader_id)
) ENGINE=InnoDB COMMENT='统一文件表';

CREATE TABLE sys_user (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username       VARCHAR(64)  NOT NULL COMMENT '登录账号',
  password_hash  VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  real_name      VARCHAR(64)  NOT NULL,
  user_no        VARCHAR(64)  DEFAULT NULL COMMENT '学号/工号',
  avatar_file_id BIGINT UNSIGNED DEFAULT NULL,
  email          VARCHAR(128) DEFAULT NULL,
  phone          VARCHAR(32)  DEFAULT NULL,
  status         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  last_login_at  DATETIME DEFAULT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_no (user_no),
  CONSTRAINT fk_user_avatar FOREIGN KEY (avatar_file_id) REFERENCES sys_file (id)
) ENGINE=InnoDB COMMENT='用户';

CREATE TABLE sys_role (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(32) NOT NULL COMMENT 'ADMIN/TEACHER/STUDENT',
  name        VARCHAR(64) NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB COMMENT='角色';

CREATE TABLE sys_user_role (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL,
  role_id    BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB COMMENT='用户-角色关联';

CREATE TABLE sys_permission (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code       VARCHAR(128) NOT NULL COMMENT '如 step:manage',
  name       VARCHAR(64)  NOT NULL,
  module     VARCHAR(64)  DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_perm_code (code)
) ENGINE=InnoDB COMMENT='权限点';

CREATE TABLE sys_role_permission (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_id       BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_perm (role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB COMMENT='角色-权限关联';

CREATE TABLE sys_config (
  config_key   VARCHAR(64)  NOT NULL,
  config_value VARCHAR(512) NOT NULL,
  description  VARCHAR(255) DEFAULT NULL,
  updated_by   BIGINT UNSIGNED DEFAULT NULL,
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (config_key)
) ENGINE=InnoDB COMMENT='系统参数配置';

-- ------------------------------------------------------------
-- B. 班级与课程
-- ------------------------------------------------------------

CREATE TABLE teaching_class (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(128) NOT NULL,
  teacher_id  BIGINT UNSIGNED NOT NULL,
  description VARCHAR(512) DEFAULT NULL,
  status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_class_teacher (teacher_id),
  CONSTRAINT fk_class_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user (id)
) ENGINE=InnoDB COMMENT='班级';

CREATE TABLE invite_code (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  class_id   BIGINT UNSIGNED NOT NULL,
  code       VARCHAR(32) NOT NULL,
  expire_at  DATETIME DEFAULT NULL COMMENT 'NULL 表示不过期',
  max_uses   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 表示不限次数',
  used_count INT UNSIGNED NOT NULL DEFAULT 0,
  enabled    TINYINT UNSIGNED NOT NULL DEFAULT 1,
  created_by BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_invite_code (code),
  KEY idx_invite_class (class_id),
  CONSTRAINT fk_invite_class FOREIGN KEY (class_id) REFERENCES teaching_class (id)
) ENGINE=InnoDB COMMENT='班级邀请码';

CREATE TABLE class_member (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  class_id       BIGINT UNSIGNED NOT NULL,
  student_id     BIGINT UNSIGNED NOT NULL,
  invite_code_id BIGINT UNSIGNED DEFAULT NULL,
  status         VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REMOVED',
  joined_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_class_student (class_id, student_id),
  KEY idx_member_student (student_id),
  CONSTRAINT fk_member_class  FOREIGN KEY (class_id)       REFERENCES teaching_class (id),
  CONSTRAINT fk_member_student FOREIGN KEY (student_id)    REFERENCES sys_user (id),
  CONSTRAINT fk_member_invite  FOREIGN KEY (invite_code_id) REFERENCES invite_code (id)
) ENGINE=InnoDB COMMENT='班级学生名单';

CREATE TABLE course_template (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name            VARCHAR(128) NOT NULL,
  description     VARCHAR(512) DEFAULT NULL,
  definition_json JSON DEFAULT NULL COMMENT '步骤与交付项结构定义',
  created_by      BIGINT UNSIGNED NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted      TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_tpl_creator (created_by)
) ENGINE=InnoDB COMMENT='课程模板（管理员维护，迭代2启用）';

CREATE TABLE course (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  class_id    BIGINT UNSIGNED NOT NULL,
  template_id BIGINT UNSIGNED DEFAULT NULL COMMENT '套用的模板，可为空',
  teacher_id  BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(128) NOT NULL,
  description VARCHAR(512) DEFAULT NULL,
  status      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
  start_at    DATETIME DEFAULT NULL,
  end_at      DATETIME DEFAULT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_course_class (class_id),
  CONSTRAINT fk_course_class    FOREIGN KEY (class_id)    REFERENCES teaching_class (id),
  CONSTRAINT fk_course_template FOREIGN KEY (template_id) REFERENCES course_template (id),
  CONSTRAINT fk_course_teacher  FOREIGN KEY (teacher_id)  REFERENCES sys_user (id)
) ENGINE=InnoDB COMMENT='课程实例';

-- ------------------------------------------------------------
-- C. 步骤与交付物
-- ------------------------------------------------------------

CREATE TABLE dict_deliverable_type (
  code               VARCHAR(32)  NOT NULL,
  name               VARCHAR(64)  NOT NULL,
  frontend_component VARCHAR(128) NOT NULL COMMENT '前端渲染组件标识',
  backend_handler    VARCHAR(128) NOT NULL COMMENT '后端校验处理器 Bean 名',
  enabled            TINYINT UNSIGNED NOT NULL DEFAULT 1,
  sort_order         INT NOT NULL DEFAULT 0,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (code)
) ENGINE=InnoDB COMMENT='交付物类型字典：只允许新增/停用，禁止修改 code';

CREATE TABLE step (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  course_id           BIGINT UNSIGNED NOT NULL,
  title               VARCHAR(200) NOT NULL,
  description         TEXT DEFAULT NULL,
  requirement         TEXT DEFAULT NULL,
  weight              DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '总评占比(%)，同一课程合计应为100',
  publish_at          DATETIME DEFAULT NULL,
  due_at              DATETIME DEFAULT NULL,
  status              VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/WITHDRAWN',
  depends_on_step_id  BIGINT UNSIGNED DEFAULT NULL COMMENT '步骤依赖，迭代2启用',
  allow_resubmit      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '批改后是否允许补交',
  sort_order          INT NOT NULL DEFAULT 0,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_step_course (course_id, sort_order),
  CONSTRAINT fk_step_course  FOREIGN KEY (course_id)          REFERENCES course (id),
  CONSTRAINT fk_step_depends FOREIGN KEY (depends_on_step_id) REFERENCES step (id)
) ENGINE=InnoDB COMMENT='课程步骤';

CREATE TABLE step_deliverable_item (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  step_id         BIGINT UNSIGNED NOT NULL,
  name            VARCHAR(128) NOT NULL COMMENT '如 设计文档 / 源代码 / 仓库地址',
  type_code       VARCHAR(32)  NOT NULL,
  required        TINYINT UNSIGNED NOT NULL DEFAULT 1,
  constraint_json JSON DEFAULT NULL COMMENT '类型专属约束，如 {"maxSize":"50MB","formats":["pdf","docx"]}',
  sort_order      INT NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted      TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_sdi_step (step_id, sort_order),
  CONSTRAINT fk_sdi_step FOREIGN KEY (step_id)   REFERENCES step (id),
  CONSTRAINT fk_sdi_type FOREIGN KEY (type_code) REFERENCES dict_deliverable_type (code)
) ENGINE=InnoDB COMMENT='步骤交付项要求';

CREATE TABLE step_reference_file (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  step_id    BIGINT UNSIGNED NOT NULL,
  file_id    BIGINT UNSIGNED NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_step_ref (step_id, file_id),
  CONSTRAINT fk_srf_step FOREIGN KEY (step_id) REFERENCES step (id),
  CONSTRAINT fk_srf_file FOREIGN KEY (file_id) REFERENCES sys_file (id)
) ENGINE=InnoDB COMMENT='步骤参考资料附件';

CREATE TABLE deliverable_template (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(128) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  owner_id    BIGINT UNSIGNED NOT NULL COMMENT '创建教师',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='常用交付物模板（迭代2启用）';

CREATE TABLE deliverable_template_item (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id     BIGINT UNSIGNED NOT NULL,
  name            VARCHAR(128) NOT NULL,
  type_code       VARCHAR(32)  NOT NULL,
  required        TINYINT UNSIGNED NOT NULL DEFAULT 1,
  constraint_json JSON DEFAULT NULL,
  sort_order      INT NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted      TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_dti_tpl (template_id, sort_order),
  CONSTRAINT fk_dti_tpl  FOREIGN KEY (template_id) REFERENCES deliverable_template (id),
  CONSTRAINT fk_dti_type FOREIGN KEY (type_code)   REFERENCES dict_deliverable_type (code)
) ENGINE=InnoDB COMMENT='交付物模板项';

-- ------------------------------------------------------------
-- D. 提交与评分
-- ------------------------------------------------------------

CREATE TABLE submission (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  step_id     BIGINT UNSIGNED NOT NULL,
  student_id  BIGINT UNSIGNED NOT NULL,
  version     INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '同一学生对同一步骤的提交版本',
  status      VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/LATE/GRADED/RETURNED',
  is_latest   TINYINT UNSIGNED NOT NULL DEFAULT 1,
  submit_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sub_version (step_id, student_id, version),
  KEY idx_sub_latest (step_id, student_id, is_latest),
  KEY idx_sub_student (student_id, submit_at),
  CONSTRAINT fk_sub_step    FOREIGN KEY (step_id)    REFERENCES step (id),
  CONSTRAINT fk_sub_student FOREIGN KEY (student_id) REFERENCES sys_user (id)
) ENGINE=InnoDB COMMENT='成果提交（保留历史版本）';

CREATE TABLE submission_item (
  id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  submission_id        BIGINT UNSIGNED NOT NULL,
  deliverable_item_id  BIGINT UNSIGNED NOT NULL,
  type_code            VARCHAR(32) NOT NULL COMMENT '冗余类型，便于按类型渲染与统计',
  text_value           TEXT DEFAULT NULL COMMENT 'TEXT 类型内容',
  file_id              BIGINT UNSIGNED DEFAULT NULL COMMENT '文件类内容',
  url_value            VARCHAR(1024) DEFAULT NULL COMMENT 'URL 类内容',
  created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted           TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sub_item (submission_id, deliverable_item_id),
  CONSTRAINT fk_si_submission FOREIGN KEY (submission_id)       REFERENCES submission (id),
  CONSTRAINT fk_si_item       FOREIGN KEY (deliverable_item_id) REFERENCES step_deliverable_item (id),
  CONSTRAINT fk_si_type       FOREIGN KEY (type_code)           REFERENCES dict_deliverable_type (code),
  CONSTRAINT fk_si_file       FOREIGN KEY (file_id)             REFERENCES sys_file (id)
) ENGINE=InnoDB COMMENT='提交项（与交付项一一对应，三个内容字段按类型择一）';

CREATE TABLE score_rubric (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  course_id   BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(128) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_rubric_course (course_id),
  CONSTRAINT fk_rubric_course FOREIGN KEY (course_id) REFERENCES course (id)
) ENGINE=InnoDB COMMENT='评分标准（迭代2启用）';

CREATE TABLE score_rubric_dimension (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rubric_id  BIGINT UNSIGNED NOT NULL,
  name       VARCHAR(128) NOT NULL COMMENT '如 功能完成度/代码质量/文档',
  weight     DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '占比(%)',
  max_score  DECIMAL(6,2) NOT NULL DEFAULT 100.00,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_dim_rubric (rubric_id),
  CONSTRAINT fk_dim_rubric FOREIGN KEY (rubric_id) REFERENCES score_rubric (id)
) ENGINE=InnoDB COMMENT='评分维度';

CREATE TABLE score (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  submission_id BIGINT UNSIGNED NOT NULL,
  teacher_id    BIGINT UNSIGNED NOT NULL,
  total_score   DECIMAL(6,2) DEFAULT NULL,
  comment       TEXT DEFAULT NULL,
  graded_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_score_submission (submission_id),
  CONSTRAINT fk_score_submission FOREIGN KEY (submission_id) REFERENCES submission (id),
  CONSTRAINT fk_score_teacher    FOREIGN KEY (teacher_id)    REFERENCES sys_user (id)
) ENGINE=InnoDB COMMENT='评分与评语';

CREATE TABLE score_detail (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  score_id     BIGINT UNSIGNED NOT NULL,
  dimension_id BIGINT UNSIGNED NOT NULL,
  score        DECIMAL(6,2) NOT NULL,
  comment      VARCHAR(512) DEFAULT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_score_dimension (score_id, dimension_id),
  CONSTRAINT fk_sd_score FOREIGN KEY (score_id)     REFERENCES score (id),
  CONSTRAINT fk_sd_dim   FOREIGN KEY (dimension_id) REFERENCES score_rubric_dimension (id)
) ENGINE=InnoDB COMMENT='分维度得分（迭代2启用）';

CREATE TABLE score_feedback (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  score_id      BIGINT UNSIGNED NOT NULL,
  student_id    BIGINT UNSIGNED NOT NULL,
  content       TEXT NOT NULL,
  teacher_reply TEXT DEFAULT NULL,
  status        VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REPLIED/CLOSED',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_feedback_score (score_id),
  CONSTRAINT fk_fb_score   FOREIGN KEY (score_id)   REFERENCES score (id),
  CONSTRAINT fk_fb_student FOREIGN KEY (student_id) REFERENCES sys_user (id)
) ENGINE=InnoDB COMMENT='成绩留言/申诉（迭代2启用）';

-- ------------------------------------------------------------
-- E. 消息、AI 与日志
-- ------------------------------------------------------------

CREATE TABLE notification (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  receiver_id  BIGINT UNSIGNED NOT NULL,
  sender_id    BIGINT UNSIGNED DEFAULT NULL,
  type         VARCHAR(32)  NOT NULL COMMENT 'STEP_PUBLISHED/DUE_REMINDER/SCORE_RELEASED',
  title        VARCHAR(255) NOT NULL,
  content      TEXT DEFAULT NULL,
  related_type VARCHAR(32)  DEFAULT NULL,
  related_id   BIGINT UNSIGNED DEFAULT NULL,
  is_read      TINYINT UNSIGNED NOT NULL DEFAULT 0,
  read_at      DATETIME DEFAULT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_notify_receiver (receiver_id, is_read, created_at)
) ENGINE=InnoDB COMMENT='站内消息（迭代2启用）';

CREATE TABLE ai_service_config (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name              VARCHAR(64)  NOT NULL,
  provider          VARCHAR(32)  NOT NULL DEFAULT 'DEEPSEEK',
  base_url          VARCHAR(255) NOT NULL,
  api_key_encrypted VARCHAR(512) NOT NULL COMMENT 'AES 加密存储，严禁明文',
  model             VARCHAR(64)  NOT NULL DEFAULT 'deepseek-chat',
  params_json       JSON DEFAULT NULL COMMENT 'temperature/top_p 等生成参数',
  timeout_ms        INT UNSIGNED NOT NULL DEFAULT 60000,
  max_retry         INT UNSIGNED NOT NULL DEFAULT 2,
  token_limit       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 表示不限',
  enabled           TINYINT UNSIGNED NOT NULL DEFAULT 1,
  is_active         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '同一时刻仅一条为1，应用层事务保证',
  created_by        BIGINT UNSIGNED DEFAULT NULL,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_ai_active (is_active, enabled)
) ENGINE=InnoDB COMMENT='大模型服务配置（迭代3启用）';

CREATE TABLE ai_call_log (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  config_id         BIGINT UNSIGNED DEFAULT NULL,
  user_id           BIGINT UNSIGNED DEFAULT NULL,
  biz_type          VARCHAR(32) NOT NULL COMMENT 'REVIEW/SUMMARY/STEP_GENERATE/TUTOR',
  related_id        BIGINT UNSIGNED DEFAULT NULL,
  model             VARCHAR(64)  DEFAULT NULL,
  prompt_tokens     INT UNSIGNED NOT NULL DEFAULT 0,
  completion_tokens INT UNSIGNED NOT NULL DEFAULT 0,
  total_tokens      INT UNSIGNED NOT NULL DEFAULT 0,
  status            VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED/TIMEOUT',
  error_msg         VARCHAR(1024) DEFAULT NULL COMMENT '必须过滤密钥',
  latency_ms        INT UNSIGNED NOT NULL DEFAULT 0,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_log_biz (biz_type, related_id),
  KEY idx_ai_log_time (created_at)
) ENGINE=InnoDB COMMENT='大模型调用记录（迭代3启用）';

CREATE TABLE operation_log (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED DEFAULT NULL,
  module      VARCHAR(64)  DEFAULT NULL,
  operation   VARCHAR(128) DEFAULT NULL,
  method      VARCHAR(16)  DEFAULT NULL,
  url         VARCHAR(512) DEFAULT NULL,
  params_json JSON DEFAULT NULL COMMENT '敏感字段（密钥/密码）须脱敏',
  ip          VARCHAR(64)  DEFAULT NULL,
  user_agent  VARCHAR(255) DEFAULT NULL,
  success     TINYINT UNSIGNED NOT NULL DEFAULT 1,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_oplog_user (user_id, created_at)
) ENGINE=InnoDB COMMENT='操作日志（迭代4启用）';

CREATE TABLE login_log (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED DEFAULT NULL,
  username    VARCHAR(64)  DEFAULT NULL,
  ip          VARCHAR(64)  DEFAULT NULL,
  user_agent  VARCHAR(255) DEFAULT NULL,
  status      VARCHAR(16)  NOT NULL COMMENT 'SUCCESS/FAILED',
  fail_reason VARCHAR(255) DEFAULT NULL,
  login_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_loginlog_time (login_at)
) ENGINE=InnoDB COMMENT='登录日志（迭代4启用）';

-- ------------------------------------------------------------
-- F. 初始化数据
-- ------------------------------------------------------------

INSERT INTO sys_role (code, name) VALUES
  ('ADMIN', '管理员'), ('TEACHER', '教师'), ('STUDENT', '学生');

INSERT INTO sys_permission (code, name, module) VALUES
  ('user:manage',      '用户管理',       'SYSTEM'),
  ('class:manage',     '班级管理',       'CLASS'),
  ('course:manage',    '课程管理',       'COURSE'),
  ('step:manage',      '步骤管理',       'STEP'),
  ('submission:submit','提交成果',       'SUBMISSION'),
  ('submission:view',  '查看全班提交',   'SUBMISSION'),
  ('score:grade',      '批改评分',       'SCORE'),
  ('template:manage',  '课程模板管理',   'TEMPLATE'),
  ('ai:config',        '大模型服务配置', 'AI'),
  ('system:config',    '系统参数配置',   'SYSTEM'),
  ('log:view',         '日志查看',       'SYSTEM');

INSERT INTO sys_role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p WHERE r.code = 'ADMIN';

-- 交付物类型字典：MVP 启用 4 种，其余由迭代 2 置 enabled=1
INSERT INTO dict_deliverable_type
  (code, name, frontend_component, backend_handler, enabled, sort_order) VALUES
  ('DOCUMENT',    '文档',         'FileUploadItem', 'DocumentValidator',   1, 1),
  ('CODE_ARCHIVE','代码压缩包',   'FileUploadItem', 'ArchiveValidator',    1, 2),
  ('REPO_URL',    '源码仓库',     'UrlInputItem',   'RepoUrlValidator',    1, 3),
  ('TEXT',        '文本/说明',    'TextAreaItem',   'TextLengthValidator', 1, 4),
  ('DEMO_URL',    '在线演示地址', 'UrlInputItem',   'UrlValidator',        0, 5),
  ('IMAGE',       '截图/图片',    'ImageUploadItem','ImageValidator',      0, 6),
  ('OTHER_FILE',  '其他附件',     'FileUploadItem', 'DefaultFileValidator',0, 7);

INSERT INTO sys_config (config_key, config_value, description) VALUES
  ('ai.global.enabled',             'false',  'AI 全局总开关，MVP 恒为关闭'),
  ('file.max_size_mb',              '50',     '单个上传文件大小上限(MB)'),
  ('invite_code.length',            '8',      '邀请码长度'),
  ('invite_code.default_valid_days','30',     '邀请码默认有效天数'),
  ('invite_code.default_max_uses',  '0',      '邀请码默认使用上限，0 表示不限');

-- 初始管理员账号由 BootstrapDataInitializer 在应用启动时用 BCrypt 生成后写入。
