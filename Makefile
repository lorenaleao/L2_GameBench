compile:
	mkdir -p build
	javac -d build benchmarkgame/gameutils/*java 
	javac -d build benchmarkgame/*java 
server:
	java -cp build benchmarkgame.Server 127.0.0.1
driver:
	java -cp build benchmarkgame.Driver 2 5 127.0.0.1 41317