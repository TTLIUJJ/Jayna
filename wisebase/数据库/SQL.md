# SQL

SQL语句的基本操作和关键词

## 存储过程

存储过程是一个预编译的SQL语句，只需创建一次，在以后该程序中就可以调用多次。如果某次操作需要多次执行SQL，使用存储过程比单纯的SQL语句执行更快。

- 可以用一个命令对象来调用存储过程
- 可以拱外部程序调用，比如java程序


**优点：**

- 存储过程是预编译处理过，执行效率高
- 存储过程的代码直接存放于数据库中，通过存储过程名直接调用，减少网络通信
- 存储过程可以重复使用，减少数据库开发人员的工作量
- 安全性高，执行存储过程需要有一定权限的用户

**缺点：**

- 难以调试和扩展
- 没有移植性

## 视图

视图是一种虚拟的表，具有和物理表相同的功能，视图通常是一个表或多个表的行或列的子集。可以对视图进行增删改查操作，对其修改会影响基本表。它使我们获取数据更容易，相比多表查询。

**优点：**

- 对于相同的数据可以产生不同的视图，因为视图可以有选择性的选取数据库里的一部分
- 维护数据的独立性，视图可以从多个表检索数据
- 用户通过简单的查询可以从原本的复杂查询中得到结果

**缺点：**

- 性能的下降：查询视图时，必须把视图的查询转化为对基本表的查询
- 局限性：如果视图是由一个复杂的多表查询所定义，那么久无法更改数据

## 通配符


**like：**指示MySQL后面利用通配符进行模糊搜索，而不是直接相等匹配进行比较。注意like不能匹配NULL。在匹配英文文本时候，是否区分大小取决于MySQL的用户配置。

- %：匹配任何字符出现的任意次数（0或者更多）
- _：匹配单个字符（有且只有1个）


## 删除

数据库的删除操作有三个关键字：

- drop直接删除表
- truncate删除表中的数据，再插入时自增id又从1开始
- delete删除表表中的数据，




## count

- count(*) 对行的数目进行计算，包括NULL
- count(字段) 对字段的列进行行数目计算，不包括NULL
- count(1)的结果和count(*)的结果是一样的

如果表没有主键，那么count(1)比count(*)快

如果表存在主键，那么count(主键，联合主键)比count(*)快

如果表只有一个字段，那么count(*)就是最快的

count(1)和count(主键)一样，只扫描主键；count(*)和count(非主键)一样，扫描整个表。很明显扫描扫描主键速度更快。



## Explain

```
mysql >EXPLAIN ...sql
```

- id为查询序列号，即SQL语句执行顺序，一个SQL可能有好多个SELECT
- select_type，SELECT的类型
	- simple：没有UNION和子查询
	- union：使用了union查询
	- primary：在使用了子查询中的最外层查询，即主查询
- table：查询的表明
	- < drivedX >：X表示id查询使用的结果集
- type：连接类型
	- system：表只有1行，这是const的特例
	- const：表最多只有一个匹配行，const用于主键索引或者唯一索引，因为只可能有一个匹配行，所以速度很快。
	- eq_ref：“对于每个来自前面的表的行组合，从该表中读取一行”，主查询得到副查询中的一行。
	- ref：对于每个来自前面的表的行组合，所有匹配索引值的行将从这集合结果中读取。
	- range：使用索引来查询给定的范围 SELECT ... uid in (1, 2)
	- index：只有索引树被扫描
	- ALL：完整的全表扫描，性能最差
- possiblie_keys：提示使用哪个索引会在该表中找到
- kyes：MySQL使用的索引
- key_len：MySQL使用的索引的长度
- rows：MySQL执行查询的行数，数值越大越不好，说明没有用好索引
- Extra：MySQL解决查询的详细信息

## Join


```sql
mysql >CREATE TABLE `t1`(
			`id` INT(11) NOT NULL AUTO_INCREMENT,
			`deptName` VARCHAR(30),
			`locaAdd` VARCHAR(40),
			PRIMAY KEY(`id`)
		)ENGINE=INNODB CHARSET=utf8;

msql >CREATE TABLE `t2`(
			`id` INT(11) NOT NULL AUTO_INCREMENT,
			`name` VARCHAR(30),
			`deptId` INT(11),
			PRIMARY KEY(`id`),		
		)ENGINE=INNODB CHARSET=utf8;
```

```sql
//中间
mysql >SELECT * FROM t1 INNER JOIN t2 ON t1.id = t2.deptId;
```

```sql
//第一排
msyql >SELECT * FROM t1 LEFT JOIN t2 ON t1.id = t2.deptId;
mysql >SELECT * FROM t1 RIGHT JOIN t2 ON t1.id = t2.deptId;
```

```sql
//第二排
mysql >SELECT * FROM t1 LEFT JOIN t2 ON t1.id = t2.deptId WHERE t2.deptId is NULL;
mysql >SELECT * FROM t1 RIGHT JOIN t2 ON t1.id = t2.deptId WHERE t1.id is NULL;
```

```sql
//第三排 MYSQL 没有OUTER JOIN 使用UNION来实现
mysql >SELECT * FROM t1 LEFT JOIN t2 ON t1.id = t2.deptId
	union
	SELCT * FROM t1 RIGHT JOIN t2 on t1.id = t2.deptId;
	
msql> SELECT * FROM t1 LEFT JOIN t2 ON t1.id = t2.deptId WHERE t2.deptId is NULL
	union
	SELECT * FROM t1 RIGHT JOIN t2 ON t1.id = t2.deptId WHERE t1.id is NULL;
```

![](http://p5s0bbd0l.bkt.clouddn.com/sql1.jpg)


```sql
// 笛卡尔积（交叉联合），非常不推荐，产生t1的行*t2的行 超大集合
mysql >SELCT * FROM t1 CROSS JOIN t2;
```

```sql
// 自然连接，基于两表中字段和数值都相同的字段，进行等值连接
//注意：两表中同名的列不能超过1个
mysql >SELECT * FROM t1 NATURAL JOIN t2;
```


## 查询性能优化

#### Explain

用来分析SQL语句，分析结果中比较重要的字段有：

- select_type：查询类型，包含简单查询、联合查询和子查询 
- key：使用的索引
- rows：扫描的行数

#### 减少返回的列

慢查询主要原因是访问了过多的数据，除了访问过多的行之外，也包括访问过多的列。最好不要使用 SELECT * 语句，要根据需要选择查询的列。

#### 减少返回的行

最好使用LIMIT语句来取出想要的那些行。
建立索引来减少条件语句的全表扫描。不使用索引的情况可能需要进行全表扫描，而使用了索引只需要扫描几行记录即可，使用Explain语句可以通过观察rows字段来看出这种差异。


![](http://owj98yrme.bkt.clouddn.com/sql10.png)

![](http://owj98yrme.bkt.clouddn.com/sql11.png)

#### 拆分大的DELETE或INSERT语句

如果一次性执行的话，可能一次锁住很多数据、占满整个事物日志、耗尽系统资源、阻塞很多小的但重要的查询


## 键和索引


主键不能重复，不能为空，在MySQL InnoDB中，若在指定主键之前，没有创建聚簇索引，InnoDB会自动创建聚蔟索引，一个表只能有一个聚蔟索引。
唯一键不能重复，可以为空


- 普通索引

```sql
	mysql >ALTER TABLE `table_name` ADD INDEX index_name(`column`);
```

- 唯一索引，不允许索引的列拥有重复的值

```
	mysql >ALTER TABLE `table_name` ADD UNIQUE(`column`);
```

- 主索引，创建主键会自动创建主键索引

```
	mysql >ALTER TABLE `table_name`ADD PRIMARY KEY(`column`); 
```

- 复合索引，需要符合最左前缀匹配

```
	mysql >ALTER TABLE `table_name` ADD INDEX index_name(`colnum1`, `colnum2`, `colnum3`);	
```



## 数据类型

#### 整型

```
TINYINT 8
SAMLLINT 16 
MEDIUMINT 24
INT 32
BIGINT 64
```

一般情况下越小的列越好，INT(11)中的数字只是规定了交互工具显示字符的个数，对于存储和计算来说是没有意义的。

#### 浮点数

FLOAT和DOUBLE为浮点类型，DECIMAL为高精度小数类型。CPU原生支持浮点运算，但是不支持DECIMAL类型的计算，因此DECIMAL的计算比浮点类型需要更高的代价。
FLOAT、DOUBLE和DECIMAL都可以指定列宽，例如DECIMAL(18, 5)表示总共18位，取5位存储小数部分。

#### 字符串

主要有 CHAR 和 VARCHAR 两种类型，一种是定长的，一种是变长的。

VARCHAR 这种变长类型能够节省空间，因为只需要存储必要的内容。但是在执行 UPDATE 时可能会使行变得比原来长，当超出一个页所能容纳的大小时，就要执行额外的操作，MyISAM 会将行拆成不同的片段存储，而 InnoDB 则需要分裂页来使行放进页内。

VARCHAR 会保留字符串末尾的空格，而 CHAR 会删除。

#### 时间和日期

MySQL 提供了两种相似的日期时间类型：DATATIME 和 TIMESTAMP。
