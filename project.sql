-- users 테이블
CREATE TABLE users (
    user_id varchar(255) NOT NULL COMMENT '아이디 중복불가 PK',
    nick_name varchar(100) NOT NULL COMMENT '닉네임 중복불가',
    password varchar(255) NOT NULL COMMENT '비밀번호',
    name varchar(100) NOT NULL COMMENT '사용자 실명',
    birth_date date DEFAULT NULL COMMENT '생년월일',
    phone_number varchar(20) DEFAULT NULL COMMENT '전화번호',
    address varchar(255) DEFAULT NULL COMMENT '주소',
    provider varchar(50) DEFAULT NULL COMMENT '로그인 제공자(네이버, 카카오, 구글 등)',
    create_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '회원가입 시간',
    update_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '정보 수정 시간',
    role varchar(10) NOT NULL DEFAULT 'USER' COMMENT '계정 권한 (USER or ADMIN)',
    email varchar(255) DEFAULT NULL,
    oauth_id varchar(255) DEFAULT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY nick_name (nick_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '회원가입 데이터 베이스'

-- board_category 테이블
CREATE TABLE board_category (
    category_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '게시판 종류의 고유 ID',
    name VARCHAR(255) COMMENT '게시판 이름(공지글, 자유게시판, 민원게시판 등)'
) COMMENT '게시판 종류 테이블';

-- board 테이블
CREATE TABLE board (
    board_num int NOT NULL AUTO_INCREMENT COMMENT '게시글 고유 ID',
    board_seq int NOT NULL COMMENT '해당 카테고리 내에서의 글 번호',
    user_id varchar(255) DEFAULT NULL COMMENT '회원 고유 식별자',
    category_id int DEFAULT NULL COMMENT '게시판 종류의 고유 ID',
    subject varchar(100) NOT NULL COMMENT '게시판 제목',
    content text NOT NULL COMMENT '게시글 내용',
    hit int DEFAULT '0' COMMENT '조회수',
    writedate datetime DEFAULT CURRENT_TIMESTAMP COMMENT '작성시간',
    notice bit(1) NOT NULL DEFAULT b'0' COMMENT '공지 여부 (1: 공지글)',
    nick_name varchar(255) DEFAULT NULL,
    PRIMARY KEY (board_num),
    UNIQUE KEY idx_category_seq (category_id, board_seq),
    KEY user_id (user_id),
    CONSTRAINT board_ibfk_1 FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT board_ibfk_2 FOREIGN KEY (category_id) REFERENCES board_category (category_id)
) ENGINE = InnoDB AUTO_INCREMENT = 17 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '게시글 테이블';

-- board_image 테이블
CREATE TABLE board_image (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '이미지 고유 ID',
    board_num INT NOT NULL COMMENT '게시글 고유 ID',
    image_path VARCHAR(255) DEFAULT NULL COMMENT '이미지 파일 경로',
    image_title VARCHAR(100) DEFAULT NULL COMMENT '이미지 제목 또는 설명',
    FOREIGN KEY (board_num) REFERENCES board (board_num) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '게시글 이미지 테이블';

-- reply 테이블
CREATE TABLE reply (
    num INT NOT NULL AUTO_INCREMENT COMMENT '댓글 ID',
    board_num INT NOT NULL COMMENT '게시글 PK',
    user_id VARCHAR(255) DEFAULT NULL COMMENT '회원 ID',
    content VARCHAR(400) NOT NULL COMMENT '댓글 내용',
    writedate DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '작성 시간',
    ipaddr VARCHAR(20),
    gup INT NOT NULL,
    lev INT DEFAULT 0,
    seq INT DEFAULT 0,
    PRIMARY KEY (num),
    KEY (board_num),
    KEY (user_id),
    CONSTRAINT fk_reply_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_reply_board FOREIGN KEY (board_num) REFERENCES board (board_num)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '댓글 테이블';

-- exroad_properties 테이블
CREATE TABLE exroad_properties (
    UFID VARCHAR(255) PRIMARY KEY COMMENT '고유 ID',
    RDNU VARCHAR(25) NOT NULL COMMENT '도로 번호',
    NAME VARCHAR(100) NOT NULL COMMENT '도로 이름'
) COMMENT '고속도로 정보 테이블'

-- exroad_coordinates 테이블
CREATE TABLE exroad_coordinates (
    UFID VARCHAR(255) COMMENT '고유 ID',
    geo_X FLOAT NOT NULL COMMENT '위도',
    geo_Y FLOAT NOT NULL COMMENT '경도',
    Foreign Key (UFID) REFERENCES exroad_properties (UFID)
) COMMENT '고속도로 위도/경도 테이블'

-- itsroad_properties 테이블
CREATE TABLE itsroad_properties (
    UFID VARCHAR(255) PRIMARY KEY COMMENT '고유 ID',
    RDNU VARCHAR(25) NOT NULL COMMENT '도로 번호',
    NAME VARCHAR(100) NOT NULL COMMENT '도로 이름'
) COMMENT '국도 정보 테이블'

-- itsroad_coordinates 테이블
CREATE TABLE itsroad_coordinates (
    UFID VARCHAR(255) COMMENT '고유 ID',
    geo_X FLOAT NOT NULL COMMENT '위도',
    geo_Y FLOAT NOT NULL COMMENT '경도',
    Foreign Key (UFID) REFERENCES itsroad_properties (UFID)
) COMMENT '국도 위도/경도 테이블'

SHOW DATABASES;
