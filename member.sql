SELECT * FROM users;

delete from users;

ALTER TABLE users AUTO_INCREMENT = 1;

-- 외래키 있을 경우
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- 🔐 암호화된 비밀번호: qwer1234!@#$

-- Naver 유저
INSERT INTO users (user_id, nick_name, password, name, birth_date, phone_number, address, provider, role, email, oauth_id)
VALUES 
('naver_user01', 'SkyWalker', 'password123', '김하늘', '1995-05-10', '010-1234-5678', '서울시 강남구', 'naver', 'USER', 'skywalker@naver.com', 'naver_01'),
('naver_user02', 'BlueMoon', 'password123', '박지현', '1997-08-21', '010-2233-4455', '부산시 해운대구', 'naver', 'USER', 'bluemoon@naver.com', 'naver_02'),
('kakao_user01', 'SunnyDay', 'password123', '이선희', '1994-03-02', '010-1122-3344', '대구시 수성구', 'kakao', 'USER', 'sunnyday@kakao.com', 'kakao_01'),
('kakao_user02', 'NightWolf', 'password123', '최건우', '1992-11-15', '010-5566-7788', '광주시 북구', 'kakao', 'USER', 'nightwolf@kakao.com', 'kakao_02'),
('gmail_user01', 'RedDragon', 'password123', '오지훈', '1996-02-28', '010-6677-8899', '인천시 남동구', 'gmail', 'USER', 'reddragon@gmail.com', 'gmail_01'),
('gmail_user02', 'DreamCatcher', 'password123', '정유진', '1998-07-07', '010-7788-9900', '대전시 서구', 'gmail', 'USER', 'dreamcatcher@gmail.com', 'gmail_02'),
('naver_user03', 'OceanWave', 'password123', '한서준', '1993-12-25', '010-9999-1111', '경기도 성남시', 'naver', 'USER', 'oceanwave@naver.com', 'naver_03'),
('kakao_user03', 'GreenForest', 'password123', '장민호', '1991-06-18', '010-1212-3434', '울산시 중구', 'kakao', 'USER', 'greenforest@kakao.com', 'kakao_03'),
('gmail_user03', 'GoldenLion', 'password123', '윤세아', '1990-09-09', '010-8989-7878', '경기도 수원시', 'gmail', 'USER', 'goldenlion@gmail.com', 'gmail_03'),
('naver_user04', 'SilverArrow', 'password123', '백은수', '1999-01-01', '010-3434-5656', '서울시 마포구', 'naver', 'USER', 'silverarrow@naver.com', 'naver_04');



INSERT INTO bike_users (user_id, name, phone_number, email, registration_date)
VALUES 
(1, '홍길동', '01012345678', 'dummy1@example.com', NOW()),
(2, '김철수', '01023456789', 'dummy2@example.com', NOW()),
(3, '이영희', '01034567890', 'dummy3@example.com', NOW()),
(4, '박지성', '01045678901', 'dummy4@example.com', NOW()),
(5, '최수연', '01056789012', 'dummy5@example.com', NOW()),
(6, '조세호', '01067890123', 'dummy6@example.com', NOW()),
(7, '한지민', '01078901234', 'dummy7@example.com', NOW()),
(8, '정해인', '01089012345', 'dummy8@example.com', NOW()),
(9, '김유정', '01090123456', 'dummy9@example.com', NOW()),
(10, '박보검', '01001234567', 'dummy10@example.com', NOW());

SELECT * FROM board_category;

INSERT INTO board_category (category_id, name) VALUES (1, '공지사항'), (2, '자유게시판'), (3, '민원게시판');

SELECT user_id, role FROM users;

SELECT * from board;

ALTER TABLE board
  DROP COLUMN `is_notice`,
  DROP COLUMN `ais_notice`,
  MODIFY `notice` bit(1) NOT NULL DEFAULT b'0' COMMENT '공지 여부 (1: 공지글)';

  SELECT * from users;

  DELETE FROM board WHERE user_id = 'rhrnrk36';