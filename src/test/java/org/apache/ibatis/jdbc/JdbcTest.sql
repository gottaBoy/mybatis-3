CREATE TABLE `t_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  `mobile` varchar(16) CHARACTER SET latin1 DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `last_update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `t_user` (name,mobile,create_time, last_update_time) VALUES ('test1', '18888888888', '2017-09-20 20:27:08', '2017-12-14 15:13:11');
INSERT INTO `t_user` (name,mobile,create_time, last_update_time)VALUES ('test2', '18888888888', '2017-09-20 20:27:08', '2017-12-14 15:13:14');
INSERT INTO `t_user` (name,mobile,create_time, last_update_time)VALUES ('test3', '18888888888', '2017-09-20 20:27:08', '2017-12-14 15:13:16');
INSERT INTO `t_user` (name,mobile,create_time, last_update_time)VALUES ('test4', '18888888888', '2017-09-20 20:27:08', '2017-12-14 15:13:19');
INSERT INTO `t_user` (name,mobile,create_time, last_update_time)VALUES ('test5', '18888888888', '2017-09-20 20:27:08', '2017-12-14 15:13:21');


/*创建一个简单存储过程 获取指定表总记录*/
drop procedure if exists proc_table_rows;
delimiter //
CREATE PROCEDURE proc_table_rows (
IN table_name varchar(32), /*表名*/
OUT table_rows INT,  /*返回值*/
IN no_use INT) /*无用测试*/
BEGIN
	SET @result = CONCAT('SELECT COUNT(*) INTO @v_rows FROM ',table_name);
	PREPARE s1 FROM @result;
	EXECUTE s1;
	DROP PREPARE s1;
	set table_rows = @v_rows;
END
//

/*执行存储过程*/
CALL proc_table_rows('t_user',@a,1);
SELECT @a;