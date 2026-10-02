-- Run after schema.sql for local UI walkthroughs only.
INSERT IGNORE INTO roles(code,name) VALUES ('CUSTOMER','Khách hàng'),('VENUE_OWNER','Chủ sân'),('SUPER_ADMIN','Quản trị sàn');
INSERT IGNORE INTO users(email,password_hash,full_name,status) VALUES
 ('demo.owner@daln.local','NOT_FOR_AUTHENTICATION','Chủ sân Demo','ACTIVE'),
 ('demo.player@daln.local','NOT_FOR_AUTHENTICATION','Người chơi Demo','ACTIVE');
INSERT IGNORE INTO user_roles(user_id,role_id)
 SELECT u.id,r.id FROM users u JOIN roles r ON r.code='VENUE_OWNER' WHERE u.email='demo.owner@daln.local';
INSERT IGNORE INTO user_roles(user_id,role_id)
 SELECT u.id,r.id FROM users u JOIN roles r ON r.code='CUSTOMER' WHERE u.email='demo.player@daln.local';
INSERT IGNORE INTO sports_categories(code,name,active) VALUES
 ('FOOTBALL','Bóng đá',TRUE),('BADMINTON','Cầu lông',TRUE),('PICKLEBALL','Pickleball',TRUE),('TENNIS','Tennis',TRUE);

INSERT INTO venues(owner_id,name,description,address,city,timezone,location,opening_time,closing_time,status)
SELECT id,'DALN Sports Hub','Cơ sở dữ liệu mẫu để trải nghiệm đặt sân.','25 Nguyễn Bỉnh Khiêm, Phường Bến Nghé','TP. Hồ Chí Minh','Asia/Ho_Chi_Minh',
 ST_GeomFromText('POINT(106.7009 10.7870)',4326,'axis-order=long-lat'),'06:00:00','23:00:00','ACTIVE'
FROM users WHERE email='demo.owner@daln.local'
AND NOT EXISTS(SELECT 1 FROM venues WHERE name='DALN Sports Hub');

INSERT IGNORE INTO venue_sports(venue_id,sport_id)
 SELECT v.id,s.id FROM venues v JOIN sports_categories s ON s.code IN ('BADMINTON','PICKLEBALL') WHERE v.name='DALN Sports Hub';

INSERT INTO courts(venue_id,sport_id,parent_id,name,court_type,capacity,active,bookable)
SELECT v.id,s.id,NULL,'Nhà thi đấu A','INDOOR',12,TRUE,FALSE
FROM venues v JOIN sports_categories s ON s.code='BADMINTON' WHERE v.name='DALN Sports Hub'
AND NOT EXISTS(SELECT 1 FROM courts WHERE venue_id=v.id AND name='Nhà thi đấu A');
INSERT INTO courts(venue_id,sport_id,parent_id,name,court_type,capacity,active,bookable)
SELECT v.id,s.id,p.id,child.court_name,'BADMINTON',4,TRUE,TRUE
FROM venues v JOIN sports_categories s ON s.code='BADMINTON'
JOIN courts p ON p.venue_id=v.id AND p.name='Nhà thi đấu A'
JOIN (SELECT 'Sân cầu lông 1' court_name UNION ALL SELECT 'Sân cầu lông 2') child
WHERE v.name='DALN Sports Hub' AND NOT EXISTS(SELECT 1 FROM courts c WHERE c.venue_id=v.id AND c.name=child.court_name);

INSERT IGNORE INTO court_closure(ancestor_id,descendant_id,depth)
 SELECT id,id,0 FROM courts WHERE venue_id=(SELECT id FROM venues WHERE name='DALN Sports Hub');
INSERT IGNORE INTO court_closure(ancestor_id,descendant_id,depth)
 SELECT parent_id,id,1 FROM courts WHERE venue_id=(SELECT id FROM venues WHERE name='DALN Sports Hub') AND parent_id IS NOT NULL;

INSERT INTO pricing_rules(venue_id,sport_id,court_id,name,day_of_week,start_time,end_time,price_per_slot,priority,active)
SELECT v.id,s.id,NULL,'Giá mẫu cả ngày',NULL,'06:00:00','23:00:00',120000,0,TRUE
FROM venues v JOIN sports_categories s ON s.code='BADMINTON' WHERE v.name='DALN Sports Hub'
AND NOT EXISTS(SELECT 1 FROM pricing_rules p WHERE p.venue_id=v.id AND p.name='Giá mẫu cả ngày');
