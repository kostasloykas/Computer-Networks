

all: WebServer3980 WebServer3981 WebServer3982 Client

WebServer3980:
	javac -Xlint ./WebServer1/WebServer3980.java

WebServer3981:
	javac -Xlint ./WebServer2/WebServer3981.java

WebServer3982:
	javac -Xlint ./WebServer3/WebServer3982.java

Client:
	javac ./Clients/Client.java


copyy:
	@cat ./WebServer1/WebServer3980.java > ./WebServer2/WebServer3981.java
	@cat ./WebServer1/WebServer3980.java > ./WebServer3/WebServer3982.java
.PHONY:

clean:
