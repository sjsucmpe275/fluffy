#Project	Fluffy	
	
Storing	data	in	the	cloud	or	on	a	network	of	servers	share	a	common	challenge	-	
how	to	securely	share,	store,	and	survive	failures	and	corruption	of	data.	There	are	
systems	like	Drop	Box,	Sync,	Google	Drive,	and	server	installations	like	HDFS	and	
databases	like	Mongo	that	use	multiple	servers	and	redundant	storage	approaches.		
	
As	consumers	we	are	left	with	choosing	one.	What	if	we	could	use	any	number	of	
services?		
	
Your	team	has	been	awarded	the	project	to	design	and	implement	a	new	approach	
to	storing	and	finding	data	in	a	larger,	decentralized,	heterogeneous	platform.	
	
This	undertaking	may	appear	to	be	straightforward,	and	some	of	the	concepts	and	
technologies	are	certainly	that.	The	challenge	is	in	combining	and	building	loosely	
coupled,	dynamic	networks	where	all	nodes	and	technology	stacks	are	not	known.	
There	are	no	set	software	stacks	and	tools	–bounds	are	fuzzy	–	fluffy	so	to	speak.	
	
Your	team	has	been	tasked	to	build	the	communication	and	balancing	overlay	
system	to	collect,	store	and	stream	data	with	emphasis	on:	<br>
1. Cloud	–	server	–	personal	device	transparency	
2. Balance	work	across	stacks	
3. Survive	failures	
	
To	this	goal,	you	are	given	the	responsibility	to	design	and	build	the	FileEdge	
architecture.	This	should	include	a	scalability	strategy	to	account	for	a	massive	
registration	processes,	distributed	image	storage,	and	search	capabilities	that	
include	real-time	monitoring	of	work	and	data	as	they	enter	the	network	of	servers.		
	
A	study	conducted	last	quarter	has	identified	that	the	best	approach	would	be	to	
design	a	system	that	can	be	hosted	on	a	series	of	compute	platforms	using	multiple	
international	deployments.	The	study	also	concluded	the	candidate	technologies	for	
the	system	should	be:	
	
####Languages:		 	
Java,	and	Python	(client	API),	C++	(client	API)	

####Core	Packages:		
Google	Protobuf,	Netty	

####Storage:		
In-memory	database

####Challenges:	 	
Leader	election,	replication,	and	work/data	balance	
	
These	candidate	languages/technologies	(though	not	inclusive)	would	be	best	
utilized	in	the	following	configuration:	The	storage	servers	should	be	Java-based	
and	feeds	(adding	files)	have	dual	Java	and	Python	APIs.	Further	recommendations	
where	the	use	of	a	GIS	database	and	metadata	support	with	geographic	
registration/searching.		

Users	of	the	system	will	likely	interact	through	any	number	of	possible	platforms	
(application,	a	web,	or	mobile	platforms)	-	you	are	building	a	toolkit/API	to	not	only	
for	clients	but	also	for	server-side	processes.		
	
Note	at	this	time	the	team	is	not	directed	to	implement	a	web	or	mobile	app.	The	
team's	target	is	strictly	the	server-side	infrastructure.	However,	the	team	will	need	
to	evaluate	the	suitability	of	the	API	for	a	spectrum	of	client	uses.	
	
	
###Communication:	
	
Since	you	are	building	a	package	for	hosting	data,	your	teams	must	ensure	that	
multiple	servers	can	interoperate	to	form	dynamic	'overlay'	fabric.	Furthermore,	it	
is	unknown	how	wide	the	network	will	form	so,	having	each	server	know	all	nodes	
is	not	possible.	
	
To	test	the	design	and	strengths	of	all	implementations,	each	team	need	to	interact	
with	deployments	of	other	teams.	Each	team	should	support	two	storage	solutions	
of	which	50%	cannot	overlap	another	team’s	choice.		

##Instruction to run the project
Download the source code
Install ant
Download these libraries:
1. commons-pool2-2.4.2.jar
2. gemfire-core-1.0.0-incubating.M1.jar
3. hamcrest-all-1.3.jar
4. jackson-all-1.8.5.jar
5. jedis-2.4.2.jar
6. junit-4.12.jar
7. leveldbjni-all-1.8.jar
8. netty-all-4.0.15.Final.jar
9. protobuf-java-2.5.0.jar
10. slf4j-api-1.7.2.jar
11. slf4j-simple-1.7.2.jar
12. spymemcached-2.8.0.jar

###How to build
Go to project directory.
Type "ant"
Source code will be compiled.

####How to start server
Go to project directory
Type "ant node-1" to start server node-1 which loads Server Configuration from
"~/runtime/route-1.conf"
Type "ant node-2" to start server node-2 which loads Server Configuration from
"~/runtime/route-2.conf"
Type "ant node-3" to start server node-3 which loads Server Configuration from
"~/runtime/route-3.conf"
Type "ant node-5" to start server node-5 which loads Server Configuration from
"~/runtime/route-5.conf"
Type "ant node-6" to start server node-6 which loads Server Configuration from
"~/runtime/route-6.conf"

Topology can be defined in route-*.conf files